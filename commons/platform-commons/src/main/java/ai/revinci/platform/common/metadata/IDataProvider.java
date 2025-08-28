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

package ai.revinci.platform.common.metadata;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IDataProvider<K, V, T extends IDataProvider<K, V, T>> extends Serializable {
    /**
     * This method sets the provided properties as the base.
     * <p>
     * Any previous properties are cleared before using the provided properties as the base.
     *
     * @param properties Map containing the properties as key-value pairs.
     *
     * @return Updated instance.
     */
    T set(Map<K, V> properties);

    /**
     * This method attempts to add the provided key / value pair to the internal map.
     *
     * @param key   Key that needs to be added and cannot be null.
     * @param value Value for the specified key.
     *
     * @return Updated instance.
     */
    T add(K key, V value);

    /**
     * This method attempts to add all the key / value pairs in the provided map to the internal map.
     * <p>
     * Note that the existing properties are not cleared unlike the {@code set} method.
     *
     * @param properties Map containing the key / value pairs that needs to be added to the internal map.
     *
     * @return Updated instance.
     */
    T addAll(Map<K, V> properties);

    /**
     * This method returns all the properties as a {@link Map} where the key represents the property name and the value
     * is the property value.
     *
     * @return Properties as a {@link Map} containing the key-value pairs.
     */
    Map<K, V> get();

    /**
     * For the provided key, this method returns the value.
     *
     * @param key Key for which the value needs to be retrieved.
     *
     * @return Value for the provided key.
     */
    V get(K key);

    /**
     * For the provided key, this method attempts to retrieve the value and casts the value to the specified target
     * type.
     *
     * @param key        Key for which the value needs to be retrieved.
     * @param targetType Target type of the value.
     * @param <R>        Target type of the value.
     *
     * @return Value for the specified key and casted to the specified type. Returns null if the value cannot be casted
     *         to the specified type.
     */
    <R> R get(K key, Class<R> targetType);

    /**
     * For the provided key, this method attempts to retrieve the value and casts the value to the specified target
     * type.
     * <p>
     * If the value for the {@code key} is null, this method returns the provided {@code defaultValue}.
     *
     * @param key          Key for which the value needs to be retrieved.
     * @param targetType   Target type of the value.
     * @param defaultValue Default value to send if the value of the key is null.
     * @param <R>          Target type of the value.
     *
     * @return Value for the specified key and cast to the specified type. Returns null if the value cannot be casted to
     *         the specified type.
     */
    default <R> R get(K key, Class<R> targetType, R defaultValue) {
        final R value = get(key, targetType);
        return Objects.nonNull(value) ?
                value :
                defaultValue;
    }

    /**
     * For the provided key, this method attempts to retrieve the value and casts the value to a string before returning
     * it.
     *
     * @param key Key for which the value needs to be retrieved.
     *
     * @return Value for the specified key and cast to a string.
     */
    @JsonIgnore
    default String asString(final K key) {
        return get(key, String.class);
    }

    /**
     * For the provided key, this method attempts to retrieve the value and casts the value to a string before returning
     * it.
     * <p>
     * If the value for the {@code key} is null, this method returns the provided {@code defaultValue}.
     *
     * @param key          Key for which the value needs to be retrieved.
     * @param defaultValue Default to use if the value of the {@code key} is null.
     *
     * @return Value for the specified key and cast to a string.
     */
    @JsonIgnore
    default String asString(final K key, final String defaultValue) {
        final String value = get(key, String.class);
        return Objects.nonNull(value) ?
                value :
                defaultValue;
    }

    /**
     * This method checks if the provided key is present in the properties captured by this instance and returns true /
     * false based on the presence / absence of the key.
     *
     * @param key Key whose presence needs to be checked.
     *
     * @return True if the key is present in the key / value pairs maintained by this instance, false otherwise.
     */
    boolean has(K key);
}