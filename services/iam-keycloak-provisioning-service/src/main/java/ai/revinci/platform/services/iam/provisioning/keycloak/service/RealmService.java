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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.password.PasswordPolicy;
import ai.revinci.platform.common.tenant.policy.TenantPasswordPolicy;
import ai.revinci.platform.common.tenant.policy.UserRegistrationPolicy;
import ai.revinci.platform.common.util.FileUtils;
import ai.revinci.platform.common.util.JsonUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealmService {
    /** Instance of type {@link Keycloak}. */
    private final Keycloak keycloak;

    /**
     * This method attempts to find the realm identified by {@code realmName} in the Keycloak server.
     *
     * @param realmName The name of the realm to find.
     *
     * @return An {@link Optional} containing the {@link RealmRepresentation} if the realm is found,
     *         {@link Optional#empty()} otherwise.
     */
    @Instrumentation
    public Optional<RealmRepresentation> findRealm(@NonNull final String realmName) {
        if (StringUtils.isBlank(realmName)) {
            RealmService.LOGGER.warn("Realm name is blank");
            return Optional.empty();
        }

        try {
            final RealmRepresentation rr = getRealmResource(realmName).toRepresentation();
            RealmService.LOGGER.debug("Realm: {}. Found realm with id {}", realmName, rr.getId());

            return Optional.of(rr);
        } catch (final NotFoundException nfe) {
            RealmService.LOGGER.warn("Realm: {}. Unable to find the realm", realmName);
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
    @Instrumentation
    public boolean exists(@NonNull final String realmName) {
        if (StringUtils.isBlank(realmName)) {
            RealmService.LOGGER.warn("Provided realm name is blank");
            return false;
        }

        return findRealm(realmName).isPresent();
    }

    /**
     * This method attempts to create a new realm identified via {@code realmName} in the Keycloak server using the
     * provided {@code applicationUrl} for configuring the redirect-urls, web-origins, etc.
     *
     * @param realm                  The name of the realm to be created.
     * @param applicationUrl         Application URL, which will be used to configure the redirect urls, etc.
     * @param passwordPolicy         Password policy for the realm.
     * @param userRegistrationPolicy User registration policy for the realm.
     */
    @Instrumentation
    public void createRealm(final String realm, final String applicationUrl, final TenantPasswordPolicy passwordPolicy,
                            final UserRegistrationPolicy userRegistrationPolicy) {
        // Before we proceed with the creation, does a realm exist with the given name?
        if (exists(realm)) {
            RealmService.LOGGER.error("Realm: {}. Realm already exists. Skipping the creation", realm);
            return;
        }

        final String template = userRegistrationPolicy.isRegisterEmailAsUsername() ?
                "templates/create-realm-template.json" :
                "templates/create-realm-template-custom-user-registration-policy.json";
        RealmService.LOGGER.info("Realm: {}. Loading the template ({}) to create a realm", realm, template);
        final String content = FileUtils.readFile(template);

        RealmService.LOGGER.debug("Realm: {}. Application Url: {}. Replacing placeholders in template", realm,
                                  applicationUrl);
        final String realmPayload = content.replace("__TENANT__", realm)
                .replace("__BASE_URL__", applicationUrl)
                .replace("__PASSWORD_LENGTH__", String.valueOf(passwordPolicy.getLength()))
                .replace("__PASSWORD_MAX_LENGTH__",
                         String.valueOf(passwordPolicy.getMaxLength()))
                .replace("__PASSWORD_EXPIRY_DURATION__",
                         String.valueOf(passwordPolicy.getExpirationDays()))
                .replace("__PASSWORD_HISTORY__",
                         String.valueOf(PasswordPolicy.getPasswordHistory()))
                .replace("__PASSWORD_POLICY_SPECIAL_CHARS__",
                         String.valueOf(passwordPolicy.getNumberOfSpecialCharacters()))
                .replace("__PASSWORD_POLICY_UPPERCASE_CHARS__",
                         String.valueOf(passwordPolicy.getNumberOfUpperCaseCharacters()))
                .replace("__PASSWORD_POLICY_LOWERCASE_CHARS__",
                         String.valueOf(passwordPolicy.getNumberOfLowerCaseCharacters()))
                .replace("__PASSWORD_POLICY_DIGITS__",
                         String.valueOf(passwordPolicy.getNumberOfDigits()));

        // Transform this string into RealmsResource.
        RealmService.LOGGER.debug("Realm: {}. Deserializing the realm payload to RealmRepresentation", realm);
        final RealmRepresentation realmRepresentation = JsonUtils.deserialize(realmPayload, RealmRepresentation.class);

        RealmService.LOGGER.info("Realm: {}. Submitting request to Keycloak to create a new realm", realm);
        keycloak.realms()
                .create(realmRepresentation);
        RealmService.LOGGER.info("Realm: {}. Successfully created the new realm", realm);
    }

    /**
     * This method retrieves the accessible URLs for the realm identified by {@code realm}.
     *
     * @param realm The name of the realm.
     *
     * @return A {@link Map} containing the accessible URLs.
     */
    @Instrumentation
    public Map<String, String> getRealmUrls(final String realm) {
        final Map<String, String> urls = new HashMap<>();
        keycloak.realm(realm)
                .clients()
                .findAll()
                .forEach(client -> {
                    final String clientId = client.getClientId();
                    if (clientId.equalsIgnoreCase(PlatformDefaults.KEYCLOAK_CLIENT_ACCOUNT_CONSOLE.value())) {
                        urls.put(Key.IAM_ACCOUNT_CONSOLE_URL.value(), client.getRootUrl()
                                .concat(client.getBaseUrl()));
                    } else if (clientId.equalsIgnoreCase(realm)) {
                        urls.put(Key.IAM_APPLICATION_URL.value(), client.getBaseUrl());
                    } else if (clientId.equalsIgnoreCase(PlatformDefaults.KEYCLOAK_CLIENT_ADMIN_CONSOLE.value())) {
                        urls.put(Key.IAM_ADMIN_CONSOLE_URL.value(), client.getRootUrl()
                                .concat(client.getBaseUrl()));
                    }
                });
        return urls;
    }

    /**
     * This method returns the {@link RealmResource} for the realm identified by {@code realmName}.
     *
     * @param realmName The name of the realm.
     *
     * @return An instance of type {@link RealmResource}.
     */
    private RealmResource getRealmResource(final String realmName) {
        return keycloak.realm(realmName);
    }
}
