
package ai.revinci.platform.utilities.keycloak.administration.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.util.ThreadUtils;
import ai.revinci.platform.utilities.keycloak.administration.error.KeycloakAdministrationErrors;
import ai.revinci.platform.utilities.keycloak.administration.model.AddBulkUsersRequest;
import ai.revinci.platform.utilities.keycloak.administration.model.AddRealmUserRequest;
import ai.revinci.platform.utilities.keycloak.administration.model.ResetUserPasswordRequest;


/**
 * A {@link Service} implementation that provides the functionality of managing users in Keycloak.
 *
 */
@Slf4j
@Service
public class UserService {
    /** Random instance. */
    private static final Random RANDOM = new Random();

    /**
     * This method attempts to find the user identified by {@code userId} under the realm identified by
     * {@code realmResource}.
     *
     * @param realmResource Realm resource under which the user needs to be found.
     * @param userId        Identifier of the user to be found.
     *
     * @return An {@link Optional} instance of type {@link UserResource}.
     */
    public Optional<UserResource> findUser(final RealmResource realmResource, final String userId) {
        try {
            return Optional.of(realmResource.users()
                                            .get(userId));
        } catch (final NotFoundException nfe) {
            UserService.LOGGER.warn("User with id {} not found", userId);
            return Optional.empty();
        }
    }

    /**
     * This method attempts to find the user identified by {@code username} under the realm identified by
     * {@code realmResource}.
     *
     * @param realmResource Realm resource under which the user needs to be found.
     * @param username      Username of the user to be found.
     *
     * @return An {@link Optional} instance of type {@link UserRepresentation}.
     */
    public Optional<UserRepresentation> findUserByUsername(final RealmResource realmResource, final String username) {
        try {
            final List<UserRepresentation> matchingUsers = realmResource.users()
                                                                        .searchByEmail(username, true);
            final int count = matchingUsers.size();
            if (count > 1) {
                UserService.LOGGER.warn("Found {} users with matching username {}. Exiting.", count, username);
                throw ServiceException.of(KeycloakAdministrationErrors.MULTIPLE_USERS_WITH_MATCHING_USERNAME, count);
            }

            if (count == 0) {
                UserService.LOGGER.warn("No user found with username: {}", username);
                return Optional.empty();
            }

            return Optional.of(matchingUsers.getFirst());
        } catch (final NotFoundException nfe) {
            UserService.LOGGER.warn("Unable to find user with username {}", username);
            return Optional.empty();
        }
    }

    /**
     * This method attempts to onboard the provided list of users ({@code newUsers}) under the realm identified by
     * {@code realmResource}.
     *
     * @param realmResource Realm resource under which the users need to be onboarded.
     * @param payload       Payload containing the collection of users to be onboarded.
     */
    public void onboardUsers(final RealmResource realmResource, final AddBulkUsersRequest payload) {
        // 1. Do we have a realm-resource?
        if (Objects.isNull(realmResource)) {
            UserService.LOGGER.warn("Realm resource is null. Cannot proceed.");
            return;
        }

        // 2. Do we have new users to create?
        if (Objects.isNull(payload) || !payload.hasUsers()) {
            UserService.LOGGER.warn("No users provided. Returning.");
            return;
        }

        // Delegate to the method for onboarding the users.
        onboardUsers(realmResource, payload.getUsers());
    }

    /**
     * This method attempts to onboard the new users identified by {@code newUsers} under the realm identified by
     * {@code realmResource}.
     *
     * @param realmResource Realm resource under which the user needs to be onboarded.
     * @param newUsers      Collection of users to be onboarded.
     */
    public void onboardUsers(final RealmResource realmResource, Collection<AddRealmUserRequest> newUsers) {
        UserService.LOGGER.info("Number of users to create: {} in the realm", newUsers.size());
        newUsers.forEach(newUser -> onboardUser(realmResource, newUser));
    }

    /**
     * This method attempts to onboard a new user identified by {@code newUser} under the realm identified by
     * {@code realmResource}.
     *
     * @param realmResource Realm resource under which the user needs to be onboarded.
     * @param newUser       User to be onboarded.
     */
    public void onboardUser(final RealmResource realmResource, final AddRealmUserRequest newUser) {
        UserService.LOGGER.info("Creating user with username: {}", newUser.getUsername());
        ThreadUtils.sleep(TimeUnit.MILLISECONDS, UserService.RANDOM.nextInt(100));
        // 1. Does the user exist?
        final String email = newUser.getEmail();
        final List<UserRepresentation> matchingUsers = realmResource.users()
                                                                    .searchByEmail(email, true);
        if (Objects.nonNull(matchingUsers) && !matchingUsers.isEmpty()) {
            final int count = matchingUsers.size();
            UserService.LOGGER.warn("Found {} users with matching email address. Skipping this user.", count);
            return;
        }

        // 2. Create the user.
        try (final Response response = realmResource.users()
                                                    .create(transform(newUser))) {
            final String id = extractIdFromResponse(response);
            UserService.LOGGER.info("Id of the newly created user: {}", id);

            // 2.1 Reset the password
            resetPassword(realmResource, id, newUser.getPassword(), newUser.isTemporaryPassword());

            // 2.2. Does the payload contain any roles to be assigned?
            if (newUser.hasRoles()) {
                assignRoleToUser(realmResource, id, newUser.roleNamesToAssign());
            }
        }
    }

