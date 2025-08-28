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

package ai.revinci.platform.services.db.provisioning.azure.configuration;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.enums.ResourceState;
import ai.revinci.platform.messaging.annotation.EnableMessaging;
import ai.revinci.platform.messaging.utils.MessageUtils;
import ai.revinci.platform.provisioning.db.configuration.DatabaseProvisioningConfiguration;
import ai.revinci.platform.provisioning.db.configuration.properties.DatabaseProvisioningProperties;
import ai.revinci.platform.provisioning.db.service.DatabaseService;
import ai.revinci.platform.security.data.model.persistence.PermissionEntity;
import ai.revinci.platform.security.data.repository.PermissionRepository;
import ai.revinci.platform.web.annotation.EnableWebConfiguration;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.fasterxml.jackson.core.type.TypeReference;


@Slf4j
@Import(value = {DatabaseProvisioningConfiguration.class})
@EnableMessaging
@EnableWebConfiguration
@EnableConfigurationProperties(value = {DatabaseProvisioningProperties.class})
@EnableJpaRepositories(basePackageClasses = {PermissionRepository.class})
@EntityScan(basePackageClasses = {PermissionEntity.class})
@Configuration
@RequiredArgsConstructor
public class AzureDatabaseProvisioningServiceConfiguration {

    /** A service implementation of type {@link DatabaseService}. */
    private final DatabaseService databaseService;

    /** Name of the application. */
    @Value("${spring.application.name}")
    private String applicationName;

    /** Azure subscription ID. */
    @Value("${azure.subscription-id}")
    private String subscriptionId;

    /** Azure resource group name. */
    @Value("${azure.resource-group}")
    private String resourceGroupName;

    /**
     * Creates and returns an instance of {@link PostgreSqlManager} for interacting with Azure PostgreSQL.
     *
     * @return An instance of type {@link PostgreSqlManager}.
     */
    @Bean
    public PostgreSqlManager postgreSqlManager() {
        try {
            AzureDatabaseProvisioningServiceConfiguration.LOGGER.info("Initializing Azure PostgreSQL Manager");

            // Create credential using DefaultAzureCredential (supports multiple auth methods)
            final TokenCredential credential = new DefaultAzureCredentialBuilder().build();

            // Create Azure profile for the subscription
            final AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            // Create and return PostgreSQL manager
            return PostgreSqlManager.authenticate(credential, profile);

        } catch (final Exception ex) {
            AzureDatabaseProvisioningServiceConfiguration.LOGGER.error("Failed to initialize Azure PostgreSQL Manager: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to initialize Azure PostgreSQL Manager", ex);
        }
    }

    /**
     * A {@link Consumer} which is registered as a bean that consumes the messages on the topic -
     * {environment}-rvc-platform-tenant-lifecycle-topic.
     *
     * @return A {@link Consumer} that consumes the messages on the topic -
     *         {environment}-rvc-platform-tenant-lifecycle-topic.
     */
    @Bean
    public Consumer<Message<String>> rvcPlatformNewTenantCreatedEvent() {
        return message -> {
            final MessageHeaders headers = message.getHeaders();
            final UUID correlationId = MessageUtils.getCorrelationId(headers);
            final UUID tenantId = MessageUtils.getTenantId(headers);
            final String realm = MessageUtils.getRealm(headers);

            AzureDatabaseProvisioningServiceConfiguration.LOGGER.info(
                    "Tenant: {}, Realm: {}, Correlation id: {}. Received new tenant creation event", tenantId, realm,
                    correlationId);

            // TODO: Need to check if the processing of the message with the correlation-id is required or it has
            //  already been processed.

            final Map<String, Object> data = MessageUtils.getPayload(message, new TypeReference<>() {
            });
            final String resourceLifecycle = data.get(Key.RESOURCE_LIFECYCLE.value())
                    .toString();
            if (!ResourceState.CREATE.is(resourceLifecycle)) {
                AzureDatabaseProvisioningServiceConfiguration.LOGGER.info(
                        "Tenant: {}, Realm: {}, Correlation id: {}. Ignoring the received event {}", tenantId, realm,
                        correlationId, resourceLifecycle);
                return;
            }

            final String cloudProvider = data.get(Key.CLOUD_HYPERVISOR.value())
                    .toString();
            try {
                databaseService.provision(cloudProvider, tenantId, realm);
            } catch (final Exception ex) {
                AzureDatabaseProvisioningServiceConfiguration.LOGGER.error(
                        "Tenant: {}, Realm: {}, Correlation id: {}. Failures while provisioning DB", tenantId, realm,
                        correlationId, ex);
                // TODO: Need to send to DLQ.
            }
        };
    }
}
