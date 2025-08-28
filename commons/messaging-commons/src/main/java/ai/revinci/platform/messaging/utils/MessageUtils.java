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

package ai.revinci.platform.messaging.utils;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.messaging.error.MessagingErrors;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
public final class MessageUtils {

    private MessageUtils() {
        throw new IllegalStateException("Cannot create instances of this class");
    }
    public static UUID getTenantId(@NonNull final MessageHeaders headers) {
        final String tenantId = MessageUtils.getHeaderAsString(headers, Key.TENANT_ID.value());
        if (StringUtils.isBlank(tenantId)) {
            MessageUtils.LOGGER.error("Tenant identifier is missing in the message headers.");
            throw ServiceException.of(MessagingErrors.MISSING_TENANT_ID_OR_REALM);
        }
        return UUID.fromString(tenantId);
    }


    /**
     * This method introspects into the provided {@code headers} and extracts the value of the header named
     * {@code correlationId}.
     *
     * @param headers Instance of type {@link MessageHeaders}, which represents the headers on the {@link Message}
     *                object.
     *
     * @return Correlation identifier, which is extracted from the header named {@code correlationId}.
     */
    public static UUID getCorrelationId(@NonNull final MessageHeaders headers) {
        final String correlationId = MessageUtils.getHeaderAsString(headers, Key.CORRELATION_ID.value());
        if (StringUtils.isBlank(correlationId)) {
            MessageUtils.LOGGER.error("Correlation identifier is missing in the message headers.");
            throw ServiceException.of(MessagingErrors.MISSING_CORRELATION_ID);
        }
        return UUID.fromString(correlationId);
    }

    /**
     * This method introspects into the provided {@code headers} and extracts the value of the header named realm.
     * @param headers
     * @return
     */

    public static String getRealm(@NonNull final MessageHeaders headers) {
        final String realm = MessageUtils.getHeaderAsString(headers, Key.REALM.value());
        if (StringUtils.isBlank(realm)) {
            MessageUtils.LOGGER.error("Realm is missing in the message headers.");
            throw ServiceException.of(MessagingErrors.MISSING_TENANT_ID_OR_REALM);
        }
        return realm;
    }


    public static <T> T getHeader(@NonNull final MessageHeaders headers, @NonNull final String key,
                                  @NonNull final Class<T> targetType) {
        return headers.get(key, targetType);
    }

    public static String getHeaderAsString(@NonNull final MessageHeaders headers, @NonNull final String key) {
        return MessageUtils.getHeader(headers, key, String.class);
    }

    public static <T> T getPayload(@NonNull final Message<String> message, @NonNull final TypeReference<T> targetType) {
        // 1. Get the message headers and the message payload.
        final MessageHeaders headers = message.getHeaders();
        final String payload = message.getPayload();

        // 2. Get the tenant-id and realm from the headers.
        final UUID tenantId = MessageUtils.getTenantId(headers);
        final String realm = MessageUtils.getRealm(headers);
        // 3. Decrypt the payload.
        final String decryptedPayload = Strings.decryptUsingSalt(payload, Strings.generateSalt(tenantId, realm));

        // 4. De-serialize the payload to IMessagePayload.
        MessageUtils.LOGGER.debug("Tenant: {}, Realm: {}. De-serializing message payload", tenantId,
                                  realm);

        final T messageData = JsonUtils.deserialize(decryptedPayload, targetType);

        MessageUtils.LOGGER.debug(
                "Tenant: {}, Realm: {}. Successfully de-serialized message payload", tenantId,
                realm);

        return messageData;
    }


}
