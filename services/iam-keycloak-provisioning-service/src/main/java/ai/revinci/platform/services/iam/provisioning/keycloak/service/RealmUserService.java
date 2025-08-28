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
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.mapper.KeycloakMapper;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.model.experience.AddBulkUsersRequest;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.model.experience.AddRealmUserRequest;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.model.experience.ResetUserPasswordRequest;
import ai.revinci.platform.services.iam.provisioning.keycloak.error.KeycloakProvisioningServiceErrors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealmUserService {
    /** Instance of type {@link Keycloak}. */
    private final Keycloak keycloak;

    /** A mapper implementation of type {@link KeycloakMapper}. */
    private final KeycloakMapper keycloakMapper;

    /**
     * This method attempts to find the user identified by {@code userId} under the realm identified by
     * {@code realmName}.
     *
     * @param realmName Name of the realm under which the user needs to be found.
     * @param userId    Identifier of the user to be found.
     *
     * @return An {@link Optional} instance of type {@link UserResource}.
     */
    @Instrumentation
    public Optional<UserResource> findUser(final String realmName, final String userId) {
        try {
            return Optional.of(keycloak.realm(realmName)
                                       .users()
                                       .get(userId));
        } catch (final NotFoundException nfe) {
            RealmUserService.LOGGER.warn("Realm: {}. User with id {} not found", realmName, userId);
            return Optional.empty();
        }
    }

    /**
     * This method attempts to find the user identified by {@code username} under the realm identified by
     * {@code realmName}.
     *
     * @param realmName Name of the realm under which the user needs to be found.
     * @param username  Username of the user to be found.
     *
     * @return An {@link Optional} instance of type {@link UserRepresentation}.
     */
    @Instrumentation
    public Optional<UserRepresentation> findUserByUsername(final String realmName, final String username) {
        try {
            final List<UserRepresentation> matchingUsers = keycloak.realm(realmName)
                    .users()
                    .searchByUsername(username, true);
            final int count = matchingUsers.size();
            if (count > 1) {
                RealmUserService.LOGGER.warn("Realm: {}. Found {} users with matching username {}. Exiting.", realmName,
                                             count, username);
                throw ServiceException.of(KeycloakProvisioningServiceErrors.MULTIPLE_USERS_WITH_MATCHING_USERNAME,
                                          count);
            }

            if (count == 0) {
                RealmUserService.LOGGER.warn("Realm: {}. No user found with username: {}", realmName, username);
                return Optional.empty();
            }

            return Optional.of(matchingUsers.getFirst());
        } catch (final NotFoundException nfe) {
            RealmUserService.LOGGER.warn("Realm: {}. Unable to find user with username {}", realmName, username);
            return Optional.empty();
        }
    }

    /**
     * This method attempts to onboard the provided list of users ({@code newUsers}) under the realm identified by
     * {@code realmName}.
     *
     * @param realmName Name of the realm under which the user needs to be found.
     * @param payload   Payload containing the collection of users to be onboarded.
     */
    @Instrumentation
    public void createUsers(final String realmName, final AddBulkUsersRequest payload) {
        // 1. Do we have a realm-resource?
        if (StringUtils.isBlank(realmName)) {
            RealmUserService.LOGGER.warn("Realm resource is null. Cannot proceed.");
            return;
        }

        // 2. Do we have new users to create?
        if (Objects.isNull(payload) || !payload.hasUsers()) {
            RealmUserService.LOGGER.warn("Realm: {}. No users provided. Returning.", realmName);
            return;
        }

        // Delegate to the method for onboarding the users.
        createUsers(realmName, payload.getUsers());
    }

    /**
     * This method attempts to onboard the new users identified by {@code newUsers} under the realm identified by
     * {@code realmResource}.
     *
     * @param realmName Name of the realm under which the user needs to be found.
     * @param newUsers  Collection of users to be onboarded.
     */
    @Instrumentation
    public void createUsers(final String realmName, Collection<AddRealmUserRequest> newUsers) {
        RealmUserService.LOGGER.info("Realm: {}. Number of users to create: {}", realmName, newUsers.size());
        newUsers.forEach(newUser -> createUser(realmName, newUser));
    }

    /**
     * This method attempts to onboard a new user identified by {@code newUser} under the realm identified by
     * {@code realmName}.
     *
     * @param realmName Name of the realm under which the user needs to be found.
     * @param newUser   User to be onboarded.
     */
    @Instrumentation
    public void createUser(final String realmName, final AddRealmUserRequest newUser) {
        RealmUserService.LOGGER.info("Realm: {}. Creating user with username: {}", realmName, newUser.getUsername());

        final RealmResource realmResource = keycloak.realm(realmName);
        // 1. Does the user exist?
        final String email = newUser.getEmail();
        final List<UserRepresentation> matchingUsers = realmResource.users()
                .searchByEmail(email, true);
        if (Objects.nonNull(matchingUsers) && !matchingUsers.isEmpty()) {
            final int count = matchingUsers.size();
            RealmUserService.LOGGER.warn("Realm: {}. Found {} users with matching email address. Skipping this user.",
                                         realmName, count);
            return;
        }

        // 2. Create the user.
        try (final Response response = realmResource.users()
                .create(keycloakMapper.transform(newUser))) {
            final String id = extractIdFromResponse(realmName, response);
            RealmUserService.LOGGER.info("Realm: {}. Id of the newly created user: {}", realmName, id);

            // 2.1 Reset the password
            resetPassword(realmName, id, newUser.getPassword(), newUser.isTemporaryPassword());

            // 2.2. Does the payload contain any roles to be assigned?
            if (newUser.hasRoles()) {
                assignRoleToUser(realmName, id, newUser.roleNamesToAssign());
            }
        }
    }

    /**
     * This method attempts to reset the password for the users identified by {@code payload} under the realm identified
     * by {@code realmName}.
     *
     * @param realmName Name of the realm under which the user needs to be found.
     * @param payload   Payload containing the collection of users whose password needs to be reset.
     */
    @Instrumentation
    public void resetPassword(final String realmName, final ResetUserPasswordRequest payload) {
        if (StringUtils.isBlank(realmName) || !payload.hasUsers()) {
            RealmUserService.LOGGER.warn("No realm-resource or no users to reset the password. Returning");
            return;
        }

        // Loop through and reset the password.
        resetPassword(realmName, payload.getUsers());
    }

    /**
     * This method attempts to reset the password for the users identified by {@code userCredentials} under the realm
     * identified by {@code realmName}.
     *
     * @param realmName       Name of the realm under which the user needs to be found.
     * @param userCredentials Collection of user credentials whose password needs to be reset.
     */
    @Instrumentation
    public void resetPassword(final String realmName,
                              final Collection<ResetUserPasswordRequest.UserCredential> userCredentials) {
        if (StringUtils.isBlank(realmName) || Objects.isNull(userCredentials) || userCredentials.isEmpty()) {
            RealmUserService.LOGGER.warn("No realm-resource or no user credentials provided. Returning");
            return;
        }

        // Loop through and reset the password.
        for (final ResetUserPasswordRequest.UserCredential credential : userCredentials) {
            final String email = credential.getEmail();
            // Find the user by email
            final Optional<UserRepresentation> matchingUser = findUserByUsername(realmName, email);
            if (matchingUser.isEmpty()) {
                RealmUserService.LOGGER.warn("Realm: {}. User with email {} not found. Cannot reset the password.",
                                             realmName, email);
                continue;
            }

            final String userId = matchingUser.get()
                    .getId();
            resetPassword(realmName, userId, credential.getPassword(), credential.isTemporaryPassword());
        }
    }

    /**
     * This method attempts to reset the password for the user identified by {@code userId} under the realm identified
     * by {@code realmName}.
     *
     * @param realmName           Name of the realm under which the user needs to be found.
     * @param userId              Identifier of the user whose password needs to be reset.
     * @param password            New password for the user.
     * @param isTemporaryPassword Flag to indicate if the password is temporary.
     */
    @Instrumentation
    public void resetPassword(final String realmName, final String userId, final String password,
                              final boolean isTemporaryPassword) {
        if (StringUtils.isAnyBlank(realmName, userId, password)) {
            RealmUserService.LOGGER.info("Invalid inputs. One of realm-name, user-id, password is blank. Returning");
            return;
        }

        RealmUserService.LOGGER.info("Realm: {}. Resetting the password for the newly created user: {}", realmName,
                                     userId);
        final Optional<UserResource> matchingUser = findUser(realmName, userId);
        if (matchingUser.isEmpty()) {
            RealmUserService.LOGGER.warn("Realm: {}. User with id {} not found. Cannot reset the password.", realmName,
                                         userId);
            return;
        }

        matchingUser.get()
                .resetPassword(transform(password, isTemporaryPassword));

        RealmUserService.LOGGER.info("Realm: {}. Successfully reset the password for user: {}", realmName, userId);
    }

    /**
     * This method transforms the provided {@code password} into a {@link CredentialRepresentation} object.
     *
     * @param password          User password.
     * @param temporaryPassword Flag to indicate if the password is temporary.
     *
     * @return An instance of type {@link CredentialRepresentation}.
     */
    private CredentialRepresentation transform(final String password, final boolean temporaryPassword) {
        final CredentialRepresentation credentials = new CredentialRepresentation();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setTemporary(temporaryPassword);
        credentials.setValue(password);

        return credentials;
    }

    /**
     * From the provided {@code response} object, this method extracts the identifier of the created resource.
     *
     * @param realmName Realm name (and used only for logging purpose).
     * @param response  Response object from which the unique identifier of the created resource needs to be extracted.
     *
     * @return Unique identifier of the created resource.
     */
    private String extractIdFromResponse(final String realmName, final Response response) {
        final URI location = response.getLocation();
        if (!response.getStatusInfo()
                .equals(Response.Status.CREATED) || Objects.isNull(location)) {
            RealmUserService.LOGGER.error("Realm: {}. User creation failed with status: {}", realmName,
                                          response.getStatus());
            throw ServiceException.of(KeycloakProvisioningServiceErrors.ADD_USER_FAILED);
        }

        // Extract the id from the location
        final String path = location.getPath();
        return path.substring(path.lastIndexOf(Token.FORWARD_SLASH.value()) + 1);
    }

    /**
     * This method attempts to assign the provided {@code roleNames} to the user identified by {@code userId} under the
     * realm identified by {@code realmResource}.
     *
     * @param realmName Realm name.
     * @param userId    Unique identifier of the user to whom the roles need to be assigned.
     * @param roleNames Collection of role names to be assigned.
     */
    private void assignRoleToUser(final String realmName, final String userId, final Collection<String> roleNames) {
        if (StringUtils.isAnyBlank(realmName, userId) || Objects.isNull(roleNames) || roleNames.isEmpty()) {
            RealmUserService.LOGGER.warn("Invalid inputs provided for assigning roles to users. Returning.");
            return;
        }

        RealmUserService.LOGGER.info("Realm: {}. Assigning roles {} to user with id {}", realmName, roleNames, userId);

        final RealmResource realmResource = keycloak.realm(realmName);
        // Get the role representations for the provided role names.
        final RolesResource rolesResource = realmResource.roles();
        final List<RoleRepresentation> rolesToAssign = roleNames.stream()
                .map(role -> rolesResource.get(role)
                        .toRepresentation())
                .toList();

        realmResource.users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(rolesToAssign);

        RealmUserService.LOGGER.info("Realm: {}. Successfully assigned roles {} to user with id {}", realmName,
                                     roleNames, userId);
    }
}
