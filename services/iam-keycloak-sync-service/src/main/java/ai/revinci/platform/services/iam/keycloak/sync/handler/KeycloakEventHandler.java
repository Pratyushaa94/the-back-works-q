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

package ai.revinci.platform.services.iam.keycloak.sync.handler;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.messaging.utils.MessageUtils;
import ai.revinci.platform.multitenancy.datasource.RoutingDataSource;
import ai.revinci.platform.multitenancy.service.TenantDataSourceRefreshListener;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmRoleEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmRoleMappingEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.RealmUserEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.UserLoginEvent;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.experience.UserLogoutEvent;
import ai.revinci.platform.services.iam.keycloak.sync.enums.OperationType;
import ai.revinci.platform.services.iam.keycloak.sync.enums.ResourceType;
import ai.revinci.platform.services.iam.keycloak.sync.service.KeycloakService;
import ai.revinci.platform.services.iam.keycloak.sync.service.RoleService;
import ai.revinci.platform.services.iam.keycloak.sync.service.UserService;

@Slf4j
@RequiredArgsConstructor
@Component
public class KeycloakEventHandler {
    /** A routing data source instance of type {@link RoutingDataSource}. */
    private final RoutingDataSource routingDataSource;

    /** A service implementation of type {@link UserService}. */
    private final UserService userService;

    /** A service implementation of type {@link RoleService}. */
    private final RoleService roleService;

    /** A service implementation of type {@link KeycloakService}. */
    private final KeycloakService keycloakService;

    /** Instance of type {@link TenantDataSourceRefreshListener}. */
    private final TenantDataSourceRefreshListener tenantDataSourceRefreshListener;

    /**
     * This method handles the incoming event message from Keycloak server.
     * <p>
     * The events that we can handle are:
     *
     * <ul>
     *     <li>REALM_ROLE : Create, Update and Deletion of realm roles.</li>
     *     <li>REALM_ROLE_MAPPING : Assigning / Unassigning of roles to/from users.</li>
     *     <li>USER : Create, Update and Deletion of realm users.</li>
     * </ul>
     *
     * @param message Incoming message.
     */
    @Instrumentation
    @Async
    public void handleKeycloakEvent(final Message<Object> message) {
        try {
            Object messagePayload = message.getPayload();
            if (messagePayload instanceof byte[] mp) {
                final String payload = new String(mp);
                // Deserialize to a map first and process the event.
                processEvent(payload, JsonUtils.deserialize(payload, Map.class));
            } else {
                KeycloakEventHandler.LOGGER.warn("Unable to understand the provided payload");
            }
        } catch (final Exception ex) {
            KeycloakEventHandler.LOGGER.error("Failures encountered while processing keycloak event. Error: {}",
                                              ex.getMessage(), ex);
        }
    }

    /**
     * This method handles the incoming database provisioned message.
     *
     * @param message A {@link Message} object containing the payload.
     */
    @Instrumentation
    @Async
    public void handleDbProvisionedEvent(final Message<String> message) {
        final MessageHeaders headers = message.getHeaders();
        final UUID correlationId = MessageUtils.getCorrelationId(headers);
        final UUID tenantId = MessageUtils.getTenantId(headers);
        final String realm = MessageUtils.getRealm(headers);

        KeycloakEventHandler.LOGGER.info("Tenant: {}, Realm: {}, Correlation id: {}. Received new db provisioned event",
                                         tenantId, realm, correlationId);

        // TODO: Need to check if the processing of the message with the correlation-id is required or it has
        //  already been processed.

        try {
            tenantDataSourceRefreshListener.tenantProvisioned(tenantId, realm);
        } catch (final Exception ex) {
            KeycloakEventHandler.LOGGER.error(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Failures while processing db provisioned event",
                    tenantId, realm, correlationId, ex);
            // TODO: Need to send to DLQ.
        }
    }

