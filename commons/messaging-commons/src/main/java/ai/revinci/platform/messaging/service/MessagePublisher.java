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

package ai.revinci.platform.messaging.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.messaging.error.MessagingErrors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePublisher {
    private final StreamBridge streamBridge;
    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Instrumentation
    public <T> void publish(@NonNull final TenantRealm tenantRealm, @NonNull final String bindingName,
                            @NonNull final T messagePayload) {
        publish(tenantRealm, bindingName, messagePayload, Map.of());

    }

    @Instrumentation
    public <T> void publish(@NonNull final TenantRealm tenantRealm, @NonNull final String bindingName,
                            @NonNull final T messagePayload, final Map<String, Object> messageFilters) {
        // 1. Validate that we have the tenant-id and realm.
        final UUID tenantId = tenantRealm.getTenantId();
        final String realm = tenantRealm.getRealm();
        if (Objects.isNull(tenantId) || StringUtils.isBlank(realm)) {
            MessagePublisher.LOGGER.error("Tenant identifier or realm is missing. Cannot publish message.");
            throw ServiceException.of(MessagingErrors.MISSING_TENANT_ID_OR_REALM);
        }

        // 2. Generate the correlation identifier, which will be added to the message header.
        final UUID correlationId = UUID.randomUUID();

        // 3. Stringify the message payload and encrypt.
        final String json = JsonUtils.serialize(messagePayload);
        final String encryptedJson = Strings.encryptUsingSalt(json, Strings.generateSalt(tenantId, realm));

        // 4. Initialize the headers, build the message object and publish the message.
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Key.TENANT_ID.value(), tenantId);
        headers.put(Key.REALM.value(), realm);
        headers.put(Key.SERVICE_NAME.value(), serviceName);

        // 5. Do we have any message filters? If so, add them as headers and skip if the key already exists.
        if (!CollectionUtils.isEmpty(messageFilters)) {
            MessagePublisher.LOGGER.trace("Tenant: {}, Realm: {}. Number of message filters: {}", tenantId, realm,
                                          messageFilters.size());

            messageFilters.entrySet()
                    .stream()
                    .filter(e -> Objects.nonNull(e.getValue()) && !headers.containsKey(e.getKey()))
                    .forEach(e -> headers.put(e.getKey(), e.getValue()));
        }

        MessagePublisher.LOGGER.info(
                "Tenant: {}, Realm: {}. Publishing message to the binding {} with correlation-id: {}", tenantId, realm,
                bindingName, correlationId);

        streamBridge.send(bindingName, MessageBuilder.createMessage(encryptedJson, new MessageHeaders(headers)));

        MessagePublisher.LOGGER.info(
                "Tenant: {}, Realm: {}. Successfully published a message to the binding {} with correlation-id: {}",
                tenantId, realm, bindingName, correlationId);
    }
}
