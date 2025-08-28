/*
 *  Copyright (c) 2025 Revinci AI.
 *
 *  All rights reserved. This software is proprietary to and embodies the
 *  confidential technology of Revinci AI. Possession,
 *  use, duplication, or dissemination of the software and media is
 *  authorized only pursuant to a valid written license from Revinci AI.
 *
 *  Unauthorized use of this software is strictly prohibited.
 *
 *  THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL REVINCI AI BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author
 *
 */

package ai.revinci.platform.services.db.provisioning.azure.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.messaging.service.MessagePublisher;
import ai.revinci.platform.provisioning.db.data.model.experience.PopulateTenantDatabase;
import ai.revinci.platform.provisioning.db.data.model.experience.ProvisionTenantDatabase;
import ai.revinci.platform.provisioning.db.service.LiquibaseService;
import ai.revinci.platform.provisioning.db.service.lifecycle.IDatabaseLifecycleService;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceStatus;
import ai.revinci.platform.provisioning.status.handler.listener.ContextProvider;
import ai.revinci.platform.provisioning.status.handler.listener.IProgressListener;
import ai.revinci.platform.services.db.provisioning.azure.error.AzureDatabaseProvisioningServiceErrors;
import ai.revinci.platform.web.service.IpService;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Database;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.ServerPropertiesForDefaultCreate;
import com.azure.resourcemanager.postgresql.models.ServerVersion;
import com.azure.resourcemanager.postgresql.models.StorageProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service("azureDatabaseLifecycleService")
@RequiredArgsConstructor
public class AzureDatabaseLifecycleService implements IDatabaseLifecycleService {
    /** Configuration file that holds the settings for the database based on category (small, medium, enterprise). */
    private static final String TENANT_DB_SETTINGS_FILE_TEMPLATE = "config/{0}-tenant-db-settings.json";

    /** Template for firewall rule name. */
    private static final String TEMPLATE_FIREWALL_RULE_NAME = "{0}-firewall-rule";

    /** Template for server name generation. */
    private static final String TEMPLATE_SERVER_NAME = "{0}-postgresql-{1}";

    /** Default PostgreSQL port. */
    private static final int POSTGRESQL_PORT = 5432;

    /** Maximum wait time for operations (in minutes). */
    private static final int MAX_WAIT_TIME_MINUTES = 30;

    private final PostgreSqlManager postgreSqlManager;
    private final LiquibaseService liquibaseService;
    private final MessagePublisher messagePublisher;
    private final IpService ipService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${azure.resource-group}")
    private String resourceGroupName;

    @Value("${azure.location:East US}")
    private String location;

    @Value("${azure.postgresql.admin-username:postgres}")
    private String adminUsername;

    @Value("${azure.postgresql.admin-password}")
    private String adminPassword;

