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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.JwtClaim;
import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.util.Strings;

@Slf4j
public final class AuthenticationUtils {
    private AuthenticationUtils() {
        throw new IllegalStateException("Cannot create instances of this class");
    }

    public static Authentication getAuthentication() {
        final Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        return Objects.nonNull(authentication) && authentication.isAuthenticated() ?
                authentication :
                null;
    }

    public static Authentication getAuthenticationOrThrow() {
        final Authentication authentication = AuthenticationUtils.getAuthentication();

        // Assert the authentication is valid.
        AuthenticationUtils.assertValidAuthentication(authentication);

        return authentication;
    }

    public static String getPrincipalOrThrow() {
        return AuthenticationUtils.getPrincipalOrThrow(AuthenticationUtils.getAuthentication());
    }

    public static String getPrincipalOrThrow(final Authentication authentication) {
        // Assert that the authentication is valid.
        AuthenticationUtils.assertValidAuthentication(authentication);

        return authentication.getName();
    }

    public static String getRealmOrThrow() {
        return AuthenticationUtils.getRealmOrThrow(AuthenticationUtils.getAuthentication());
    }

    public static String getRealmOrThrow(final Authentication authentication) {
        // Assert that the provided authentication is valid.
        AuthenticationUtils.assertValidAuthentication(authentication);
        if (authentication instanceof JwtAuthenticationToken jat) {
            final Object realm = jat.getTokenAttributes()
                    .get(JwtClaim.REALM.value());
            if (Objects.nonNull(realm)) {
                return realm.toString();
            }
            // Throw an exception
            throw ServiceException.of(CommonErrors.MISSING_REALM_IN_AUTHENTICATION);
        }

        // The authentication object is not the expected one. Throw an exception.
        throw ServiceException.of(CommonErrors.MISSING_AUTHENTICATION);
    }

    public static UUID getTenantIdOrThrow() {
        return AuthenticationUtils.getTenantIdOrThrow(AuthenticationUtils.getAuthentication());
    }

    public static UUID getTenantIdOrThrow(final Authentication authentication) {
        // Assert that the provided authentication is valid.
        AuthenticationUtils.assertValidAuthentication(authentication);
        if (authentication instanceof JwtAuthenticationToken jat) {
            final Object tid = jat.getTokenAttributes()
                    .get(JwtClaim.TENANT_ID.value());
            if (tid instanceof String tenantId) {
                return UUID.fromString(tenantId);
            }
            // Throw an exception
            throw ServiceException.of(CommonErrors.MISSING_TENANT_ID_IN_AUTHENTICATION);
        }

        // The authentication object is not the expected one. Throw an exception.
        throw ServiceException.of(CommonErrors.MISSING_AUTHENTICATION);
    }

    public static UUID getAuthenticatedUserIdOrThrow() {
        final Authentication auth = Optional.ofNullable(AuthenticationUtils.getAuthentication())
                .filter(Authentication::isAuthenticated)
                .orElseThrow(
                        () -> ServiceException.of(CommonErrors.MISSING_AUTHENTICATION));

        if (!(auth instanceof JwtAuthenticationToken jat)) {
            AuthenticationUtils.LOGGER.error("Invalid authentication token type");
            throw ServiceException.of(CommonErrors.MISSING_AUTHENTICATION);
        }

        return Optional.ofNullable(jat.getTokenAttributes()
                                           .get(JwtClaim.USER_ID.value()))
                .map(Object::toString)
                .filter(Strings::isValidUUID)
                .map(UUID::fromString)
                .orElseThrow(() -> {
                    AuthenticationUtils.LOGGER.error("Failed to extract user-id from the authentication token");
                    return ServiceException.of(CommonErrors.MISSING_AUTHENTICATION);
                });
    }

    public static boolean isAdmin() {
        return AuthenticationUtils.isAdmin(AuthenticationUtils.getAuthentication());
    }

    public static boolean isSuperAdmin() {
        return AuthenticationUtils.isSuperAdmin(AuthenticationUtils.getAuthentication());
    }

    public static boolean isAdmin(final Authentication authentication) {
        final String adminRoleAuthority = PatternTemplate.AUTHORITY.format(PlatformDefaults.ROLE_ADMIN.value());

        try {
            AuthenticationUtils.assertValidAuthentication(authentication);
            return authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority -> authority.getAuthority()
                            .equals(adminRoleAuthority));
        } catch (final Exception ex) {
            AuthenticationUtils.LOGGER.warn("Failed to determine if the user is an admin", ex);
        }
        return false;
    }

    public static boolean isSuperAdmin(final Authentication authentication) {
        final String superAdminRoleAuthority = PatternTemplate.AUTHORITY.format(PlatformDefaults.ROLE_SUPER_ADMIN.value());

        try {
            AuthenticationUtils.assertValidAuthentication(authentication);
            final String realm = AuthenticationUtils.getRealmOrThrow(authentication);
            final boolean superAdmin = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority -> authority.getAuthority()
                            .equals(superAdminRoleAuthority));
            return superAdmin && realm.equals(PlatformDefaults.REALM_RVC.value());
        } catch (final Exception ex) {
            AuthenticationUtils.LOGGER.warn("Failed to determine if the user is a super-admin", ex);
        }
        return false;
    }
    private static void assertValidAuthentication(final Authentication authentication) {
        if (Objects.isNull(authentication) || authentication instanceof AnonymousAuthenticationToken) {
            throw ServiceException.of(CommonErrors.MISSING_AUTHENTICATION);
        }
    }

}
