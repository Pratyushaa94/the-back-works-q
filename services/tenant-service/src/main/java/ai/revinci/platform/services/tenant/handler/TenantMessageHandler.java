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

package ai.revinci.platform.services.tenant.handler;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.client.ICacheClient;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.configuration.TenantConfiguration;
import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.common.util.ThreadUtils;
import ai.revinci.platform.messaging.utils.MessageUtils;
import ai.revinci.platform.multitenancy.datasource.RoutingDataSource;
import ai.revinci.platform.multitenancy.service.TenantDataSourceRefreshListener;
import ai.revinci.platform.services.tenant.service.TenantService;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMessageHandler {
    /** A cryptographically secure random number generator. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** A routing data source instance of type {@link RoutingDataSource}. */
    private final RoutingDataSource routingDataSource;

    /** Instance of type {@link TenantDataSourceRefreshListener}. */
    private final TenantDataSourceRefreshListener tenantDataSourceRefreshListener;


    /** A service implementation of type {@link TenantService}. */
    private final TenantService tenantService;

    /** An instance of type {@link ICacheClient}. */
    private final ICacheClient redisCacheClient;

    /**
     * This method handles the incoming database provisioned message.
     *
     * @param message A {@link Message} object containing the payload.
     */
    @Instrumentation
    @Async
    public void handleDbProvisionedEvent(final Message<String> message) {
        final MessageHeaders headers = message.getHeaders();
        final UUID correlationId = MessageUtils.getCorrelationId(headers);
        final UUID tenantId = MessageUtils.getTenantId(headers);
        final String realm = MessageUtils.getRealm(headers);

        // TODO: Need to check if the processing of the message with the correlation-id is required or it has
        //  already been processed.

        TenantMessageHandler.LOGGER.info("Tenant: {}. Realm: {}, Correlation id: {}. Received new db provisioned event",
                                         tenantId, realm, correlationId);
        try {
            tenantDataSourceRefreshListener.tenantProvisioned(tenantId, realm);
        } catch (final Exception ex) {
            TenantMessageHandler.LOGGER.error(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Failures while processing new db provisioned event",
                    tenantId, realm, correlationId, ex);
            // TODO: Need to send to DLQ.
        }

        // Does the routing datasource now have the new tenant? If not, let us not even move to the next steps as we
        // know that they will lead to exceptions.
        if (!routingDataSource.hasTenantDataSource(realm)) {
            TenantMessageHandler.LOGGER.warn(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Datasource not found for tenant and realm combination",
                    tenantId, realm, correlationId);
            return;
        }

        // The above block makes sure that the datasource for the newly onboarded tenant is available.
        // We can now sync the roles, users and role-mappings from the keycloak to the tenant-specific database.
        final String key = PatternTemplate.CACHE_KEY_TENANT_DB_POST_PROVISIONING.format(realm);
        try {
            final boolean postProvisioningInProgress = isDbPostProvisioningInProgress(tenantId, realm, correlationId);
            if (postProvisioningInProgress) {
                TenantMessageHandler.LOGGER.info(
                        "Tenant: {}, Realm: {}, Correlation id: {}. Tenant db post-provisioning is already in progress",
                        tenantId, realm, correlationId);
                return;
            }

            redisCacheClient.put(key, Boolean.TRUE);
            TenantContext.set(TenantRealm.builder()
                                      .realm(realm)
                                      .tenantId(tenantId)
                                      .build());

            // Get the payload from the incoming message.
            final Map<String, Object> data = MessageUtils.getPayload(message, new TypeReference<>() {
            });

            // Tenant name and secret.
            final String name = data.get(Key.NAME.value())
                    .toString();
            final String secret = data.get(Key.SECRET.value())
                    .toString();
            final String config = data.get(Key.TENANT_CONFIGURATION.value())
                    .toString();
            final TenantConfiguration configuration = JsonUtils.deserialize(config, TenantConfiguration.class);

            TenantMessageHandler.LOGGER.info("Tenant: {}. Realm: {}, Correlation id: {}. Syncing new tenant data",
                                             tenantId, realm, correlationId);
            tenantService.syncNewTenant(tenantId, realm, name, secret, configuration);
        } catch (final Exception ex) {
            TenantMessageHandler.LOGGER.error(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Failures while syncing new tenant data", tenantId,
                    realm, correlationId, ex);
        } finally {
            // Clear the context.
            TenantContext.clear();
            redisCacheClient.delete(key);
        }
    }


    /**
     * This method checks if the post-provisioning is already in progress.
     *
     * @param tenantId      Unique identifier of the tenant.
     * @param realm         Realm name of the tenant.
     * @param correlationId Unique identifier of the message.
     *
     * @return True if the post-provisioning is in progress, false otherwise.
     */
    private boolean isDbPostProvisioningInProgress(final UUID tenantId, final String realm, final UUID correlationId) {
        final String key = PatternTemplate.CACHE_KEY_TENANT_DB_POST_PROVISIONING.format(realm);
        try {
            ThreadUtils.sleep(TimeUnit.MILLISECONDS, TenantMessageHandler.RANDOM.nextInt(100));
            return redisCacheClient.get(key, Boolean.class)
                    .orElse(false);
        } catch (final Exception ex) {
            TenantMessageHandler.LOGGER.error(
                    "Tenant: {}, Realm: {}, Correlation id: {}. Error while checking post-processing check", tenantId,
                    realm, correlationId, ex);
        }

        return false;
    }
}