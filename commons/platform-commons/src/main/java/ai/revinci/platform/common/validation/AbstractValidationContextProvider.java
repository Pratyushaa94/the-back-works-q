/*
 * Copyright (c) 2025 Revinci AI.
 *
 * All rights reserved. This software is proprietary to and embodies the
 * confidential technology of Revinci AI. Possession,
 * use, duplication, or dissemination of the software and media is
 * authorized only pursuant to a valid written license from
 * Revinci AI Solutions Pvt. Ltd.
 *
 * Unauthorized use of this software is strictly prohibited.
 *
 * THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Revinci AI BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ai.revinci.platform.common.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;

import lombok.NoArgsConstructor;

import com.fasterxml.jackson.databind.JavaType;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.util.Adapter;
import ai.revinci.platform.common.util.JsonUtils;

/**
 * An abstract implementation of the validation context provider.
 * <p>
 * Subclasses can implement and provide any specific validation context.
 *
 * @author Subbu
 */
@NoArgsConstructor
public class AbstractValidationContextProvider {
    /** Map to hold the context data. */
    protected final Map<Object, Object> context = new HashMap<>();

    /**
     * This method returns a boolean value indicating whether the context has the key.
     *
     * @param key Key to check.
     *
     * @return True if the key is present in the context, false otherwise.
     */
    public boolean hasKey(@NonNull final Object key) {
        return context.containsKey(key);
    }

    /**
     * This method puts the provided {@code key} and {@code value} into the context.
     *
     * @param key   Key which needs to be stored.
     * @param value Value corresponding to the key.
     */
    public void put(@NonNull final Object key, @NonNull final Object value) {
        context.put(key, value);
    }

    /**
     * This method retrieves the value from the context for the provided {@code key}.
     *
     * @param key Key to retrieve the value.
     *
     * @return An {@link Optional} wrapping the value if present, otherwise returns an empty {@link Optional}.
     */
    public Optional<Object> get(@NonNull final Object key) {
        return Optional.ofNullable(context.get(key));
    }

    /**
     * This method retrieves the value for the specified {@code key} and converts it to the specified type.
     *
     * @param key        Key to retrieve the value.
     * @param targetType Target type to which the value needs to be converted.
     * @param <T>        Type of the value to be returned.
     *
     * @return An {@link Optional} wrapping the value if present, otherwise returns an empty {@link Optional}.
     */
    public <T> Optional<T> get(@NonNull final Object key, @NonNull final Class<T> targetType) {
        final Optional<Object> value = get(key);
        return value.map(v -> Adapter.adapt(v, targetType));
    }

    /**
     * This method retrieves the value for the specified {@code key} and converts it to the specified type.
     * <p>
     * If the context does not contain the requested {@code key} or a null value existed in the context, a
     * {@link ServiceException} is thrown.
     *
     * @param key        Key to retrieve the value.
     * @param targetType Target type to which the value needs to be converted.
     * @param <T>        Type of the value to be returned.
     *
     * @return Value for the requested {@code key} if present, else throws a {@link ServiceException}.
     */
    public <T> T getOrThrow(@NonNull final Object key, @NonNull final Class<T> targetType) {
        return get(key, targetType).orElseThrow(() -> ServiceException.of(CommonErrors.RESOURCE_NOT_FOUND));
    }

    /**
     * This method retrieves the value for the specified {@code key} and converts it to the specified type.
     *
     * @param key     Key to retrieve the value.
     * @param typeRef Instance of type {@link ParameterizedTypeReference} capturing the target / desired type.
     * @param <T>     Type of the value to be returned.
     *
     * @return An {@link Optional} wrapping the value if present, otherwise returns an empty {@link Optional}.
     */
    public <T> Optional<T> get(@NonNull final Object key, @NonNull final ParameterizedTypeReference<T> typeRef) {
        final Object value = context.get(key);
        if (value == null) {
            return Optional.empty();
        }

        try {
            final JavaType javaType = JsonUtils.OBJECT_MAPPER.constructType(typeRef.getType());
            return Optional.of(JsonUtils.OBJECT_MAPPER.convertValue(value, javaType));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * This method retrieves the value for the specified {@code key} and converts it to the specified type.
     * <p>
     * If the context does not contain the requested {@code key} or a null value existed in the context, a
     * {@link ServiceException} is thrown.
     *
     * @param key     Key to retrieve the value.
     * @param typeRef Instance of type {@link ParameterizedTypeReference} capturing the target / desired type.
     * @param <T>     Type of the value to be returned.
     *
     * @return Value for the requested {@code key} if present, else throws a {@link ServiceException}.
     */
    public <T> T getOrThrow(@NonNull final Object key, @NonNull final ParameterizedTypeReference<T> typeRef) {
        return get(key, typeRef).orElseThrow(
                () -> ServiceException.of(CommonErrors.VALIDATION_CONTEXT_CANNOT_CAST_VALUE));
    }

    /**
     * This method retrieves the value for the specified {@code key} and converts it to the {@link String} type.
     *
     * @param key Key to retrieve the value.
     *
     * @return An {@link Optional} wrapping the value if present, otherwise returns an empty {@link Optional}.
     */
    public Optional<String> getAsString(@NonNull final Object key) {
        return get(key, new ParameterizedTypeReference<>() {
        });
    }

    /**
     * This method retrieves the value for the specified {@code key} and converts it to the {@link String} type.
     *
     * @param key Key to retrieve the value.
     *
     * @return Value for the requested {@code key} if present, else throws a {@link ServiceException}.
     */
    public String getAsStringOrThrow(@NonNull final Object key) {
        return getAsString(key).orElseThrow(
                () -> ServiceException.of(CommonErrors.VALIDATION_CONTEXT_MISSING_KEY, key));
    }
}
