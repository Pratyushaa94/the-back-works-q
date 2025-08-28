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

package ai.revinci.platform.services.notification.handler;

import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.messaging.utils.MessageUtils;
import ai.revinci.platform.multitenancy.service.TenantDataSourceRefreshListener;
import ai.revinci.platform.notification.model.NotificationMessage;
import ai.revinci.platform.services.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMessageHandler {
    /** A service implementation of type {@link NotificationService}. */
    private final NotificationService notificationService;

    /** Instance of type {@link TenantDataSourceRefreshListener}. */
    private final TenantDataSourceRefreshListener tenantDataSourceRefreshListener;

    /**
     * This method handles the incoming notification message.
     *
     * @param message A {@link Message} object containing the payload.
     */
    @Instrumentation
    @Async
    public void handleNotificationEvent(final Message<String> message) {
        // 1. Extract the relevant details.
        final MessageHeaders headers = message.getHeaders();
        final UUID correlationId = MessageUtils.getCorrelationId(headers);
        final UUID tenantId = MessageUtils.getTenantId(headers);
        final String realm = MessageUtils.getRealm(headers);
        // Extract the payload.
        final NotificationMessage nm = MessageUtils.getPayload(message, new TypeReference<>() {
        });

        // TODO: Need to check if the processing of the message with the correlation-id is required or it has
        //  already been processed.

        NotificationMessageHandler.LOGGER.info(
                "Tenant: {}, Realm: {}, Correlation id: {}. Received notification message", tenantId, realm,
                correlationId);

        try {
            // 1. Set the tenant context to the incoming realm so that the appropriate database will be picked.
            NotificationMessageHandler.LOGGER.info(
                    "Tenant: {}, Realm: {}, Correlation id: {}. Initializing tenant context", tenantId, realm,
                    correlationId);
            TenantContext.set(TenantRealm.builder()
                                      .realm(realm)
                                      .tenantId(tenantId)
                                      .build());

            // 2. Send the notification.
            notificationService.send(nm);
        } catch (final Exception ex) {
            NotificationMessageHandler.LOGGER.error(
                    "Tenant: {}, Realm: {}, Correlation id: {}. Failures while sending notification", tenantId, realm,
                    correlationId, ex);
        } finally {
            TenantContext.clear();
        }
    }

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

        NotificationMessageHandler.LOGGER.info(
                "Tenant: {}. Realm: {}, Correlation id: {}. Received new db provisioned event", tenantId, realm,
                correlationId);
        try {
            tenantDataSourceRefreshListener.tenantProvisioned(tenantId, realm);
        } catch (final Exception ex) {
            NotificationMessageHandler.LOGGER.error(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Failures while processing new db provisioned event.",
                    tenantId, realm, correlationId, ex);
            // TODO: Need to send to DLQ.
        }
    }
}

