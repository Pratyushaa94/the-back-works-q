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

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.data.jpa.utils.PageUtils;
import ai.revinci.platform.security.data.mapper.PermissionMapper;
import ai.revinci.platform.security.data.model.experience.UserPermission;
import ai.revinci.platform.security.data.model.persistence.PermissionEntity;
import ai.revinci.platform.security.data.repository.PermissionRepository;
import ai.revinci.platform.security.error.SecurityErrors;
import ai.revinci.platform.security.util.AuthenticationUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    /** A repository implementation of type {@link PermissionRepository}. */
    private final PermissionRepository permissionRepository;

    /** A mapper implementation of type {@link PermissionMapper}. */
    private final PermissionMapper permissionMapper;


    @Instrumentation
    @Transactional(readOnly = true)
    public Page<UserPermission> getUserPermissions(final int page, final int size) {
        // 1. Get the user's realm and the logged-in user's principal (which is email).
        final String realm = AuthenticationUtils.getRealmOrThrow();
        final String email = AuthenticationUtils.getPrincipalOrThrow();
        final Pageable pageSettings = PageUtils.createPaginationConfiguration(page, size);

        // 2. Find all the permissions applicable for this user.
        PermissionService.LOGGER.info("Realm: {}. Finding user permissions for user {}", realm,
                                      Strings.maskEmail(email));
        final Page<PermissionEntity> userPermissions = permissionRepository.findUserPermissions(realm, email,
                                                                                                pageSettings);
        PermissionService.LOGGER.debug("Realm: {}. No of user permissions for user {} = {}", realm,
                                       Strings.maskEmail(email), userPermissions.getSize());

        // 3. Transform and return.
        if (userPermissions.hasContent()) {
            final List<UserPermission> permissions = permissionMapper.transform(userPermissions.getContent());
            return PageUtils.createPage(permissions, pageSettings, userPermissions.getTotalElements());
        }

        return PageUtils.emptyPage(pageSettings);
    }

    public Collection<String> getUserPermissionCodes(final String realm, final String email) {
        // 1. Get the user's realm and the logged-in user's principal (which is email).
        final String currentUserRealm = AuthenticationUtils.getRealmOrThrow();
        final String currentUsername = AuthenticationUtils.getPrincipalOrThrow();

        // 2. The provided information must match the details of the logged-in user.
        if (!currentUserRealm.equals(realm) || !currentUsername.equals(email)) {
            // Throw an exception.
            throw ServiceException.of(SecurityErrors.FAILED_TO_RETRIEVE_USER_PERMISSIONS);
        }

        // 2. Find all the permissions applicable for this user.
        PermissionService.LOGGER.info("Realm: {}. Finding user permission codes for user {}", realm,
                                      Strings.maskEmail(email));
        final List<String> userPermissions = permissionRepository.findUserPermissionCodes(realm, email);
        PermissionService.LOGGER.debug("Realm: {}. No of user permission codes for user {} = {}", realm,
                                       Strings.maskEmail(email), userPermissions.size());

        return userPermissions;
    }
}
