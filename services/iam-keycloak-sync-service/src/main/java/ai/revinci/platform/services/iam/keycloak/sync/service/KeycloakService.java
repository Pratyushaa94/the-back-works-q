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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.services.iam.keycloak.sync.configuration.properties.KeycloakProperties;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmRoleEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmRoleMappingEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmUserEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.repository.NotificationSettingsRepository;
import ai.revinci.platform.services.iam.keycloak.sync.enums.OperationType;
import ai.revinci.platform.services.iam.keycloak.sync.enums.ResourceType;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService implements ApplicationContextAware {
    /** A configuration properties of type {@link KeycloakProperties}. */
    private final KeycloakProperties keycloakProperties;

    /** A service implementation of type {@link RoleService}. */
    private final RoleService roleService;

    /** A service implementation of type {@link UserService}. */
    private final UserService userService;

    /** A repository implementation of type {@link NotificationSettingsRepository}. */
    private final NotificationSettingsRepository notificationSettingsRepository;

    /** Application context. */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * This method attempts to synchronize the new roles and users from the Keycloak server to the tenant-specific
     * database.
     * <p>
     * This method is called from the realm provisioned event handler i.e., whenever a new tenant-specific realm has
     * been successfully provisioned, this method is called to synchronize the new roles and users from the Keycloak
     * server to the tenant-specific database.
     *
     * @param tenantId Unique identifier of the tenant.
     * @param realm    Realm name from where the roles, users, role-mappings have to be synchronized to the
     *                 tenant-specific database.
     */
    @Instrumentation
    @Transactional
    public void syncNewRealmRolesAndUsers(@NonNull final UUID tenantId, @NonNull final String realm) {
        // Get the Keycloak bean from application context.
        final Keycloak keycloak = applicationContext.getBean(Keycloak.class);

        // 1. Get the realm resource.
        final RealmResource realmResource = keycloak.realm(realm);

        // 2. Sync the roles.
        syncRoles(realm, realmResource);

        // 3. Sync the users and user-role mappings.
        syncUsers(tenantId, realm, realmResource);
    }

    /**
     * This method attempts to find a keycloak user whose {@code email}, {@code firstname} and {@code lastname} matches
     * the provided parameters.
     *
     * @param realm     Realm name.
     * @param email     Email address of the user.
     * @param firstname First name of the user.
     * @param lastname  Last name of the user.
     *
     * @return An {@link Optional} of type {@link UserRepresentation}.
     */
    @Instrumentation
    public Optional<UserRepresentation> findKeycloakUser(@NonNull final String realm, @NonNull final String email,
                                                         final String firstname, final String lastname) {
        // Get the Keycloak bean from application context.
        final Keycloak keycloak = applicationContext.getBean(Keycloak.class);

        return keycloak.realm(realm)
                .users()
                .searchByEmail(email, true)
                .stream()
                .filter(u -> Strings.same(u.getEmail(), email) && Strings.same(u.getFirstName(),
                                                                               firstname) && Strings.same(
                        u.getLastName(), lastname))
                .findFirst();
    }

    /**
     * This method attempts to synchronize the roles from the Keycloak server to the tenant-specific database for the
     * provided {@code realm}.
     *
     * @param realm         Realm name from where the roles have to be synchronized to the tenant-specific database.
     * @param realmResource Resource representation of the {@code realm}.
     */
    private void syncRoles(final String realm, final RealmResource realmResource) {
        KeycloakService.LOGGER.info("Realm: {}. Retrieving realm roles", realm);
        // 1. Fetch the roles and filter the roles that need to be ignored.
        final List<RoleRepresentation> roles = realmResource.roles()
                .list()
                .stream()
                .filter(r -> !keycloakProperties.ignoreRole(r.getName()))
                .toList();

        KeycloakService.LOGGER.info("Realm: {}. No of roles to sync: {}", realm, roles.size());

        // 2. Loop through the roles and sync them.
        final Collection<String> roleNames = new ArrayList<>();
        for (final RoleRepresentation rr : roles) {
            final RealmRoleEvent rre = RealmRoleEvent.builder()
                    .operation(OperationType.CREATE)
                    .resourceType(ResourceType.REALM_ROLE)
                    .realm(realm)
                    .data(RealmRoleEvent.Data.builder()
                                  .id(rr.getId())
                                  .name(rr.getName())
                                  .description(rr.getDescription())
                                  .build())
                    .build();
            KeycloakService.LOGGER.info("Realm: {}. Syncing role: {}", realm, rr.getName());
            roleService.saveRole(rre);

            // Add the role name, so that we can sync the permissions.
            roleNames.add(rr.getName());
        }

        // 3. Sync permissions for the roles.
        roleService.assignAllPermissionsForRoles(realm, roleNames);
    }

    /**
     * This method attempts to synchronize the users from the Keycloak server to the tenant-specific database for the
     * provided {@code realm}.
     *
     * @param tenantId      Unique identifier of the tenant.
     * @param realm         Realm name from where the users have to be synchronized to the tenant-specific database.
     * @param realmResource Resource representation of the {@code realm}.
     */
    private void syncUsers(final UUID tenantId, final String realm, final RealmResource realmResource) {
        KeycloakService.LOGGER.info("Realm: {}. Retrieving realm users", realm);
        // 1. Fetch the roles and filter the roles that need to be ignored.
        final List<UserRepresentation> users = realmResource.users()
                .list()
                .stream()
                .toList();

        KeycloakService.LOGGER.info("Realm: {}. No of users to sync: {}", realm, users.size());

        // 2. Loop through the roles and sync them.
        for (final UserRepresentation ur : users) {
            final String maskedEmail = Strings.maskEmail(ur.getEmail());
            final RealmUserEvent rue = RealmUserEvent.builder()
                    .operation(OperationType.CREATE)
                    .resourceType(ResourceType.USER)
                    .realm(realm)
                    .data(RealmUserEvent.Data.builder()
                                  .id(ur.getId())
                                  .username(ur.getUsername())
                                  .firstName(ur.getFirstName())
                                  .lastName(ur.getLastName())
                                  .email(ur.getEmail())
                                  .enabled(ur.isEnabled())
                                  .build())
                    .build();
            KeycloakService.LOGGER.info("Realm: {}. Syncing user: {}", realm, maskedEmail);
            userService.saveUser(rue);

            // 3. Sync the user's role-mappings.
            final UserResource userResource = realmResource.users()
                    .get(ur.getId());
            final List<RoleRepresentation> assignedRoles = userResource.roles()
                    .realmLevel()
                    .listAll();
            if (CollectionUtils.isEmpty(assignedRoles)) {
                KeycloakService.LOGGER.info("Realm: {}. No roles mapped to user {}", realm, maskedEmail);
                continue;
            }

            KeycloakService.LOGGER.info("Realm: {}. Syncing {} role-mappings for user: {}", realm, assignedRoles.size(),
                                        maskedEmail);
            syncRoleMappings(realm, ur.getId(), ur.getEmail(), ur.getUsername(), assignedRoles);
        }
    }

    /**
     * This method attempts to synchronize the role-mappings for the provided combination of {@code userId},
     * {@code email}, {@code username} and {@code userRoles}.
     *
     * @param realm     Realm name from where the role-mappings have to be synchronized to the tenant-specific
     *                  database.
     * @param userId    Unique identifier of the user.
     * @param email     Email address of the user.
     * @param username  Username of the user.
     * @param userRoles List of roles mapped to the user.
     */
    private void syncRoleMappings(final String realm, final String userId, final String email, final String username,
                                  final List<RoleRepresentation> userRoles) {
        KeycloakService.LOGGER.info("Realm: {}. Retrieving role mappings", realm);
        // 1. Find the roles from the keycloak server, ignoring the roles we don't need & convert to experience models.
        final List<RealmRoleMappingEvent.Data> roles = userRoles.stream()
                .filter(rr -> !keycloakProperties.ignoreRole(
                        rr.getName()))
                .map(rr -> RealmRoleMappingEvent.Data.builder()
                        .roleId(rr.getId())
                        .name(rr.getName())
                        .build())
                .collect(Collectors.toList());

        // 2. Sync this role-mapping
        KeycloakService.LOGGER.info("Realm: {}. Syncing role-mapping for userId: {}", realm, userId);
        userService.assignRolesToUser(RealmRoleMappingEvent.builder()
                                              .operation(OperationType.CREATE)
                                              .resourceType(ResourceType.REALM_ROLE_MAPPING)
                                              .realm(realm)
                                              .userId(userId)
                                              .email(email)
                                              .username(username)
                                              .data(roles)
                                              .build());
    }
}