    /**
     * This method handles the incoming realm provisioned message.
     *
     * @param message A {@link Message} object containing the payload.
     */
    @Instrumentation
    @Async
    public void handleRealmProvisionedEvent(final Message<String> message) {
        final MessageHeaders headers = message.getHeaders();
        final UUID correlationId = MessageUtils.getCorrelationId(headers);
        final UUID tenantId = MessageUtils.getTenantId(headers);
        final String realm = MessageUtils.getRealm(headers);

        KeycloakEventHandler.LOGGER.info(
                "Tenant: {}, Realm: {}, Correlation id: {}. Received realm provisioned event", tenantId, realm,
                correlationId);

        // TODO: Need to check if the processing of the message with the correlation-id is required or it has
        //  already been processed.

        // Does the routing datasource now have the new tenant? If not, let us not even move to the next steps as we
        // know that they will lead to exceptions.
        if (!routingDataSource.hasTenantDataSource(realm)) {
            KeycloakEventHandler.LOGGER.warn(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Datasource not found for tenant and realm combination",
                    tenantId, realm, correlationId);
            return;
        }

        // We can now sync the roles, users and role-mappings from the keycloak to the tenant-specific database.
        try {
            if (StringUtils.isNotBlank(realm)) {
                TenantContext.set(TenantRealm.builder()
                                          .realm(realm)
                                          .tenantId(tenantId)
                                          .build());
            }

            // There is no message payload for this case as we need only the tenant-id and realm.

            KeycloakEventHandler.LOGGER.info(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Syncing tenant roles and users", tenantId, realm,
                    correlationId);
            keycloakService.syncNewRealmRolesAndUsers(tenantId, realm);
        } catch (final Exception ex) {
            KeycloakEventHandler.LOGGER.error(
                    "Tenant: {}. Realm: {}, Correlation id: {}. Failures while syncing tenant roles and users",
                    tenantId, realm, correlationId, ex);
        } finally {
            // Clear the context.
            TenantContext.clear();
        }
    }

    /**
     * This method processes the keycloak event data available in the provided {@code eventData}.
     *
     * @param originalPayload Original payload.
     * @param eventData       Map containing the event data.
     */
    private void processEvent(final String originalPayload, final Map<?, ?> eventData) {
        try {
            // Extract the operation and resource type from the map.
            final OperationType operation = getOperation(eventData);
            final ResourceType resourceType = getResourceType(eventData);

            if (Objects.isNull(operation) || Objects.isNull(resourceType)) {
                KeycloakEventHandler.LOGGER.info("Operation ({}) / Resource type ({}) is missing. Ignoring the event",
                                                 operation, resourceType);
                return;
            }

            // Get the realm and set it in the context. This determines the datasource to be picked up.
            final String realm = getRealm(eventData);

            // Does the routing datasource now have the new tenant? If not, let us not even move to the next steps as we
            // know that they will lead to exceptions.
            if (!routingDataSource.hasTenantDataSource(realm)) {
                KeycloakEventHandler.LOGGER.warn("Realm: {}. Datasource not found for tenant and realm combination",
                                                 realm);
                return;
            }

            if (StringUtils.isNotBlank(realm)) {
                TenantContext.set(TenantRealm.builder()
                                          .realm(realm)
                                          .build());
            }

            if (ResourceType.REALM_ROLE.equals(resourceType)) {
                // Event pertaining to realm-role
                handleRealmRoleEvent(JsonUtils.deserialize(originalPayload, RealmRoleEvent.class));
            } else if (ResourceType.REALM_ROLE_MAPPING.equals(resourceType)) {
                // Event pertaining to realm-role-mapping
                handleRealmRoleMappingEvent(JsonUtils.deserialize(originalPayload, RealmRoleMappingEvent.class));
            } else if (ResourceType.USER.equals(resourceType)) {
                if (operation.equals(OperationType.LOGIN)) {
                    // Event pertaining to user login
                    handleUserLoginEvent(JsonUtils.deserialize(originalPayload, UserLoginEvent.class));
                } else if (operation.equals(OperationType.LOGOUT)) {
                    // Event pertaining to user logout
                    handleUserLogoutEvent(JsonUtils.deserialize(originalPayload, UserLogoutEvent.class));
                } else {
                    // Event pertaining to realm-user
                    handleRealmUserEvent(JsonUtils.deserialize(originalPayload, RealmUserEvent.class));
                }
            }
        } finally {
            // Clear the context.
            TenantContext.clear();
        }
    }

    /**
     * This method handles the event pertaining to realm roles.
     *
     * @param payload Realm role event.
     */
    private void handleRealmRoleEvent(final RealmRoleEvent payload) {
        final String realm = payload.getRealm();
        final OperationType operation = payload.getOperation();
        KeycloakEventHandler.LOGGER.info("Realm: {}. Processing {} operation for realm role data: {}", realm, operation,
                                         payload);
        if (OperationType.CREATE.equals(operation) || OperationType.UPDATE.equals(operation)) {
            roleService.saveRole(payload);
        } else if (OperationType.DELETE.equals(operation)) {
            roleService.deleteRole(payload);
        }
    }

    /**
     * This method handles the event pertaining to realm-role mapping.
     *
     * @param payload Realm role mapping event.
     */
    private void handleRealmRoleMappingEvent(final RealmRoleMappingEvent payload) {
        final String realm = payload.getRealm();
        final OperationType operation = payload.getOperation();
        KeycloakEventHandler.LOGGER.info("Realm: {}. Processing {} operation for realm role mapping data: {}", realm,
                                         operation, payload);
        if (OperationType.CREATE.equals(operation)) {
            userService.assignRolesToUser(payload);
        } else if (OperationType.DELETE.equals(operation)) {
            userService.unassignRolesFromUser(payload);
        }
    }

