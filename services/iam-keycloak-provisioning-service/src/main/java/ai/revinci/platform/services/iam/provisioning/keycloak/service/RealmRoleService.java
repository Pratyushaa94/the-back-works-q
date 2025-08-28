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

package ai.revinci.platform.services.iam.provisioning.keycloak.service;

import jakarta.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.model.experience.AddRoleRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealmRoleService {
    /** Instance of type {@link Keycloak}. */
    private final Keycloak keycloak;

    /**
     * This method attempts to find the role identified by {@code roleName} in the Keycloak server under the realm
     * identified by {@code realmName}.
     *
     * @param realmName Realm name where the role needs to be found.
     * @param roleName  The name of the role to find.
     *
     * @return An {@link Optional} containing the {@link RoleRepresentation} if the role is found,
     *         {@link Optional#empty()} otherwise.
     */
    @Instrumentation
    public Optional<RoleRepresentation> findRealmRole(final String realmName, final String roleName) {
        try {
            return Optional.of(keycloak.realm(realmName)
                                       .roles()
                                       .get(roleName)
                                       .toRepresentation());
        } catch (final NotFoundException nfe) {
            RealmRoleService.LOGGER.info("Realm: {}. Role {} not found", realmName, roleName);
            return Optional.empty();
        }
    }

    /**
     * This method attempts to check if the role identified by {@code roleName} exists in the realm identified by
     * {@code realmName}.
     *
     * @param realmName Realm name where the role needs to be found.
     * @param roleName  The name of the role to check for existence.
     *
     * @return {@code true} if the realm exists, {@code false} otherwise.
     */
    @Instrumentation
    public boolean realmRoleExists(final String realmName, final String roleName) {
        return findRealmRole(realmName, roleName).isPresent();
    }

    /**
     * This method attempts to create new roles ({@code newRoles} parameter) in the realm identified by
     * {@code realmName}.
     *
     * @param realmName Name of the realm where the role needs to be created.
     * @param newRoles  Collection of roles that have to be created in the realm identified by {@code realmName}.
     */
    @Instrumentation
    public void createRoles(final String realmName, final Collection<AddRoleRequest> newRoles) {
        Optional.ofNullable(newRoles)
                .ifPresent(roles -> roles.forEach(role -> createRole(realmName, role)));
    }

    /**
     * This method attempts to create a new role whose name is {@code roleName} in the realm identified by
     * {@code realmName}.
     *
     * @param realmName Name of the realm where the role needs to be created.
     * @param newRole   Instance of type {@link AddRoleRequest} that contains the details of the role to be created.
     */
    @Instrumentation
    public void createRole(final String realmName, final AddRoleRequest newRole) {
        final String roleName = newRole.getName();
        RealmRoleService.LOGGER.info("Realm: {}. Creating role {} in realm {}", realmName, roleName, realmName);

        // 1. Does the role exist?
        if (realmRoleExists(realmName, roleName)) {
            RealmRoleService.LOGGER.warn("Realm: {}. Role {} already exists in the realm {}. Skipping creation.",
                                         realmName, roleName, realmName);
            return;
        }

        // 2. Create the new role.
        final RoleRepresentation createRoleRequest = new RoleRepresentation();
        createRoleRequest.setName(roleName);
        createRoleRequest.setDescription(newRole.getDescription());
        keycloak.realm(realmName)
                .roles()
                .create(createRoleRequest);
        RealmRoleService.LOGGER.info("Realm: {}. Successfully created role {} in realm {}", realmName,
                                     newRole.getName(), realmName);
    }
}

