
package ai.revinci.platform.utilities.keycloak.administration.service;

import jakarta.ws.rs.NotFoundException;
import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.util.FileUtils;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.utilities.keycloak.administration.model.AddBulkUsersRequest;
import ai.revinci.platform.utilities.keycloak.administration.model.AddRoleRequest;
import ai.revinci.platform.utilities.keycloak.administration.model.OnboardRealmRequest;
import ai.revinci.platform.utilities.keycloak.administration.model.ResetUserPasswordRequest;


/**
 * A {@link Service} implementation that provides the functionality of creating realms in Keycloak.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealmService {
    /** Instance of type {@link Keycloak}. */
    private final Keycloak keycloak;

    /** Instance of type {@link UserService}. */
    private final UserService userService;

    /**
     * This method returns the {@link RealmResource} for the realm identified by {@code realmName}.
     *
     * @param realmName The name of the realm.
     *
     * @return An instance of type {@link RealmResource}.
     */
    public RealmResource getRealmResource(final String realmName) {
        return keycloak.realm(realmName);
    }

    /**
     * This method attempts to find the realm identified by {@code realmName} in the Keycloak server.
     *
     * @param realmName The name of the realm to find.
     *
     * @return An {@link Optional} containing the {@link RealmRepresentation} if the realm is found,
     *         {@link Optional#empty()} otherwise.
     */
    public Optional<RealmRepresentation> findRealm(final String realmName) {
        try {
            return Optional.of(getRealmResource(realmName).toRepresentation());
        } catch (final NotFoundException nfe) {
            RealmService.LOGGER.info("Realm {} not found", realmName);
            return Optional.empty();
        }
    }

    /**
     * This method attempts to check if the realm identified by {@code realmName} exists in the Keycloak server.
     *
     * @param realmName The name of the realm to check for existence.
     *
     * @return {@code true} if the realm exists, {@code false} otherwise.
     */
    public boolean exists(final String realmName) {
        return findRealm(realmName).isPresent();
    }

    /**
     * This method attempts to onboard a new realm identified by {@code realmName} in the Keycloak server.
     * <p>
     * All the resources mentioned in the {@code onboardRealmRequest} will be created in the realm.
     *
     * @param payload The request object that contains the details of the realm to be onboarded.
     */
    public void onboardRealm(@NonNull final String serverUrl, @NonNull final OnboardRealmRequest payload) {
        final String realmName = payload.getRealmName();

        // 1. Onboard the realm
        createRealm(payload.getApplicationUrl(), realmName);

        // 2. Get the realm resource.
        final RealmResource realmResource = getRealmResource(realmName);

        // 3. Add the new roles.
        if (payload.hasRoles()) {
            for (final AddRoleRequest role : payload.getRoles()) {
                createRole(realmName, realmResource, role);
            }
        }

        // 4. Add the new users.
        if (payload.hasUsers()) {
            userService.onboardUsers(realmResource, payload.getUsers());
        }
    }

    /**
     * This method attempts to create a new realm identified via {@code realmName} in the Keycloak server using the
     * provided {@code applicationUrl} for configuring the redirect-urls, web-origins, etc.
     * <p>
     * NOTE: This method creates a realm with the default configuration. If you need to create a realm with custom
     * configuration (e.g., creation of new roles, users, clients, etc.), use
     * {@link #onboardRealm(String, OnboardRealmRequest)}.
     *
     * @param applicationUrl Application URL, which will be used to configure the redirect urls, etc.
     * @param realmName      The name of the realm to be created.
     */
    public void createRealm(final String applicationUrl, final String realmName) {
        RealmService.LOGGER.info("Creating realm {}", realmName);

        // Before we proceed with the creation, does a realm exist with the given name?
        if (exists(realmName)) {
            RealmService.LOGGER.error("Realm {} already exists. Skipping the creation", realmName);
            return;
        }

        RealmService.LOGGER.info("Loading the template to create a realm");
        final String content = FileUtils.readFile("templates/create-realm-template.json");

        RealmService.LOGGER.info("Replacing placeholders. Realm: {}, Base URL: {}", realmName, applicationUrl);
        final String realmPayload = content.replace("__TENANT__", realmName)
                                           .replace("__BASE_URL__", applicationUrl);

        // Transform this string into RealmsResource.
        RealmService.LOGGER.info("Deserializing the realm payload to RealmRepresentation");
        final RealmRepresentation realmRepresentation = JsonUtils.deserialize(realmPayload, RealmRepresentation.class);

        RealmService.LOGGER.info("Creating a new realm for {}", realmName);
        keycloak.realms()
                .create(realmRepresentation);
        RealmService.LOGGER.info("Realm {} created successfully", realmName);
    }

    /**
     * This method attempts to find the role identified by {@code roleName} in the Keycloak server under the realm
     * identified by {@code realmResource}.
     *
     * @param realmResource Resource representation of the target realm.
     * @param roleName      The name of the role to find.
     *
     * @return An {@link Optional} containing the {@link RoleRepresentation} if the role is found,
     *         {@link Optional#empty()} otherwise.
     */
    public Optional<RoleRepresentation> findRealmRole(final RealmResource realmResource, final String roleName) {
        try {
            return Optional.of(realmResource.roles()
                                            .get(roleName)
                                            .toRepresentation());
        } catch (final NotFoundException nfe) {
            RealmService.LOGGER.info("Role {} not found", roleName);
            return Optional.empty();
        }
    }

    /**
     * This method attempts to check if the role identified by {@code roleName} exists in the realm identified by
     * {@code realmResource}.
     *
     * @param realmResource Resource representation of the target realm.
     * @param roleName      The name of the role to check for existence.
     *
     * @return {@code true} if the realm exists, {@code false} otherwise.
     */
    public boolean realmRoleExists(final RealmResource realmResource, final String roleName) {
        return findRealmRole(realmResource, roleName).isPresent();
    }

    /**
     * This method attempts to create a new role whose name is {@code roleName} in the realm identified by
     * {@code realmName}.
     *
     * @param realmName     Name of the realm where the role needs to be created.
     * @param realmResource Instance of type {@link RealmResource} that identifies the {@code realmName} and the
     *                      {@code newRole} will be created in this realm.
     * @param newRole       Instance of type {@link AddRoleRequest} that contains the details of the role to be
     *                      created.
     */
    public void createRole(final String realmName, final RealmResource realmResource, final AddRoleRequest newRole) {
        final String roleName = newRole.getName();
        RealmService.LOGGER.info("Creating role {} in realm {}", roleName, realmName);

        // 1. Does the role exist?
        if (realmRoleExists(realmResource, roleName)) {
            RealmService.LOGGER.warn("Role {} already exists in the realm {}. Skipping creation.", roleName, realmName);
            return;
        }

        // 2. Create the new role.
        final RoleRepresentation createRoleRequest = new RoleRepresentation();
        createRoleRequest.setName(roleName);
        createRoleRequest.setDescription(newRole.getDescription());
        realmResource.roles()
                     .create(createRoleRequest);
        RealmService.LOGGER.info("Successfully created role {} in realm {}", newRole.getName(), realmName);
    }

    /**
     * This method attempts to onboard a collection of users contained in the {@code payload} under the realm identified
     * by {@code realmName} property within the {@code payload}.
     *
     * @param payload The request object that contains the details of the users to be onboarded.
     */
    public void onboardUsers(@NonNull final AddBulkUsersRequest payload) {
        final String realmName = payload.getRealmName();
        userService.onboardUsers(getRealmResource(realmName), payload);
    }

    /**
     * This method attempts to reset the password of the users defined in the {@code payload}
     *
     * @param payload The request object that contains the details of the users to be onboarded.
     */
    public void resetPasswordForUsers(@NonNull final ResetUserPasswordRequest payload) {
        final String realmName = payload.getRealmName();
        userService.resetPassword(getRealmResource(realmName), payload);
    }
}
