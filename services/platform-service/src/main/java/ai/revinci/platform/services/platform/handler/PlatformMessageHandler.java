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

package ai.revinci.platform.services.platform.handler;

import java.util.UUID;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.messaging.utils.MessageUtils;
import ai.revinci.platform.services.platform.service.TenantService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformMessageHandler implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Instrumentation
    @Async
    public void handleRealmProvisionedEvent(final Message<String> message) {
        final MessageHeaders headers = message.getHeaders();
        //final UUID correlationId = MessageUtils.getCorrelationId(headers);
        final UUID tenantId = MessageUtils.getTenantId(headers);
        final String realm = MessageUtils.getRealm(headers);

        // TODO: Need to check if the processing of the message with the correlation-id is required or it has
        //  already been processed.

        try {
            // Get the tenant-service from the application context.
            final TenantService tenantService = applicationContext.getBean(TenantService.class);
            PlatformMessageHandler.LOGGER.info(
                    "Tenant: {}. Realm: {}. Received new db provisioned event", tenantId, realm);
            tenantService.tenantOnboarded(tenantId, realm);
        } catch (final Exception ex) {
            PlatformMessageHandler.LOGGER.error(
                    "Tenant: {}, Realm: {}. Failures while handling db provisioned event", tenantId,
                    realm, ex);
        }
    }
}