    /**
     * This method attempts to reset the password for the users identified by {@code payload} under the realm identified
     * by {@code realmResource}.
     *
     * @param realmResource Realm resource under which the user's password needs to be reset.
     * @param payload       Payload containing the collection of users whose password needs to be reset.
     */
    public void resetPassword(final RealmResource realmResource, final ResetUserPasswordRequest payload) {
        if (Objects.isNull(realmResource) || !payload.hasUsers()) {
            UserService.LOGGER.warn("No realm-resource or no users to reset the password. Returning");
            return;
        }

        // Loop through and reset the password.
        resetPassword(realmResource, payload.getUsers());
    }

    /**
     * This method attempts to reset the password for the users identified by {@code userCredentials} under the realm
     * identified by {@code realmResource}.
     *
     * @param realmResource   Realm resource under which the user's password needs to be reset.
     * @param userCredentials Collection of user credentials whose password needs to be reset.
     */
    public void resetPassword(final RealmResource realmResource,
                              final Collection<ResetUserPasswordRequest.UserCredential> userCredentials) {
        if (Objects.isNull(realmResource) || Objects.isNull(userCredentials) || userCredentials.isEmpty()) {
            UserService.LOGGER.warn("No realm-resource or no user credentials provided. Returning");
            return;
        }

        // Loop through and reset the password.
        for (final ResetUserPasswordRequest.UserCredential credential : userCredentials) {
            final String email = credential.getEmail();
            // Find the user by email
            final Optional<UserRepresentation> matchingUser = findUserByUsername(realmResource, email);
            if (matchingUser.isEmpty()) {
                UserService.LOGGER.warn("User with email {} not found. Cannot reset the password.", email);
                continue;
            }

            final String userId = matchingUser.get()
                                              .getId();
            resetPassword(realmResource, userId, credential.getPassword(), credential.isTemporaryPassword());
        }
    }

    /**
     * This method attempts to reset the password for the user identified by {@code userId} under the realm identified
     * by {@code realmResource}.
     *
     * @param realmResource       Realm resource under which the user's password needs to be reset.
     * @param userId              Identifier of the user whose password needs to be reset.
     * @param password            New password for the user.
     * @param isTemporaryPassword Flag to indicate if the password is temporary.
     */
    public void resetPassword(final RealmResource realmResource, final String userId, final String password,
                              final boolean isTemporaryPassword) {
        UserService.LOGGER.info("Resetting the password for the newly created user: {}", userId);
        final Optional<UserResource> matchingUser = findUser(realmResource, userId);
        if (matchingUser.isEmpty()) {
            UserService.LOGGER.warn("User with id {} not found. Cannot reset the password.", userId);
            return;
        }

        matchingUser.get()
                    .resetPassword(transform(password, isTemporaryPassword));

        UserService.LOGGER.info("Successfully reset the password for user: {}", userId);
    }

    /**
     * This method transforms the provided {@code user} into a {@link UserRepresentation} object.
     *
     * @param user User to be transformed.
     *
     * @return An instance of type {@link UserRepresentation}.
     */
    private UserRepresentation transform(final AddRealmUserRequest user) {
        final UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setFirstName(user.getFirstname());
        newUser.setLastName(user.getLastname());
        newUser.setEnabled(true);
        newUser.setEmailVerified(true);


        return newUser;
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
     * @param response Response object from which the unique identifier of the created resource needs to be extracted.
     *
     * @return Unique identifier of the created resource.
     */
    private String extractIdFromResponse(final Response response) {
        final URI location = response.getLocation();
        if (!response.getStatusInfo()
                     .equals(Response.Status.CREATED) || Objects.isNull(location)) {
            UserService.LOGGER.error("User creation failed with status: {}", response.getStatus());
            throw ServiceException.of(KeycloakAdministrationErrors.ADD_USER_FAILED);
        }

        // Extract the id from the location
        final String path = location.getPath();
        return path.substring(path.lastIndexOf(Token.FORWARD_SLASH.value()) + 1);
    }

    /**
     * This method attempts to assign the provided {@code roleNames} to the user identified by {@code userId} under the
     * realm identified by {@code realmResource}.
     *
     * @param realmResource Realm resource under which the roles need to be assigned.
     * @param userId        Unique identifier of the user to whom the roles need to be assigned.
     * @param roleNames     Collection of role names to be assigned.
     */
    private void assignRoleToUser(final RealmResource realmResource, final String userId,
                                  final Collection<String> roleNames) {
        if (Objects.isNull(realmResource) || StringUtils.isBlank(userId) || Objects.isNull(
                roleNames) || roleNames.isEmpty()) {
            UserService.LOGGER.warn("Invalid inputs provided for assigning roles to users. Returning.");
            return;
        }

        UserService.LOGGER.info("Assigning roles {} to user with id {}", roleNames, userId);
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

        UserService.LOGGER.info("Successfully assigned roles {} to user with id {}", roleNames, userId);
    }
}
