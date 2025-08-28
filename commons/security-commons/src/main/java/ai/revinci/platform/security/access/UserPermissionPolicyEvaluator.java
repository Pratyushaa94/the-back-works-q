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

package ai.revinci.platform.security.access;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.security.service.PermissionService;
import ai.revinci.platform.security.util.AuthenticationUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPermissionPolicyEvaluator implements PermissionEvaluator {
    /** A service implementation of type {@link PermissionService}. */
    private final PermissionService permissionService;

    @Override
    public boolean hasPermission(final Authentication authentication, final Object targetDomainObject,
                                 final Object permission) {
        return hasPermission(authentication, permission);
    }

    @Override
    public boolean hasPermission(final Authentication authentication, final Serializable targetId,
                                 final String targetType, final Object permission) {
        return hasPermission(authentication, permission);
    }

    /**
     * This method checks if the provided {@code authentication} object has the permissions provided in the
     * {@code permission} parameter.
     *
     * @param authentication Authentication object (which generally represents the current logged-in user).
     * @param permission     One or more permissions. If multiple permissions, the data type will be {@code String[]}.
     *
     * @return True if the provided {@code authentication} has the {@code permission}.
     */
    private boolean hasPermission(final Authentication authentication, final Object permission) {
        if (authentication == null || permission == null) {
            UserPermissionPolicyEvaluator.LOGGER.warn("Invalid data. Either authentication or permission object is null");
            return false;
        }

        // Get the realm-name and username from the authentication.
        final String realm = AuthenticationUtils.getRealmOrThrow(authentication);
        final String username = AuthenticationUtils.getPrincipalOrThrow(authentication);

        // Retrieve the permissions for the user in the authentication object.
        final Collection<String> userPermissions = permissionService.getUserPermissionCodes(realm, username);

        // Do we have multiple permissions?
        if (permission instanceof String[] permissions) {
            boolean verdict = false;
            for (final String requiredPermission : permissions) {
                if (userPermissions.contains(requiredPermission)) {
                    verdict = true;
                    break;
                }
            }
            return verdict;
        }

        // Single permission
        return userPermissions.contains(permission.toString());
    }
}
