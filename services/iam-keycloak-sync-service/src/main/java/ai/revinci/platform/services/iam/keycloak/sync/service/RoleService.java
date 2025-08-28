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

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.security.data.model.persistence.PermissionEntity;
import ai.revinci.platform.security.data.repository.PermissionRepository;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmRoleEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.RoleEntity;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.RolePermissionEntity;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.RolePermissionId;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.TenantEntity;
import ai.revinci.platform.services.iam.keycloak.sync.data.repository.RoleRepository;

@Slf4j
@RequiredArgsConstructor
@Service
public class RoleService {
    /** A repository implementation of type {@link RoleRepository}. */
    private final RoleRepository roleRepository;

    /** A repository implementation of type {@link PermissionRepository}. */
    private final PermissionRepository permissionRepository;

    /** A service implementation of type {@link TenantService}. */
    private final TenantService tenantService;

    /** A service implementation of type {@link UserService}. */
    private final UserService userService;

    /**
     * Saves the role details based on the role event.
     *
     * @param event A {@link RealmRoleEvent} object containing the role event data.
     */
    @Instrumentation
    @Transactional
    public void saveRole(final RealmRoleEvent event) {
        final String realm = event.getRealm();

        // Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        // If we reach here, the tenant exists.
        final RealmRoleEvent.Data roleData = event.getData();
        final String iamRoleId = roleData.getId();
        final String name = roleData.getName();
        final String description = roleData.getDescription();

        // Is there a role with this tenant?
        final RoleEntity role;
        final Optional<RoleEntity> matchingRole = findMatchingRole(tenantId, iamRoleId, name);
        if (matchingRole.isPresent()) {
            role = matchingRole.get();
            // We will update only the description of the role.
            role.setDescription(description);
            if (Objects.isNull(role.getIamRoleId()) && StringUtils.isNotBlank(iamRoleId)) {
                role.setIamRoleId(UUID.fromString(iamRoleId));
            }
        } else {
            role = RoleEntity.builder()
                    .tenant(tenant)
                    .iamRoleId(StringUtils.isBlank(iamRoleId) ?
                                       null :
                            UUID.fromString(iamRoleId))
                    .name(name)
                    .description(description)
                    .build();
        }

        RoleService.LOGGER.info("Tenant: {}. Realm: {}. Saving role details for role id: {}", tenantId, realm,
                                iamRoleId);

        roleRepository.saveAndFlush(role);
    }

    /**
     * Deletes the role available in the role event.
     *
     * @param event A {@link RealmRoleEvent} object containing the role event data.
     */
    @Instrumentation
    @Transactional
    public void deleteRole(final RealmRoleEvent event) {
        final String realm = event.getRealm();

        // Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        // If we reach here, the tenant exists.
        final RealmRoleEvent.Data roleData = event.getData();
        final String iamRoleId = roleData.getId();

        // Is there a role with this tenant?
        final RoleEntity role;
        final Optional<RoleEntity> matchingRole = findMatchingRole(tenantId, iamRoleId, null);
        if (matchingRole.isPresent()) {
            role = matchingRole.get();
            // Unassign the roles from the users.
            userService.unassignRolesFromUser(realm, UUID.fromString(iamRoleId));

            RoleService.LOGGER.info("Tenant: {}. Realm: {}. Deleting role with iam role id: {}", tenantId, realm,
                                    iamRoleId);

            roleRepository.delete(role);
        } else {
            RoleService.LOGGER.warn("Tenant: {}. Realm: {}. Unable to find role with id: {}", tenantId, realm,
                                    iamRoleId);
        }
    }

    /**
     * This method assigns all the permissions for the provided {@code roleNames}.
     * <p>
     * This needs to be used with caution as it assigns all the permissions to the roles.
     * <p>
     * This is generally called as soon as the tenant provisioning is completed.
     *
     * @param realm     Realm name.
     * @param roleNames Collection of role names.
     */
    @Instrumentation
    @Transactional
    void assignAllPermissionsForRoles(final String realm, final Collection<String> roleNames) {
        // 1. Are there any role names to assign?
        if (Objects.isNull(roleNames) || roleNames.isEmpty()) {
            RoleService.LOGGER.warn("Realm: {}. No roles to assign permissions", realm);
            return;
        }

        // 2. Does the realm exist?
        final TenantEntity tenant = tenantService.findByRealm(realm);
        final UUID tenantId = tenant.getId();

        // 3. Find the roles by names.
        final Collection<RoleEntity> matchingRoles = roleRepository.findByNames(tenantId, roleNames);
        if (matchingRoles.isEmpty()) {
            RoleService.LOGGER.warn(
                    "Tenant: {}. Realm: {}. Unable to find roles with names: {}. Cannot add permissions", tenantId,
                    realm, roleNames);
            return;
        }

        // 4. Find all the permissions in the system.
        final Collection<PermissionEntity> permissions = permissionRepository.findAll();
        if (permissions.isEmpty()) {
            RoleService.LOGGER.warn("Tenant: {}. Realm: {}. No permissions defined in the system", tenantId, realm);
            return;
        }

        // 5. Assign permissions to each of the roles.
        matchingRoles.forEach(role -> {
            role.getPermissions()
                    .addAll(permissions.stream()
                                    .map(pe -> RolePermissionEntity.builder()
                                            .id(RolePermissionId.builder()
                                                        .roleId(role.getId())
                                                        .permissionCode(pe.getCode())
                                                        .build())
                                            .role(role)
                                            .permission(pe)
                                            .build())
                                    .toList());
        });

        // 6. Save and flush the role-permission mappings.
        RoleService.LOGGER.info("Tenant: {}. Realm: {}. Saving {} permissions to roles: {}", tenantId, realm,
                                permissions.size(), roleNames);
        roleRepository.saveAll(matchingRoles);
    }

    /**
     * Finds a role based on the provided role details.
     *
     * @param tenantId  Unique identifier of the tenant.
     * @param iamRoleId Unique identifier of the role in the IAM system (keycloak).
     * @param name      Role name.
     *
     * @return An {@link Optional} wrapping the matching {@link RoleEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    private Optional<RoleEntity> findMatchingRole(final UUID tenantId, final String iamRoleId, final String name) {
        Optional<RoleEntity> matchingRole = Optional.empty();
        if (StringUtils.isNotBlank(iamRoleId)) {
            matchingRole = roleRepository.findByIamRoleId(tenantId, UUID.fromString(iamRoleId));
        }

        if (matchingRole.isEmpty() && StringUtils.isNotBlank(name)) {
            matchingRole = roleRepository.findByName(tenantId, name);
        }

        return matchingRole;
    }
}

