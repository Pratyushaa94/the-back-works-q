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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.JwtClaim;
import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.security.data.model.experience.UserPresenceResponse;
import ai.revinci.platform.security.data.model.persistence.UserEntity;
import ai.revinci.platform.security.data.repository.UserRepository;
import ai.revinci.platform.security.error.SecurityErrors;
import ai.revinci.platform.security.util.JwtTokenUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    /** A repository implementation of type {@link UserRepository}. */
    private final UserRepository userRepository;


    private final PlatformJwtGrantedAuthoritiesConverter platformJwtGrantedAuthoritiesConverter;


    @Override
    public AbstractAuthenticationToken convert(@NonNull final Jwt jwt) {
        // 1. First check is - is the tenant context containing the realm, which is the same as the realm in the token?
        final boolean isPlatformSuperAdmin = JwtTokenUtils.isPlatformSuperAdministrator(jwt);
        if (isPlatformSuperAdmin) {
            PlatformJwtAuthenticationConverter.LOGGER.debug(
                    "Bypassing realm match check as the user is a platform super-admin");
        } else {
            assertRealmMatch(jwt);
        }

        // Before we do the conversion, assert that the user wrapped by the jwt is an existing active user in
        // the system.
        final boolean isAuthorizedParty = JwtTokenUtils.isAuthorizedParty(jwt);
        if (isAuthorizedParty) {
            // This can happen whenever we are onboarding "revinci" tenant using "admin-cli" of Keycloak.
            // Assign the super_admin privilege if the request is originating from platform-initializer
            final String superAdminAuthority = PatternTemplate.AUTHORITY.format(PlatformDefaults.ROLE_SUPER_ADMIN.value());
            PlatformJwtAuthenticationConverter.LOGGER.info("Request is originating from an authorized party");
            return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(superAdminAuthority)),
                                              JwtTokenUtils.extractPrincipal(jwt));
        }

        Jwt enhancedJwt = jwt;
        // Assert that the user is a valid user and has access to the application being accessed.
        final UserPresenceResponse response = assertValidUser(jwt);
        if (response.isTenantUserTableExists() && response.hasUser()) {
            // Reaching here means that the above conditions are met. Enhance the JWT with additional claims.
            enhancedJwt = enhanceJwtWithAdditionalClaims(jwt, response.getUser());
        }

        // If we reach here, then the user is valid and active. Proceed with the conversion after enhancing the
        // JWT with additional user details.
        final Collection<GrantedAuthority> authorities = platformJwtGrantedAuthoritiesConverter.convert(enhancedJwt);
        return new JwtAuthenticationToken(enhancedJwt, authorities, JwtTokenUtils.extractPrincipal(enhancedJwt));
    }

    protected void assertRealmMatch(final Jwt jwt) {
        final String realmInToken = jwt.getClaimAsString(JwtClaim.REALM.value());
        final String realmInHeader = TenantContext.realm();
        if (StringUtils.isNoneBlank(realmInToken, realmInHeader) && !realmInToken.equals(realmInHeader)) {
            PlatformJwtAuthenticationConverter.LOGGER.error("Realm mismatch. Expected: {}, Received: {}",
                                                               realmInToken, realmInHeader);
            throw ServiceException.of(SecurityErrors.TOKEN_INVALID);
        }
    }

    protected UserPresenceResponse assertValidUser(final Jwt jwt) {
        final String usernameClaim = JwtClaim.PREFERRED_USERNAME.value();
        final String username = jwt.getClaimAsString(usernameClaim);
        if (StringUtils.isBlank(username)) {
            PlatformJwtAuthenticationConverter.LOGGER.error("Unable to extract username from JWT token");
            throw ServiceException.of(SecurityErrors.CLAIM_NOT_FOUND, usernameClaim);
        }

        // Do we have the user in the system?
        final UserPresenceResponse response = checkForUserPresence(username);
        if (!response.isTenantUserTableExists()) {
            // Maybe the request is handled by a non-tenant-aware service and in such a case, the "tenant_user" table
            // does not exist.
            PlatformJwtAuthenticationConverter.LOGGER.warn("tenant_user table does not exist");
            return response;
        }

        // Reaching here means the "tenant_user" table exists but the user is not found. So throw an exception.
        if (!response.hasUser()) {
            PlatformJwtAuthenticationConverter.LOGGER.error(
                    "Unable to find the user {}. Either it does not exist or is either locked / deleted",
                    Strings.maskEmail(username));
            throw ServiceException.of(SecurityErrors.BAD_CREDENTIALS);
        }

        // Is the user deleted or locked?
        final UserEntity user = response.getUser();
        if (user.getLocked() || user.getDeleted()) {
            PlatformJwtAuthenticationConverter.LOGGER.error("User {} is either locked or deleted",
                                                               Strings.maskEmail(username));
            throw ServiceException.of(SecurityErrors.BAD_CREDENTIALS);
        }

        return response;
    }

    private Jwt enhanceJwtWithAdditionalClaims(final Jwt jwt, final UserEntity user) {
        final Map<String, Object> claims = new HashMap<>(jwt.getClaims());
        // Tenant identifier.
        claims.put(JwtClaim.TENANT_ID.value(), user.getTenantId()
                .toString());
        // User identifier.
        claims.put(JwtClaim.USER_ID.value(), user.getId()
                .toString());

        // Return the extended JWT
        return new Jwt(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getHeaders(), claims);
    }

    private UserPresenceResponse checkForUserPresence(final String username) {
        try {
            final Optional<UserEntity> matchingUser = userRepository.findByEmailOrUsername(username);
            return UserPresenceResponse.builder()
                    .tenantUserTableExists(true)
                    .user(matchingUser.orElse(null))
                    .build();
        } catch (final InvalidDataAccessResourceUsageException ex) {
            if (ex.getMessage()
                    .contains("tenant_user\" does not exist")) {
                // We need to ignore this message and skip the user-validation check.
                PlatformJwtAuthenticationConverter.LOGGER.warn(
                        "Bypassing user validation check as the request is served by a non-tenant-aware service");
                return UserPresenceResponse.builder()
                        .tenantUserTableExists(false)
                        .user(null)
                        .build();
            }
        }
        // Default user table existence to true.
        return UserPresenceResponse.builder()
                .tenantUserTableExists(true)
                .user(null)
                .build();
    }
}
