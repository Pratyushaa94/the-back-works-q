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

package ai.revinci.platform.security.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.client.ICacheClient;
import ai.revinci.platform.common.enums.JwtClaim;
import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.security.token.PlatformJwtDecoder;
import ai.revinci.platform.security.util.JwtTokenUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService implements ApplicationContextAware {
    private static final long DEFAULT_REVOKED_TOKEN_EXPIRATION_DURATION_MINUTES = 60L;
    private ApplicationContext applicationContext;
    private final ICacheClient redisCacheClient;

    @Override
    public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Instrumentation
    public void revoke(@NonNull final String token) {
        // Build the key.
        final String key = PatternTemplate.CACHE_KEY_REVOKED_TOKEN.format(token);
        // Put the key.
        redisCacheClient.put(key, Boolean.TRUE, Duration.ofMinutes(tokenExpirationDurationInMinutes(token)));
    }

    @Instrumentation
    public boolean isRevoked(@NonNull final String token) {
        // Build the key.
        final String key = PatternTemplate.CACHE_KEY_REVOKED_TOKEN.format(token);
        // Get the value of the key.
        return redisCacheClient.get(key, Boolean.class)
                .orElse(Boolean.FALSE)
                .equals(Boolean.TRUE);
    }

    private long tokenExpirationDurationInMinutes(@NonNull final String token) {
        try {
            final JwtDecoder jwtDecoder = applicationContext.getBean(PlatformJwtDecoder.class);
            final Jwt jwt = jwtDecoder.decode(token);
            // Token expiration in seconds.
            final long tokenExpirationEpoch = JwtTokenUtils.extractClaim(JwtClaim.EXP, jwt);
            // Current time in seconds
            final long currentEpoch = Instant.now()
                    .getEpochSecond();
            // Duration between the two in seconds.
            final long durationInSeconds = tokenExpirationEpoch - currentEpoch;
            // Convert this duration to minutes.
            return durationInSeconds <= 0 ?
                    5 :
                    Duration.ofSeconds(durationInSeconds)
                            .toMinutes();
        } catch (final Exception ex) {
            TokenRevocationService.LOGGER.warn("Error while extracting expiration from token. Error: {}",
                                               ex.getMessage());
        }
        return TokenRevocationService.DEFAULT_REVOKED_TOKEN_EXPIRATION_DURATION_MINUTES;
    }
}