    /**
     * This method handles the event pertaining to realm users.
     *
     * @param payload Realm user event.
     */
    private void handleRealmUserEvent(final RealmUserEvent payload) {
        final String realm = payload.getRealm();
        final OperationType operation = payload.getOperation();
        KeycloakEventHandler.LOGGER.info("Realm: {}. Processing {} operation for user data: {}", realm, operation,
                                         payload);
        if (OperationType.CREATE.equals(operation) || OperationType.UPDATE.equals(operation)) {
            // When creating a new user, we seem to miss the username and user-id. Let us reach out to keycloak and
            // fetch the user details.
            updateMissingUserInformation(realm, payload);
            userService.saveUser(payload);
        } else if (OperationType.DELETE.equals(operation)) {
            userService.deleteUser(payload);
        }
    }

    /**
     * This method handles the event pertaining to user login.
     *
     * @param payload User login event.
     */
    private void handleUserLoginEvent(final UserLoginEvent payload) {
        final String realm = payload.getRealm();
        final OperationType operation = payload.getOperation();
        KeycloakEventHandler.LOGGER.info("Realm: {}. Processing {} operation for user login data: {}", realm, operation,
                                         payload);
        userService.updateUserLoginTime(payload);
    }

    /**
     * This method handles the event pertaining to user logout.
     *
     * @param payload User logout event.
     */
    private void handleUserLogoutEvent(final UserLogoutEvent payload) {
        final String realm = payload.getRealm();
        final OperationType operation = payload.getOperation();
        KeycloakEventHandler.LOGGER.info("Realm: {}. Processing {} operation for user logout data: {}", realm,
                                         operation, payload);
        // TODO: Update the cache to reflect the user logout.
    }

    /**
     * From the provided {@code data}, this method extracts the realm.
     *
     * @param data The data from which the realm is to be extracted.
     *
     * @return Realm if found, otherwise {@code null}.
     */
    private String getRealm(final Map<?, ?> data) {
        final Object realm = data.get(Key.REALM.value());
        return Objects.nonNull(realm) ?
                realm.toString() :
                null;
    }

    /**
     * From the provided {@code data}, this method extracts the operation type.
     *
     * @param data The data from which the operation type is to be extracted.
     *
     * @return Operation type if found, otherwise {@code null}.
     */
    private OperationType getOperation(final Map<?, ?> data) {
        final Object operation = data.get(Key.OPERATION.value());
        return Objects.nonNull(operation) ?
                OperationType.valueOf(operation.toString()) :
                null;
    }

    /**
     * From the provided {@code data}, this method extracts the resource type.
     *
     * @param data The data from which the resource type is to be extracted.
     *
     * @return Resource type if found, otherwise {@code null}.
     */
    private ResourceType getResourceType(final Map<?, ?> data) {
        final Object resourceType = data.get(Key.RESOURCE_TYPE.value());
        return Objects.nonNull(resourceType) ?
                ResourceType.valueOf(resourceType.toString()) :
                null;
    }

    /**
     * This method updates the missing user-information in the payload.
     * <p>
     * Whenever a new user is created in Keycloak, it fires an event and apparently, the {@code username} and {@code id}
     * comes out as empty. This method tries to fetch the user from Keycloak and update the missing information.
     *
     * @param realm   Realm.
     * @param payload Realm user event.
     */
    private void updateMissingUserInformation(final String realm, final RealmUserEvent payload) {
        final RealmUserEvent.Data data = payload.getData();
        if (StringUtils.isAnyBlank(data.getUsername(), data.getId())) {
            KeycloakEventHandler.LOGGER.warn("Realm: {}. No username or user-id in the event", realm);
            final Optional<UserRepresentation> user = keycloakService.findKeycloakUser(realm, data.getEmail(),
                                                                                       data.getFirstName(),
                                                                                       data.getLastName());
            if (user.isPresent()) {
                final UserRepresentation matchingUser = user.get();
                if (StringUtils.isBlank(data.getUsername()) && StringUtils.isNotBlank(matchingUser.getUsername())) {
                    data.setUsername(matchingUser.getUsername());
                    KeycloakEventHandler.LOGGER.debug("Realm: {}. Updated username from keycloak", realm);
                }
                if (StringUtils.isBlank(data.getId()) && StringUtils.isNotBlank(matchingUser.getId())) {
                    data.setId(matchingUser.getId());
                    KeycloakEventHandler.LOGGER.debug("Realm: {}. Updated user-id from keycloak", realm);
                }
            }
        }
    }
}
