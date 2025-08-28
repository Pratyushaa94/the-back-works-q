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

package ai.revinci.platform.security.util;

import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.JwtClaim;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.enums.URI;
import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.security.error.SecurityErrors;

@Slf4j
public final class JwtTokenUtils {
    private JwtTokenUtils() {
        throw new IllegalStateException("Cannot create instances of this class");
    }

    public static String extractIssuerFromToken(@NonNull final String token) {
        if (StringUtils.isBlank(token)) {
            JwtTokenUtils.LOGGER.warn("Token is null or empty");
            return StringUtils.EMPTY;
        }

        try {
            // 1. Split the token by ".".
            final String[] parts = token.split(Token.DOT_IN_REGEXP.value());
            if (parts.length != 3) {
                JwtTokenUtils.LOGGER.error("Invalid JWT token. Found {} parts instead of 3", parts.length);
                throw ServiceException.of(SecurityErrors.TOKEN_INVALID);
            }

            // 2. Decode the payload (second part of the JWT)
            final String payload = new String(Base64.getUrlDecoder()
                                                      .decode(parts[1]));

            // 3. Extract the issuer (iss) from the payload
            final String strippedPayload = payload.replaceAll(Token.DOUBLE_QUOTE_IN_REGEXP.value(), StringUtils.EMPTY);
            // Check if "iss:" exists. If so, get its index.
            final int issuerIndex = strippedPayload.indexOf(JwtClaim.ISS.value()
                                                                    .concat(Token.COLON.value()));
            return strippedPayload.substring(issuerIndex + 4, strippedPayload.indexOf(Token.COMMA.value(), issuerIndex))
                    .trim();
        } catch (final Exception e) {
            JwtTokenUtils.LOGGER.error("Failed to extract issuer from token: {}", e.getMessage(), e);
            throw ServiceException.of(SecurityErrors.TOKEN_ISSUER_EXTRACTION_FAILED);
        }
    }

    public static String extractIssuerFromTokenOrThrow(@NonNull final String token) {
        final String issuer = JwtTokenUtils.extractIssuerFromToken(token);
        if (StringUtils.isBlank(issuer)) {
            JwtTokenUtils.LOGGER.error("Unable to extract issuer from token");
            throw ServiceException.of(SecurityErrors.TOKEN_ISSUER_NOT_FOUND);
        }

        return issuer;
    }

    public static String extractRealmFromToken(@NonNull final String token) {
        final String issuer = JwtTokenUtils.extractIssuerFromToken(token);
        if (StringUtils.isNotBlank(issuer)) {
            return JwtTokenUtils.extractRealmFromIssuer(issuer);
        }

        return StringUtils.EMPTY;
    }


    public static String extractRealmFromIssuer(@NonNull final String issuer) {
        return issuer.substring(issuer.lastIndexOf(Token.FORWARD_SLASH.value()) + 1);
    }
    public static JwtDecoder createJwtDecoder(@NonNull final String issuer) {
        // Build the issuer JWK-set URI.
        final String jwkSetUri = MessageFormat.format(URI.ISSUER_JWK_SET.value(), issuer);
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();
    }

    public static <T> T extractClaim(@NonNull final JwtClaim claim, @NonNull final Jwt token) {
        return token.getClaim(claim.value());
    }

    public static String extractPrincipal(@NonNull final Jwt jwt) {
        return JwtTokenUtils.extractClaim(JwtClaim.PREFERRED_USERNAME, jwt);
    }

    public static Collection<GrantedAuthority> extractAuthorities(@NonNull final Jwt jwt) {
        // 1. Extract the realm_access claim from the JWT token.
        final Map<String, Object> realmAccess = JwtTokenUtils.extractClaim(JwtClaim.REALM_ACCESS, jwt);
        if (Objects.isNull(realmAccess) || !realmAccess.containsKey(Key.ROLES.value())) {
            JwtTokenUtils.LOGGER.debug("Token does not have realm_access claim or roles is missing in it");
            return Set.of();
        }

        // 2. Extract roles from the realm_access and convert to a Collection<String>.
        final Object rolesObject = realmAccess.get(Key.ROLES.value());
        if (rolesObject instanceof final Collection<?> roles) {
            final String realm = jwt.getClaimAsString(JwtClaim.REALM.value());
            final boolean rvcRealm = PlatformDefaults.REALM_RVC.value()
                    .equals(realm);
            // If the role name is "super_admin" and does not belong to "revinci" tenant, we will skip that role.
            // "super_admin" role is supported only for "revinci" tenant. No other tenant can have a role with the
            // name "super_admin".
            final String superAdminRole = PlatformDefaults.ROLE_SUPER_ADMIN.value();
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(role -> superAdminRole.equals(role) && !rvcRealm ?
                            null :
                            new SimpleGrantedAuthority(PatternTemplate.AUTHORITY.format(role)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        // 3. Return an empty collection.
        return Set.of();
    }

    public static boolean isPlatformSuperAdministrator(final Jwt jwt) {
        final String realm = jwt.getClaimAsString(JwtClaim.REALM.value());
        final String iss = jwt.getClaimAsString(JwtClaim.ISS.value());
        final String superAdminAuthority = PatternTemplate.AUTHORITY.format(PlatformDefaults.ROLE_SUPER_ADMIN.value());
        final boolean hasSuperAdminRole = JwtTokenUtils.extractAuthorities(jwt)
                .stream()
                .anyMatch(ga -> ga.getAuthority()
                        .equals(superAdminAuthority));
        return PlatformDefaults.REALM_RVC.value()
                .equals(realm) && iss.endsWith(realm) && hasSuperAdminRole;
    }

    public static boolean isAuthorizedParty(final Jwt jwt) {
        // Authorized party
        final String azp = jwt.getClaimAsString(JwtClaim.AZP.value());
        final String realm = jwt.getClaimAsString(JwtClaim.REALM.value());
        final String iss = jwt.getClaimAsString(JwtClaim.ISS.value());

        return PlatformDefaults.AZP_ADMIN_CLI.value()
                .equals(azp) && PlatformDefaults.REALM_MASTER.value()
                .equals(realm) && iss.endsWith(realm);
    }

}
