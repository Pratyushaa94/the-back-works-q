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

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.tenant.policy.TenantPasswordPolicy;
import ai.revinci.platform.common.tenant.policy.UserRegistrationPolicy;
import ai.revinci.platform.provisioning.iam.data.model.experience.OnboardRealmRequest;
import ai.revinci.platform.provisioning.iam.service.lifecycle.IRealmLifecycleService;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceStatus;
import ai.revinci.platform.provisioning.status.handler.listener.ContextProvider;
import ai.revinci.platform.provisioning.status.handler.listener.IProgressListener;
import ai.revinci.platform.services.iam.provisioning.keycloak.configuration.properties.KeycloakProperties;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.mapper.KeycloakMapper;
import ai.revinci.platform.services.iam.provisioning.keycloak.error.KeycloakProvisioningServiceErrors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService implements IRealmLifecycleService {
    /** Configuration properties instance of type {@link KeycloakProperties}. */
    private final KeycloakProperties keycloakProperties;

    /** A mapper instance of type {@link KeycloakMapper}. */
    private final KeycloakMapper keycloakMapper;

    /** A service implementation of type {@link RealmService}. */
    private final RealmService realmService;

    /** A service implementation of type {@link RealmRoleService}. */
    private final RealmRoleService realmRoleService;

    /** A service implementation of type {@link RealmUserService}. */
    private final RealmUserService realmUserService;

    @Override
    public boolean realmExists(@NonNull final String realm) {
        return realmService.exists(realm);
    }

    @Override
    public void provisionRealm(@NonNull final OnboardRealmRequest payload, @NonNull final IProgressListener callback) {
        // Set the application url on the payload.
        payload.setApplicationUrl(keycloakProperties.getApplicationUrl());

        final OnboardRealmRequest.Tenant tenant = payload.getTenant();
        final UUID tenantId = tenant.getId();
        final UUID resourceId = tenant.getResourceId();
        final String realm = tenant.getRealmName();

        try {
            // 1. Onboard the realm
            createRealm(realm, payload.getApplicationUrl(), payload);

            // 2. Add the new roles.
            if (payload.hasRoles()) {
                createRealmRoles(realm, payload.getRoles());
            }

            // 3. Add the new users.
            if (payload.hasUsers()) {
                createRealmUsers(realm, payload.getUsers());
            }

            // Send the updates via the callback.
            callback.update(ContextProvider.instance()
                                    .tenantId(tenantId)
                                    .realm(realm)
                                    .resourceId(resourceId)
                                    .resourceStatus(TenantResourceStatus.PROVISIONING_COMPLETED));

            // Get the realm urls and send it via callback.
            final Map<String, String> realmUrls = realmService.getRealmUrls(realm);
            callback.update(ContextProvider.instance()
                                    .tenantId(tenantId)
                                    .realm(realm)
                                    .resourceId(resourceId)
                                    .resourceStatus(TenantResourceStatus.ACTIVE)
                                    .iamAccountConsoleUrl(realmUrls.get(Key.IAM_ACCOUNT_CONSOLE_URL.value()))
                                    .iamApplicationUrl(realmUrls.get(Key.IAM_APPLICATION_URL.value()))
                                    .iamAdminConsoleUrl(realmUrls.get(Key.IAM_ADMIN_CONSOLE_URL.value())));
        } catch (final Exception e) {
            // Send the updates via the callback before throwing the exception.
            callback.update(ContextProvider.instance()
                                    .tenantId(tenantId)
                                    .realm(realm)
                                    .resourceId(resourceId)
                                    .resourceStatus(TenantResourceStatus.PROVISIONING_FAILED)
                                    .errorMessage(e.getMessage()));
            throw e;
        }
    }

    /**
     * Provisions a new realm in the IAM provider i.e., Keycloak.
     *
     * @param realm          The name of the realm to be provisioned.
     * @param applicationUrl The application URL, that will be used as the redirect urls for users post login.
     * @param payload        Payload for onboarding a realm.
     */
    private void createRealm(final String realm, final String applicationUrl, final OnboardRealmRequest payload) {
        try {
            final TenantPasswordPolicy passwordPolicy = payload.getPasswordPolicy();
            final UserRegistrationPolicy userRegistrationPolicy = payload.getUserRegistrationPolicy();
            KeycloakService.LOGGER.info("Realm: {}. Creating a new with application url: {}", realm, applicationUrl);
            realmService.createRealm(realm, applicationUrl, passwordPolicy, userRegistrationPolicy);
        } catch (final Exception ex) {
            KeycloakService.LOGGER.error("Failed to provision realm: {}", realm, ex);
            throw ServiceException.of(KeycloakProvisioningServiceErrors.FAILED_TO_PROVISION_REALM, realm,
                                      ex.getMessage());
        }
    }

    /**
     * This method attempts to add the provided {@code roles} as realm roles in the {@code realm}.
     *
     * @param realm Realm name where the new roles have to be added.
     * @param roles A collection of new roles who have to be added to the realm.
     */
    private void createRealmRoles(final String realm, final Collection<OnboardRealmRequest.RealmRole> roles) {
        try {
            KeycloakService.LOGGER.info("Realm: {}. Adding {} new realm roles", realm, roles.size());
            realmRoleService.createRoles(realm, keycloakMapper.transformRoles(roles));
        } catch (final Exception ex) {
            KeycloakService.LOGGER.error("Failed to create roles in realm: {}", realm, ex);
            throw ServiceException.of(KeycloakProvisioningServiceErrors.FAILED_TO_CREATE_REALM_ROLES, realm,
                                      ex.getMessage());
        }
    }

    /**
     * This method attempts to add the provided {@code users} as realm users in the {@code realm}.
     *
     * @param realm Realm name where the new users have to be added.
     * @param users A collection of new users who have to be added to the realm.
     */
    private void createRealmUsers(final String realm, final Collection<OnboardRealmRequest.RealmUser> users) {
        try {
            KeycloakService.LOGGER.info("Realm: {}. Adding {} new realm users", realm, users.size());
            realmUserService.createUsers(realm, keycloakMapper.transformUsers(users));
        } catch (final Exception ex) {
            KeycloakService.LOGGER.error("Failed to create users in realm: {}", realm, ex);
            throw ServiceException.of(KeycloakProvisioningServiceErrors.FAILED_TO_CREATE_REALM_USERS, realm,
                                      ex.getMessage());
        }
    }
}
