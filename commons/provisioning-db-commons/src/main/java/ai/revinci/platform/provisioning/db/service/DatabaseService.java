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

package ai.revinci.platform.provisioning.db.service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.CloudHypervisor;
import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.provisioning.db.configuration.properties.DatabaseProvisioningProperties;
import ai.revinci.platform.provisioning.db.data.model.experience.PopulateTenantDatabase;
import ai.revinci.platform.provisioning.db.data.model.experience.ProvisionTenantDatabase;
import ai.revinci.platform.provisioning.db.enums.DatabaseEngine;
import ai.revinci.platform.provisioning.db.error.DBProvisioningErrors;
import ai.revinci.platform.provisioning.db.service.lifecycle.IDatabaseLifecycleService;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantResourceEntity;
import ai.revinci.platform.provisioning.status.handler.enums.DeploymentEnvironment;
import ai.revinci.platform.provisioning.status.handler.enums.TenantCategory;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceStatus;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceType;
import ai.revinci.platform.provisioning.status.handler.enums.TenantStatus;
import ai.revinci.platform.provisioning.status.handler.listener.ContextProvider;
import ai.revinci.platform.provisioning.status.handler.listener.IProgressListener;
import ai.revinci.platform.provisioning.status.handler.service.ResourceConfigurationGenerator;
import ai.revinci.platform.provisioning.status.handler.service.TenantService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseService implements ApplicationContextAware, IProgressListener {
    private final TenantService tenantService;
    private final Environment environment;
    private final DatabaseProvisioningProperties databaseProvisioningProperties;
    private final ResourceConfigurationGenerator resourceConfigurationGenerator;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Instrumentation
    @Transactional
    @Override
    public void update(@NonNull final ContextProvider contextProvider) {
        tenantService.updateStatus(contextProvider);
    }

    @Instrumentation
    @Async
    @Transactional
    public void provision(@NonNull final String cloudProvider, @NonNull final UUID tenantId,
                          @NonNull final String realm) {
        try {
            DatabaseService.LOGGER.info("Initiated the database provisioning for tenant: {}", tenantId);

            // 1. Check if the tenant identifier exists in the system. If so, do not initiate the provisioning process.
            final Optional<TenantEntity> matchingTenant = tenantService.findByIdOrRealm(tenantId, realm);
            if (matchingTenant.isEmpty()) {
                DatabaseService.LOGGER.warn(
                        "Unable to find a tenant with id {} or realm {}. Skipping the provisioning process.", tenantId,
                        realm);
                return;
            }

            // 2. If the tenant exists and if a resource record already exists, then we skip the provisioning process.
            final TenantEntity tenant = matchingTenant.get();
            final TenantResourceType resourceType = TenantResourceType.PLATFORM_TENANT_DB;
            final Optional<TenantResourceEntity> resource = tenant.findResourceType(resourceType);
            if (resource.isPresent()) {
                DatabaseService.LOGGER.warn(
                        "Tenant: {}. Resource type {} exists for the tenant. Skipping the provisioning process.",
                        tenantId, resourceType);
                return;
            }

            // 3. Update the tenant status to provisioning in progress.
            TenantEntity updatedTenant = tenantService.updateTenantStatus(tenant,
                                                                          TenantStatus.PROVISIONING_IN_PROGRESS);

            // 4. Add a new tenant-resource record in the tenant_resource table.
            final TenantResourceEntity tre = tenantService.addTenantResource(updatedTenant, resourceType);
            DatabaseService.LOGGER.info("Tenant: {}. Added resource and resource configuration records", tenantId);

            // 5. Build the payload for the database provisioning request.
            final CloudHypervisor csp = CloudHypervisor.valueOf(cloudProvider);
            final ProvisionTenantDatabase payload = getPayloadForProvisioningTenantDB(updatedTenant, tre, csp);
            if (Objects.isNull(payload)) {
                DatabaseService.LOGGER.error("Tenant: {}. Unable to build the payload for database provisioning",
                                             tenantId);
                return;
            }

            // 6. Get an instance of IDatabaseLifecycleService for the specified cloud provider and delegate.
            final String beanName = csp.getBeanName();
            DatabaseService.LOGGER.info("Tenant: {}. Delegating to {} bean for provisioning in cloud", tenantId,
                                        beanName);
            getDatabaseLifecycleService(tenantId, csp).provisionDatabase(payload, this);
        } catch (final Exception ex) {
            DatabaseService.LOGGER.error("Tenant: {}. Errors encountered while provisioning database. Error: {}",
                                         tenantId, ex.getMessage(), ex);
        }
    }

    @Instrumentation
    @Async
    @Transactional
    public void handleDatabaseProvisioningAsyncUpdates(@NonNull final ContextProvider contextProvider) {
        final String operationId = contextProvider.operationIdForCreateDbInstance();
        try {
            final TenantResourceStatus resourceStatus = contextProvider.resourceStatus();
            if (StringUtils.isBlank(operationId) || Objects.isNull(resourceStatus)) {
                DatabaseService.LOGGER.warn("Operation id or resource status is empty. Skipping the update process.");
                return;
            }

            DatabaseService.LOGGER.info("Received an async update request for Operation Id: {}, Status: {}",
                                        operationId, resourceStatus);

            // 1. Find the tenant with a configuration key "createDBInstanceOperationId" and value as the provided
            // operationId parameter.
            final String key = Key.OPERATION_ID_CREATE_DB_INSTANCE.value();
            final Optional<TenantEntity> tenant = tenantService.findTenantWithResourceConfigurationKey(key,
                                                                                                       operationId);
            if (tenant.isEmpty()) {
                DatabaseService.LOGGER.warn("Unable to find a tenant with resource configuration key: {}, value: {}",
                                            key, operationId);
                return;
            }

            // 2. Find the resource with the matching externalId.
            final TenantEntity matchingTenant = tenant.get();
            final UUID tenantId = matchingTenant.getId();
            final Optional<TenantResourceEntity> resource = matchingTenant.findResourceWithConfigurationKey(key,
                                                                                                            operationId);
            if (resource.isEmpty()) {
                DatabaseService.LOGGER.warn(
                        "Tenant: {}. Unable to find a tenant resource with configuration key: {}, value: {}", tenantId,
                        key, operationId);
                return;
            }
            final TenantResourceEntity matchingResource = resource.get();
            final UUID resourceId = matchingResource.getId();

            // 3. Add the tenantId and resourceId to the context-provider and delegate to tenantService for updates.
            contextProvider.tenantId(tenantId)
                    .resourceId(resourceId);

            tenantService.updateStatus(contextProvider);

            // 4. If the resource status is provisioning completed, then we need to run the post-provisioning steps.
            if (TenantResourceStatus.PROVISIONING_COMPLETED.equals(resourceStatus)) {
                populateDatabase(matchingTenant, matchingResource, contextProvider);
            }
        } catch (final Exception ex) {
            DatabaseService.LOGGER.error(
                    "Operation Id: {}. Errors encountered while processing async updates. Error: {}", operationId,
                    ex.getMessage(), ex);
        }
    }


    @Instrumentation
    protected void populateDatabase(final TenantEntity tenant, final TenantResourceEntity resource,
                                    final ContextProvider contextProvider) {
        try {
            // 1. Get the payload for populating the tenant-specific database.
            final PopulateTenantDatabase payload = getPayloadForPopulatingTenantDB(tenant, resource, contextProvider);
            final UUID tenantId = tenant.getId();

            // 2. Get the bean name from the cloud provider and retrieve the bean instance from application context.
            final CloudHypervisor cloudProvider = payload.getCloudHypervisor();
            final String beanName = cloudProvider.getBeanName();
            DatabaseService.LOGGER.debug("Tenant: {}. Delegating to {} bean for database population", tenantId,
                                         beanName);
            getDatabaseLifecycleService(tenantId, cloudProvider).populateDatabase(payload, this);
        } catch (final Exception ex) {
            DatabaseService.LOGGER.error("Tenant: {}. Errors encountered while populating database. Error: {}",
                                         tenant.getId(), ex.getMessage(), ex);
        }
    }

    private IDatabaseLifecycleService getDatabaseLifecycleService(final UUID tenantId,
                                                                  final CloudHypervisor providerType) {
        // 1. Get the bean name from the provider type.
        final String beanName = providerType.getBeanName();
        DatabaseService.LOGGER.debug("Tenant: {}. Retrieving {} bean from the context", tenantId, beanName);

        // 2. Get the bean from the application context.
        return applicationContext.getBean(beanName, IDatabaseLifecycleService.class);
    }

    private ProvisionTenantDatabase getPayloadForProvisioningTenantDB(final TenantEntity tenant,
                                                                      final TenantResourceEntity resource,
                                                                      final CloudHypervisor csp) {
        if (CloudHypervisor.AZURE.equals(csp)) {
            return getAzurePayloadForProvisioningTenantDB(tenant, resource);
        }

        return null;
    }

    private PopulateTenantDatabase getPayloadForPopulatingTenantDB(final TenantEntity tenant,
                                                                   final TenantResourceEntity resource,
                                                                   final ContextProvider contextProvider) {
        // 1. Extract all the required information.
        final UUID tenantId = tenant.getId();
        final String realm = tenant.getRealmName();
        final UUID resourceId = resource.getId();

        // Account / Project id.
        final String accountId = resource.findConfigurationKey(Key.ACCOUNT_ID.value(), StringUtils.EMPTY);
        // Database instance name.
        final String dbInstanceName = resource.findConfigurationKey(Key.DB_INSTANCE_NAME.value(), StringUtils.EMPTY);
        // Schema name.
        final String schema = resource.findConfigurationKey(Key.SCHEMA.value(), PlatformDefaults.SCHEMA_PUBLIC.value());
        // Database name.
        final String dbName = resource.findConfigurationKey(Key.DB_NAME.value(), StringUtils.EMPTY);
        // Encrypted password for the user.
        final String encryptedPwd = resource.findConfigurationKey(Key.PASSWORD.value())
                .orElseThrow(() -> ServiceException.of(
                        DBProvisioningErrors.MISSING_RESOURCE_CONFIGURATION_KEY,
                        Key.PASSWORD.value(), resourceId));
        final byte[] password = decryptPassword(encryptedPwd, tenant.getSecret());


        // 2 Build the payload to populate the database.
        final CloudHypervisor cloudProvider = contextProvider.cloudProvider();
        return PopulateTenantDatabase.builder()
                .cloudHypervisor(cloudProvider)
                .accountId(accountId)
                .database(PopulateTenantDatabase.Database.builder()
                                  .instanceName(dbInstanceName)
                                  .schema(schema)
                                  .dbName(dbName)
                                  .rootPassword(password)
                                  .build())
                .tenant(PopulateTenantDatabase.Tenant.builder()
                                .id(tenantId)
                                .resourceId(resourceId)
                                .realmName(realm)
                                .name(tenant.getName())
                                .secret(tenant.getSecret())
                                .configuration(tenant.getConfiguration())
                                .build())
                .build();
    }

    private byte[] decryptPassword(final String encryptedPassword, final String secret) {
        return Strings.decrypt(encryptedPassword, secret.getBytes())
                .getBytes();
    }

    private ProvisionTenantDatabase getAzurePayloadForProvisioningTenantDB(final TenantEntity tenant,
                                                                          final TenantResourceEntity resource) {
        final TenantCategory category = TenantCategory.valueOf(tenant.getCategory()
                                                                       .getCode());
        final String realm = tenant.getRealmName();
        final String[] activeProfiles = environment.getActiveProfiles();
        // 1. Which environment are we dealing with?
        final DeploymentEnvironment env = DeploymentEnvironment.findMatchingLowestEnvironment(activeProfiles);

        // 2. Currently supporting only Azure.
        final DatabaseProvisioningProperties.AzureDefaults azure = databaseProvisioningProperties.getAzure();
        final DatabaseEngine dbEngine = DatabaseEngine.valueOf(azure.getDatabaseEngine());
        final String dbUsername = resourceConfigurationGenerator.generateTenantDbUsername(category);
        final String dbInstanceName = resourceConfigurationGenerator.generateTenantDbInstanceName(env, category, realm);
        final String schema = resourceConfigurationGenerator.generateTenantDbSchemaName(category, realm);
        final String dbName = resourceConfigurationGenerator.generateTenantDbName(env, category, realm);
        final String jdbcUrl = resourceConfigurationGenerator.generateTenantDbJdbcUrl(category, schema);
        final String encryptedDbPassword = resource.findConfigurationKey(Key.PASSWORD.value())
                .orElseThrow(() -> ServiceException.of(
                        DBProvisioningErrors.MISSING_RESOURCE_CONFIGURATION_KEY,
                        Key.PASSWORD.value(), resource.getId()));
        final byte[] password = decryptPassword(encryptedDbPassword, tenant.getSecret());


        return ProvisionTenantDatabase.builder()
                .deploymentEnvironment(env)
                .cloudHypervisor(CloudHypervisor.AZURE)
                .region(azure.getRegion())
                .accountId(azure.getAccountId())
                .tenant(ProvisionTenantDatabase.Tenant.builder()
                                .id(tenant.getId())
                                .resourceId(resource.getId())
                                .realmName(realm)
                                .name(tenant.getName())
                                .secret(tenant.getSecret())
                                .category(category)
                                .configuration(tenant.getConfiguration())
                                .build())
                .database(ProvisionTenantDatabase.Database.builder()
                                  .engine(dbEngine)
                                  .instanceName(dbInstanceName)
                                  .schema(schema)
                                  .dbName(dbName)
                                  .username(dbUsername)
                                  .rootPassword(password)
                                  .jdbcUrl(jdbcUrl)
                                  .build())
                .build();
    }


}
