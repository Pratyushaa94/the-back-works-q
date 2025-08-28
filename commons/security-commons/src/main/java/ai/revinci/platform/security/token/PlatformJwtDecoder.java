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

package ai.revinci.platform.security.token;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.JwtClaim;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.security.error.SecurityErrors;
import ai.revinci.platform.security.service.TokenRevocationService;
import ai.revinci.platform.security.util.JwtTokenUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformJwtDecoder implements JwtDecoder {
    private final Map<String, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();

    private final TokenRevocationService tokenRevocationService;

    @Override
    public Jwt decode(final String token) throws JwtException {
        // 1. Extract the issuer from the token (without decoding it fully)
        final String issuer = JwtTokenUtils.extractIssuerFromTokenOrThrow(token);

        // 2. Get or create a JwtDecoder for the extracted issuer
        final JwtDecoder jwtDecoder = jwtDecoders.computeIfAbsent(issuer, JwtTokenUtils::createJwtDecoder);

        try {
            // 3. Decode the JWT using the appropriate JwtDecoder
            final Jwt jwt = jwtDecoder.decode(token);

            // 4. Verify that the issuer in the token matches the one we extracted
            final String issuerValue = jwt.getIssuer()
                    .toString();
            if (!issuerValue.equals(issuer)) {
                PlatformJwtDecoder.LOGGER.error("Issuer mismatch. Expected: {}, Actual: {}", issuer, issuerValue);
                throw ServiceException.of(SecurityErrors.TOKEN_ISSUER_MISMATCH);
            }

            // 5. Is this token valid (i.e., has the user with this token logged out in which case, this is invalid).
            if (tokenRevocationService.isRevoked(token)) {
                PlatformJwtDecoder.LOGGER.error(
                        "Invalid token provided. The token has been revoked as the user must have logged out");
                throw ServiceException.of(SecurityErrors.TOKEN_INVALID);
            }

            // Extend the JWT with additional claims.
            return enhanceJwtWithAdditionalClaims(jwt);
        } catch (final JwtValidationException jve) {
            PlatformJwtDecoder.LOGGER.error("Token validation failed. Error message: {}", jve.getMessage(), jve);
            throw ServiceException.of(SecurityErrors.TOKEN_INVALID);
        } catch (final JwtException e) {
            PlatformJwtDecoder.LOGGER.error("Failed to decode token. Error message: {}", e.getMessage(), e);
            throw ServiceException.of(SecurityErrors.TOKEN_DECODE_FAILED);
        }
    }

    private Jwt enhanceJwtWithAdditionalClaims(final Jwt jwt) {
        final Map<String, Object> claims = new HashMap<>(jwt.getClaims());
        claims.put(JwtClaim.REALM.value(), JwtTokenUtils.extractRealmFromIssuer(jwt.getIssuer()
                                                                                        .toString()));

        // Return the extended JWT
        return new Jwt(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getHeaders(), claims);
    }
}
