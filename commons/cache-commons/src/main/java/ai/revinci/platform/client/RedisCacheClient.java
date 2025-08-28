/*
 *
 *  * Copyright (c) 2025 Revinci AI.
 *  *
 *  * All rights reserved. This software is proprietary to and embodies the
 *  * confidential technology of Revinci AI. Possession,
 *  * use, duplication, or dissemination of the software and media is
 *  * authorized only pursuant to a valid written license from Revinci AI.
 *  *
 *  * Unauthorized use of this software is strictly prohibited.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 *  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL REVINCI AI BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package ai.revinci.platform.client;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.error.CacheErrors;

/**
 * A client for interacting with Redis cache.
 * Implements the {@link ICacheClient} interface to provide caching functionality.
 *
 * @author Subbu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheClient implements ICacheClient{

     /** Instance of type {@link RedisTemplate}. */
    private final RedisTemplate<String, Object> redisTemplate;

    @Instrumentation
    @Override
    public <T> void put(@NonNull final String key, @NonNull final T value) {
        put(key, value, null);
    }

    /**
     * Puts a value into the cache with the specified key and timeout.
     *
     * @param key     the key under which the value is stored
     * @param value   the value to be stored
     * @param timeout the duration after which the key will expire, can be null
     * @param <T>     the type of the value
     */
    @Instrumentation
    @Override
    public <T> void put(@NonNull final String key, @NonNull final T value, final Duration timeout) {
        final String keyToUse = constructCacheKey(key);
        try {
            final Object serializedValue = serializeFromCache(value);
            if (Objects.isNull(serializedValue)) {
                RedisCacheClient.LOGGER.warn("Cannot insert into cache as Value provided for key {} is null.", keyToUse);
                return;
            }

            if (Objects.isNull(timeout)) {
                redisTemplate.opsForValue()
                        .set(keyToUse, serializedValue);
            } else {
                redisTemplate.opsForValue()
                        .set(keyToUse, serializedValue, timeout);
            }
        } catch (final Exception ex) {
            RedisCacheClient.LOGGER.error("Failed to put value for key: {}", keyToUse, ex);
            throw ServiceException.of(CacheErrors.CACHE_OPERATION_FAILED, keyToUse);
        }
    }

    @Instrumentation
    @Override
    public <T> Optional<T> get(@NonNull final String key, @NonNull final Class<T> type) {
        final String keyToUse = constructCacheKey(key);
        try {
            final Object value = redisTemplate.opsForValue()
                    .get(keyToUse);
            return Optional.ofNullable(value)
                    .map(v -> deserializeFromCache(v, type));
        } catch (final Exception ex) {
            RedisCacheClient.LOGGER.error("Failed to retrieve value for key: {}", keyToUse, ex);
            throw ServiceException.of(CacheErrors.CACHE_OPERATION_FAILED, keyToUse);
        }
    }

    @Instrumentation
    public void deleteFromCache(@NonNull final String key) {
        final String keyToUse = constructCacheKey(key);
        try {
            redisTemplate.delete(keyToUse);
        } catch (final Exception ex) {
            RedisCacheClient.LOGGER.error("Failed to delete key: {}", keyToUse, ex);
            throw ServiceException.of(CacheErrors.CACHE_OPERATION_FAILED, keyToUse);
        }
    }



    /**
     * This method serializes the provided {@code value} to a JSON string.
     *
     * @param value Value to be serialized.
     *
     * @return Serialized JSON string.
     */
    private Object serializeFromCache(final Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        if (isPrimitiveOrBasicType(value.getClass())) {
            return value;
        }

        return JsonUtils.serialize(value);
    }

    /**
     * This method attempts to deserialize the provided {@code value} to the specified {@code type}.
     *
     * @param value Value to be deserialized.
     * @param type  Type to be deserialized to.
     * @param <T>   Type of the value.
     *
     * @return Deserialized value.
     */
    private <T> T deserializeFromCache(final Object value, final Class<T> type) {
        if (Objects.isNull(value)) {
            return null;
        }

        if (isPrimitiveOrBasicType(type)) {
            return type.cast(value);
        }

        try {
            if (value instanceof String jsonString) {
                return JsonUtils.deserialize(jsonString, type);
            }

            return JsonUtils.OBJECT_MAPPER.convertValue(value, type);
        } catch (final Exception ex) {
            RedisCacheClient.LOGGER.error("Failed to deserialize value", ex);
            if (ex instanceof ServiceException) {
                throw ex;
            }

            throw ServiceException.of(CacheErrors.FAILED_TO_DESERIALIZE_CACHE_VALUE);
        }
    }

    /**
     * Checks if the provided {@code type} is a primitive type or a basic type like
     * {@link Number}, {@link Boolean}, or {@link String}.
     * @param type
     * @return
     */
    private boolean isPrimitiveOrBasicType(final Class<?> type) {
        return type.isPrimitive() || Number.class.isAssignableFrom(type) || type.equals(Boolean.class) || type.equals(
                String.class);
    }

    /**
     * Constructs a cache key using the provided key and the current tenant's realm.
     * If the realm is blank, it returns the key as is.
     *
     * @param key
     * @return
     */
    private String constructCacheKey(final String key) {
        final String realm = TenantContext.realm();
        return StringUtils.isBlank(realm) ?
                key :
                PatternTemplate.TENANT_CACHE_KEY_TEMPLATE.format(realm, key);
    }

    @Instrumentation
    @Override
    public void delete(@NonNull final String key) {
        final String keyToUse = buildKey(key);
        try {
            redisTemplate.delete(keyToUse);
        } catch (final Exception ex) {
            RedisCacheClient.LOGGER.error("Failed to delete key: {}", keyToUse, ex);
            throw ServiceException.of(CacheErrors.CACHE_OPERATION_FAILED, keyToUse);
        }
    }

    private String buildKey(final String key) {
        final String realm = TenantContext.realm();
        return StringUtils.isBlank(realm) ?
                key :
                PatternTemplate.TENANT_CACHE_KEY_TEMPLATE.format(realm, key);
    }
}
