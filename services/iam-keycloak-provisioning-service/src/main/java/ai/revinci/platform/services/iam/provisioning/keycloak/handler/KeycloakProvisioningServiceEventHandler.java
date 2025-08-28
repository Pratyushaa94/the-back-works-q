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

package ai.revinci.platform.services.iam.provisioning.keycloak.handler;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.IamProvider;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.messaging.utils.MessageUtils;
import ai.revinci.platform.provisioning.iam.service.IamService;

@Slf4j
@RequiredArgsConstructor
@Component
public class KeycloakProvisioningServiceEventHandler {
    /** A cryptographically secure random number generator. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** A service implementation of type IamService. */
    private final IamService iamService;

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

        KeycloakProvisioningServiceEventHandler.LOGGER.info(
                "Tenant: {}, Realm: {}, Correlation id: {}. Received new db provisioned event",
                tenantId, realm, correlationId);

        // TODO: Need to check if the processing of the message with the correlation-id is required or it has
        //  already been processed.

        try {
            KeycloakProvisioningServiceEventHandler.LOGGER.info(
                    "Tenant: {}, Realm: {}, Correlation id: {}. Provisioning realm", tenantId, realm, correlationId);

            iamService.provisionRealm(IamProvider.KEYCLOAK, tenantId, realm);

            KeycloakProvisioningServiceEventHandler.LOGGER.info(
                    "Tenant: {}, Realm: {}, Correlation id: {}. Successfully provisioned realm", tenantId, realm,
                    correlationId);
        } catch (final Exception ex) {
            KeycloakProvisioningServiceEventHandler.LOGGER.error(
                    "Tenant: {}, Realm: {}, Correlation id: {}. Failures encountered while provisioning realm",
                    tenantId, realm, correlationId, ex);
        }
    }
}
