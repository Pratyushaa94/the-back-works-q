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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import ai.revinci.platform.common.util.Adapter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public abstract class AbstractDataProvider<K, V, T extends AbstractDataProvider<K, V, T>> implements IDataProvider<K,
        V, T> {
    /** Properties associated with this object. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private final Map<K, V> data = new LinkedHashMap<>();

    @JsonIgnore
    @Override
    public T set(final Map<K, V> data) {
        if (Objects.nonNull(data) && !data.isEmpty()) {
            this.data.clear();
            this.data.putAll(data);
        }
        return self();
    }

    @JsonIgnore
    @Override
    public T add(final K key, final V value) {
        if (Objects.nonNull(key)) {
            data.put(key, value);
        }
        return self();
    }

    @JsonIgnore
    @Override
    public T addAll(final Map<K, V> data) {
        if (Objects.nonNull(data) && !data.isEmpty()) {
            this.data.putAll(data);
        }
        return self();
    }

    @JsonIgnore
    @Override
    public Map<K, V> get() {
        return data;
    }

    @JsonIgnore
    @Override
    public V get(final K key) {
        return data.get(key);
    }

    @JsonIgnore
    @Override
    public <R> R get(final K key, final Class<R> targetType) {
        final V value = get(key);
        // Adapt only if the value is non-null.
        if (Objects.nonNull(value)) {
            return Adapter.adapt(value, targetType);
        }
        return null;
    }

    @JsonIgnore
    @Override
    public boolean has(final K key) {
        final V value = get(key);
        if (value instanceof String str) {
            return StringUtils.isNotBlank(str);
        }
        return Objects.nonNull(value);
    }

    /**
     * Returns the instance of this class.
     *
     * @return Instance of this class.
     */
    protected abstract T self();
}

