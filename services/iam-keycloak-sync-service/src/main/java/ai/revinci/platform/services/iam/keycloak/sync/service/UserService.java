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

package ai.revinci.platform.services.iam.keycloak.sync.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmRoleMappingEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmUserEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.UserLoginEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.RoleEntity;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.TenantEntity;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.TenantUserEntity;
import ai.revinci.platform.services.iam.keycloak.sync.data.repository.RoleRepository;
import ai.revinci.platform.services.iam.keycloak.sync.data.repository.TenantUserRepository;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    /** A repository implementation of type {@link TenantUserRepository}. */
    private final TenantUserRepository tenantUserRepository;

    /** A repository implementation of type {@link RoleRepository}. */
    private final RoleRepository roleRepository;

    /** A service implementation of type {@link TenantService}. */
    private final TenantService tenantService;

    /**
     * Updates the last login time for the user based on the user login event.
     *
     * @param event A {@link UserLoginEvent} object containing the user login event data.
     */
    @Instrumentation
    @Transactional
    public void updateUserLoginTime(final UserLoginEvent event) {
        final String realm = event.getRealm();

        // Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        final UserLoginEvent.Data loginData = event.getData();
        final String email = loginData.getEmail();
        final String username = loginData.getUsername();
        final String userId = loginData.getUserId();

        // Is there a user with this tenant?
        final Optional<TenantUserEntity> matchingUser = findMatchingUser(tenantId, userId, email, username);
        if (matchingUser.isPresent()) {
            final TenantUserEntity user = matchingUser.get();
            UserService.LOGGER.info("Tenant: {}. Realm: {}. Updating last login time for user: {}", tenantId, realm,
                                    user.getId());
            user.setLastLogin(loginData.getLoginTime());
            if (Objects.isNull(user.getIamUserId()) && StringUtils.isNotBlank(userId)) {
                user.setIamUserId(UUID.fromString(userId));
            }

            tenantUserRepository.saveAndFlush(user);
        } else {
            UserService.LOGGER.warn(
                    "Tenant: {}. Realm: {}. Unable to find user with email: {} / username: {} / iam user id: {}",
                    tenantId, realm, email, username, userId);
        }
    }

    /**
     * Saves the user details based on the user event.
     *
     * @param event A {@link RealmUserEvent} object containing the user event data.
     */
    @Instrumentation
    @Transactional
    public void saveUser(final RealmUserEvent event) {
        final String realm = event.getRealm();

        // Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        // If we reach here, the tenant exists.
        final RealmUserEvent.Data userData = event.getData();
        final String userId = userData.getId();
        final String username = userData.getUsername();
        final String email = userData.getEmail();
        final String firstname = userData.getFirstName();
        final String lastname = userData.getLastName();
        final boolean enabled = userData.isEnabled();

        // Is there a user with this tenant?
        final TenantUserEntity user;
        final Optional<TenantUserEntity> matchingUser = findMatchingUser(tenantId, userId, email, username);
        if (matchingUser.isPresent()) {
            user = matchingUser.get();
            // We will update only the firstname, lastname and enabled fields. We will update username and email only if
            // they are not already set.
            user.setFirstname(firstname);
            user.setLastname(lastname);
            user.setActive(enabled);
            user.setDeleted(false);
            if (Objects.isNull(user.getIamUserId()) && StringUtils.isNotBlank(userId)) {
                user.setIamUserId(UUID.fromString(userId));
            }
            if (StringUtils.isNotBlank(username) && StringUtils.isBlank(user.getUsername())) {
                user.setUsername(username);
            }
            if (StringUtils.isNotBlank(email) && StringUtils.isBlank(user.getEmail())) {
                user.setEmail(email);
            }

        } else {
            user = TenantUserEntity.builder()
                    .tenant(tenant)
                    .iamUserId(StringUtils.isBlank(userId) ?
                                       null :
                            UUID.fromString(userId))
                    .username(username)
                    .email(email)
                    .firstname(firstname)
                    .lastname(lastname)
                    .active(enabled)
                    .locked(false)
                    .deleted(false)
                    .build();
        }

        UserService.LOGGER.info("Tenant: {}. Realm: {}. Saving user details for iam user id: {}", tenantId, realm,
                                userId);

        tenantUserRepository.saveAndFlush(user);
    }

    /**
     * Deletes (soft-delete) the user available in the user event.
     *
     * @param event A {@link RealmUserEvent} object containing the user event data.
     */
    @Instrumentation
    @Transactional
    public void deleteUser(final RealmUserEvent event) {
        final String realm = event.getRealm();

        // Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        // If we reach here, the tenant exists.
        final RealmUserEvent.Data userData = event.getData();
        final String userId = userData.getId();
        final String email = userData.getUsername();
        final String username = userData.getEmail();

        // Is there a user with this tenant?
        final TenantUserEntity user;
        final Optional<TenantUserEntity> matchingUser = findMatchingUser(tenantId, userId, email, username);
        if (matchingUser.isPresent()) {
            user = matchingUser.get();
            // We will update only the firstname, lastname and enabled fields.
            user.setDeleted(true);
            user.setLocked(true);
            user.setActive(false);
            if (Objects.isNull(user.getIamUserId()) && StringUtils.isNotBlank(userId)) {
                user.setIamUserId(UUID.fromString(userId));
            }

            UserService.LOGGER.info("Tenant: {}. Realm: {}. Soft-deleting user with id: {}", tenantId, realm, userId);

            tenantUserRepository.saveAndFlush(user);
        } else {
            UserService.LOGGER.warn("Tenant: {}. Realm: {}. Unable to find user with id: {}", tenantId, realm, userId);
        }
    }

    /**
     * Assign the roles to the users available in the provided event instance of type {@link RealmRoleMappingEvent}.
     *
     * @param event A {@link RealmUserEvent} object containing the user role mappings data.
     */
    @Instrumentation
    @Transactional
    public void assignRolesToUser(final RealmRoleMappingEvent event) {
        updateRoleMappingsForUser(event, true);
    }

    /**
     * Unassign the provided {@code roleId} from the users.
     *
     * @param realm     Realm to which the users belong.
     * @param iamRoleId The role that needs to be unassigned from the users.
     */
    @Instrumentation
    @Transactional
    public void unassignRolesFromUser(final String realm, final UUID iamRoleId) {
        // Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        // If we reach here, the tenant exists. Find all users who have the role assigned to them within this tenant.
        final List<TenantUserEntity> users = tenantUserRepository.findUsersAssignedToIamRoleId(tenantId, iamRoleId);
        if (!users.isEmpty()) {
            UserService.LOGGER.info("Tenant: {}. Realm: {}. Role {} is assigned to {} users", tenantId, realm,
                                    iamRoleId, users.size());
            final List<TenantUserEntity> updatedUsers = new ArrayList<>();
            for (final TenantUserEntity user : users) {
                if (user.removeRoleIfPresent(iamRoleId)) {
                    UserService.LOGGER.info("Tenant: {}. Realm: {}. Removed role {} from the user {}", tenantId, realm,
                                            iamRoleId, user.getId());
                    updatedUsers.add(user);
                }
            }
            if (!updatedUsers.isEmpty()) {
                UserService.LOGGER.info("Tenant: {}. Realm: {}. Removed role {} from {} users", tenantId, realm,
                                        iamRoleId, updatedUsers.size());

                tenantUserRepository.saveAllAndFlush(updatedUsers);
            }
        } else {
            UserService.LOGGER.info("Tenant: {}. Realm: {}. Iam role id {} is not assigned to any users", tenantId,
                                    realm, iamRoleId);
        }
    }

    /**
     * Unassign the roles from the users available in the provided event instance of type
     * {@link RealmRoleMappingEvent}.
     *
     * @param event A {@link RealmUserEvent} object containing the user role mappings data.
     */
    @Instrumentation
    @Transactional
    public void unassignRolesFromUser(final RealmRoleMappingEvent event) {
        updateRoleMappingsForUser(event, false);
    }

    /**
     * Finds a user based on the provided user details.
     *
     * @param tenantId  Unique identifier of the tenant.
     * @param iamUserId Unique identifier of the user in the IAM system (Keycloak).
     * @param email     Email address of the user.
     * @param username  Username of the user.
     *
     * @return An {@link Optional} wrapping the matching {@link TenantUserEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    private Optional<TenantUserEntity> findMatchingUser(final UUID tenantId, final String iamUserId, final String email,
                                                        final String username) {
        Optional<TenantUserEntity> matchingUser = Optional.empty();

        if (StringUtils.isNotBlank(iamUserId)) {
            matchingUser = tenantUserRepository.findByIamUserId(tenantId, UUID.fromString(iamUserId));
        }

        if (matchingUser.isEmpty() && StringUtils.isNotBlank(email)) {
            matchingUser = tenantUserRepository.findByEmail(tenantId, email);
        }

        if (matchingUser.isEmpty() && StringUtils.isNotBlank(username)) {
            matchingUser = tenantUserRepository.findByUsername(tenantId, username);
        }

        return matchingUser;
    }

    /**
     * This method attempts to assign or unassign the roles against the user in the provided {@code event}.
     *
     * @param event             Event containing the user role mappings details.
     * @param assignRolesToUser If true, the payload is for assigning the roles to the user. If false, the payload is
     *                          for unassigning the roles from the user.
     */
    private void updateRoleMappingsForUser(final RealmRoleMappingEvent event, final boolean assignRolesToUser) {
        final String realm = event.getRealm();

        // Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        // If we reach here, the tenant exists.
        final String userId = event.getUserId();
        final String email = event.getEmail();
        final String username = event.getUsername();
        final Optional<TenantUserEntity> matchingUser = findMatchingUser(tenantId, userId, email, username);
        if (matchingUser.isPresent()) {
            final TenantUserEntity user = matchingUser.get();
            boolean verdict = false;
            for (final RealmRoleMappingEvent.Data roleMapping : event.getData()) {
                final UUID iamRoleId = UUID.fromString(roleMapping.getRoleId());
                final Optional<RoleEntity> matchingRole = roleRepository.findByIamRoleId(tenantId, iamRoleId);
                if (matchingRole.isEmpty()) {
                    UserService.LOGGER.warn("Tenant: {}. Realm: {}. Unable to find role with iam role id: {}", tenantId,
                                            realm, iamRoleId);
                    continue;
                }

                final RoleEntity role = matchingRole.get();
                if (assignRolesToUser) {
                    if (user.addRoleIfAbsent(iamRoleId, role)) {
                        UserService.LOGGER.info("Tenant: {}. Realm: {}. Added role {} to user {}", tenantId, realm,
                                                iamRoleId, userId);
                        verdict = true;
                    }
                } else {
                    if (user.removeRoleIfPresent(iamRoleId)) {
                        UserService.LOGGER.info("Tenant: {}. Realm: {}. Removed role {} from user {}", tenantId, realm,
                                                iamRoleId, userId);
                        verdict = true;
                    }
                }
            }

            if (verdict) {
                // If the user-id is missing in our database, update it.
                if (Objects.isNull(user.getIamUserId()) && StringUtils.isNotBlank(userId)) {
                    user.setIamUserId(UUID.fromString(userId));
                }

                UserService.LOGGER.info("Tenant: {}. Realm: {}. Updating role mappings for user {}", tenantId, realm,
                                        userId);
                tenantUserRepository.saveAndFlush(user);
            }

        } else {
            UserService.LOGGER.warn("Tenant: {}. Realm: {}. Unable to find user with id: {}", tenantId, realm, userId);
        }
    }
}
