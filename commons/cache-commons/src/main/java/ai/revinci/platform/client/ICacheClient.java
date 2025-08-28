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

// @author: Subbu

package ai.revinci.platform.client;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ICacheClient {

    /**
     * Puts a value into the cache with the specified key.
     * @param key
     * @param value
     * @param <T>
     */
    <T> void put(String key, T value);

    /**
     *
     * @param key
     * @param value
     * @param timeout
     * @param <T>
     */
    <T> void put(String key, T value, Duration timeout);

    /**
     * Retrieves a value from the cache by its key.
     * @param key
     * @param type
     * @return
     * @param <T>
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Retrieves a value from the cache by its key, with a default value if not found.
     * @param key
     */
    void delete(String key);


}