    @Instrumentation
    @Override
    public void provisionDatabase(@NonNull final ProvisionTenantDatabase request,
                                 @NonNull final IProgressListener callback) {
        final ProvisionTenantDatabase.Tenant tenant = request.getTenant();
        final ProvisionTenantDatabase.Database databaseInfo = request.getDatabase();
        final UUID tenantId = tenant.getId();
        final String realmName = tenant.getRealmName();
        final String category = tenant.getCategory().name().toLowerCase();
        
        try {
            AzureDatabaseLifecycleService.LOGGER.info("Starting Azure PostgreSQL database provisioning for tenant: {} ({})", realmName, tenantId);

            // 1. Update status to provisioning
            callback.update(ContextProvider.instance()
                    .accountId(request.getAccountId())
                    .tenantId(tenantId)
                    .resourceId(tenant.getResourceId())
                    .resourceStatus(TenantResourceStatus.PROVISIONING_INITIATED)
                    .dbInstanceName(databaseInfo.getInstanceName())
                    .dbName(databaseInfo.getDbName()));

            // 2. Load database settings for the tenant category
            final Map<String, Object> dbSettings = loadTenantDatabaseSettings(category);

            // 3. Generate server name
            final String serverName = MessageFormat.format(TEMPLATE_SERVER_NAME, realmName.toLowerCase(),
                UUID.randomUUID().toString().substring(0, 8));

            // 4. Create PostgreSQL server
            final Server server = createPostgreSqlServer(serverName, dbSettings);
            AzureDatabaseLifecycleService.LOGGER.info("Created Azure PostgreSQL server: {}", server.name());

            // 5. Create database for the tenant
            final String databaseName = realmName.toLowerCase();
            final Database createdDatabase = createDatabase(serverName, databaseName);
            AzureDatabaseLifecycleService.LOGGER.info("Created database: {} on server: {}", createdDatabase.name(), serverName);

            // 6. Configure firewall rules
            configureFirewallRules(serverName, realmName);

            // 7. Populate database with initial data
            final String connectionUrl = MessageFormat.format("jdbc:postgresql://{0}:{1}/{2}?sslmode=require",
                    server.fullyQualifiedDomainName(), POSTGRESQL_PORT, databaseName);
            
            liquibaseService.populateDatabase(connectionUrl, databaseName, adminUsername, adminPassword.getBytes());

            // 8. Update status to active
            callback.update(ContextProvider.instance()
                    .accountId(request.getAccountId())
                    .tenantId(tenantId)
                    .resourceId(tenant.getResourceId())
                    .resourceStatus(TenantResourceStatus.ACTIVE)
                    .dbInstanceName(databaseInfo.getInstanceName())
                    .dbName(databaseInfo.getDbName()));

            AzureDatabaseLifecycleService.LOGGER.info("Successfully provisioned Azure PostgreSQL database for tenant: {} ({})", realmName, tenantId);

        } catch (final Exception ex) {
            AzureDatabaseLifecycleService.LOGGER.error("Failed to provision database for tenant: {} ({}): {}", realmName, tenantId, ex.getMessage(), ex);
            callback.update(ContextProvider.instance()
                    .accountId(request.getAccountId())
                    .tenantId(tenantId)
                    .resourceId(tenant.getResourceId())
                    .resourceStatus(TenantResourceStatus.PROVISIONING_FAILED)
                    .dbInstanceName(databaseInfo.getInstanceName())
                    .dbName(databaseInfo.getDbName())
                    .errorMessage("Database provisioning failed: " + ex.getMessage()));
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_SERVER_CREATION_FAILED, ex.getMessage());
        }
    }

    @Instrumentation
    @Override
    public void populateDatabase(@NonNull final PopulateTenantDatabase request,
                                @NonNull final IProgressListener callback) {
        final UUID tenantId = request.getTenant().getId();
        final String realmName = request.getTenant().getRealmName();
        
        try {
            AzureDatabaseLifecycleService.LOGGER.info("Populating database for tenant: {}", realmName);

            // Update status to populating
            callback.update(ContextProvider.instance()
                    .tenantId(tenantId)
                    .resourceStatus(TenantResourceStatus.PROVISIONING_IN_PROGRESS));

            // TODO: Implement proper database population logic
            AzureDatabaseLifecycleService.LOGGER.info("Database population not yet implemented for Azure");

            // Update status to completed
            callback.update(ContextProvider.instance()
                    .tenantId(tenantId)
                    .resourceStatus(TenantResourceStatus.ACTIVE));

            AzureDatabaseLifecycleService.LOGGER.info("Successfully populated database for tenant: {}", realmName);

        } catch (final Exception ex) {
            AzureDatabaseLifecycleService.LOGGER.error("Failed to populate database for tenant: {}: {}", realmName, ex.getMessage(), ex);
            callback.update(ContextProvider.instance()
                    .tenantId(tenantId)
                    .resourceStatus(TenantResourceStatus.PROVISIONING_FAILED)
                    .errorMessage("Database population failed: " + ex.getMessage()));
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_DATABASE_CREATION_FAILED, realmName, ex.getMessage());
        }
    }

    /**
     * Creates an Azure PostgreSQL server with the specified configuration.
     */
    private Server createPostgreSqlServer(final String serverName, final Map<String, Object> dbSettings) {
        try {
            AzureDatabaseLifecycleService.LOGGER.info("Creating Azure PostgreSQL server: {}", serverName);

            // Extract settings from configuration
            final String skuName = (String) dbSettings.getOrDefault("skuName", "B_Gen5_1");
            final Integer storageGB = (Integer) dbSettings.getOrDefault("storageGB", 20);
            final Integer backupRetentionDays = (Integer) dbSettings.getOrDefault("backupRetentionDays", 7);
            final String version = (String) dbSettings.getOrDefault("version", "11");

            return postgreSqlManager.servers()
                .define(serverName)
                .withRegion(location)
                .withExistingResourceGroup(resourceGroupName)
                .withProperties(new ServerPropertiesForDefaultCreate()
                    .withAdministratorLogin(adminUsername)
                    .withAdministratorLoginPassword(adminPassword)
                    .withVersion(ServerVersion.fromString(version))
                    .withStorageProfile(new StorageProfile()
                        .withStorageMB(storageGB * 1024)
                        .withBackupRetentionDays(backupRetentionDays)
                        .withGeoRedundantBackup(com.azure.resourcemanager.postgresql.models.GeoRedundantBackup.DISABLED)))
                .withSku(new com.azure.resourcemanager.postgresql.models.Sku().withName(skuName))
                .create();

        } catch (final ManagementException ex) {
            AzureDatabaseLifecycleService.LOGGER.error("Failed to create Azure PostgreSQL server: {}: {}", serverName, ex.getMessage(), ex);
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_SERVER_CREATION_FAILED, serverName, ex.getMessage());
        }
    }

    /**
     * Creates a database on the specified Azure PostgreSQL server.
     */
    private Database createDatabase(final String serverName, final String databaseName) {
        try {
            AzureDatabaseLifecycleService.LOGGER.info("Creating database: {} on server: {}", databaseName, serverName);

            return postgreSqlManager.databases()
                .define(databaseName)
                .withExistingServer(resourceGroupName, serverName)
                .withCharset("UTF8")
                .withCollation("English_United States.1252")
                .create();

        } catch (final ManagementException ex) {
            AzureDatabaseLifecycleService.LOGGER.error("Failed to create database: {} on server: {}: {}", databaseName, serverName, ex.getMessage(), ex);
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_DATABASE_CREATION_FAILED, databaseName, serverName, ex.getMessage());
        }
    }

    /**
     * Configures firewall rules for the Azure PostgreSQL server.
     */
    private void configureFirewallRules(final String serverName, final String realmName) {
        try {
            AzureDatabaseLifecycleService.LOGGER.info("Configuring firewall rules for server: {}", serverName);

            // Get current IP address
            final String currentIp = ipService.getPublicIPv4();
            final String firewallRuleName = MessageFormat.format(TEMPLATE_FIREWALL_RULE_NAME, realmName.toLowerCase());

            // Create firewall rule to allow current IP
            postgreSqlManager.firewallRules()
                .define(firewallRuleName)
                .withExistingServer(resourceGroupName, serverName)
                .withStartIpAddress(currentIp)
                .withEndIpAddress(currentIp)
                .create();

            AzureDatabaseLifecycleService.LOGGER.info("Created firewall rule: {} for IP: {}", firewallRuleName, currentIp);

        } catch (final Exception ex) {
            AzureDatabaseLifecycleService.LOGGER.error("Failed to configure firewall rules for server: {}: {}", serverName, ex.getMessage(), ex);
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_FIREWALL_RULE_CREATION_FAILED, serverName, ex.getMessage());
        }
    }

    /**
     * Loads tenant database settings from configuration file.
     */
    private Map<String, Object> loadTenantDatabaseSettings(final String category) {
        try {
            final String configFileName = MessageFormat.format(TENANT_DB_SETTINGS_FILE_TEMPLATE, category.toLowerCase());
            final ClassPathResource resource = new ClassPathResource(configFileName);

            if (!resource.exists()) {
                throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.TENANT_DATABASE_SETTINGS_NOT_FOUND, category);
            }

            final Path configPath = resource.getFile().toPath();
            final String content = Files.readString(configPath);

            return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});

        } catch (final IOException ex) {
            AzureDatabaseLifecycleService.LOGGER.error("Failed to load tenant database settings for category: {}: {}", category, ex.getMessage(), ex);
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.TENANT_DATABASE_SETTINGS_NOT_FOUND, category);
        }
    }
}
