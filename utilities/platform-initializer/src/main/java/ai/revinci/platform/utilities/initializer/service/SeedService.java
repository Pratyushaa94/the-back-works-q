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

package ai.revinci.platform.utilities.initializer.service;

import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.util.FileUtils;
import ai.revinci.platform.utilities.initializer.configuration.properties.KeycloakProperties;
import io.micrometer.common.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService implements ApplicationContextAware {
    /** Properties instance of type {@link KeycloakProperties}. */
    private final KeycloakProperties keycloakProperties;

    /** Rest template instance of type {@link RestTemplate}. */
    private final RestTemplate restTemplate;

    /** Instance of type ApplicationContext. */
    private ApplicationContext applicationContext;

    /** Endpoint for onboarding tenants within the RVC Platform. */
    @Value("${revinciai.platform.master-service.urls.onboard-tenant}")
    private String masterServiceOnboardTenantUrl;

    @Override
    public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Seeds the platform with core constructs like onboarding Revinci AI as a tenant, etc.
     *
     */
    public void initialize(final String[] args) {
        final String keycloakServerUrl = keycloakProperties.getServerUrl();

        final Keycloak keycloak = getKeycloakBean();

        SeedService.LOGGER.info("Authenticating into keycloak: {}", keycloakServerUrl);
        final String accessToken = authenticateIntoKeycloak(keycloak);
        if (StringUtils.isBlank(accessToken)) {
            SeedService.LOGGER.error("Failed to authenticate into keycloak: {}", keycloakServerUrl);
            throw new IllegalArgumentException("Failed to authenticate into Keycloak.");
        }

        // Take the payload in onboard/revinciai-tenant.json and send it to the master-service using the above token
        // as the bearer token.
        SeedService.LOGGER.info("Successfully authenticated into keycloak: {}. Loading the payload", keycloakServerUrl);
        final String payload = FileUtils.readFile("onboard/revinciai-tenant.json", SeedService.class.getClassLoader());

        SeedService.LOGGER.info("Submitting the tenant onboarding request to the master-service");
        submitTenantOnboardingRequest(accessToken, payload);
    }

    /**
     * This method authenticates into the Keycloak server and returns the access token.
     *
     * @param keycloak Instance of type Keycloak.
     *
     * @return Access token.
     */
    private String authenticateIntoKeycloak(final Keycloak keycloak) {
        return getKeycloakBean().tokenManager()
                .getAccessToken()
                .getToken();
    }

    /**
     * This method retrieves a bean of type {@link Keycloak} from the {@link ApplicationContext}.
     *
     * @return Keycloak bean instance of type {@link Keycloak}.
     */
    private Keycloak getKeycloakBean() {
        SeedService.LOGGER.debug("Retrieving keycloak bean from the application context");
        return applicationContext.getBean(Keycloak.class);
    }

    /**
     * This method submits a POST-request to the master service for onboarding the "revinci" tenant.
     *
     * @param accessToken Access token.
     * @param payload     Payload containing the details of the tenant being onboarded.
     */
    private void submitTenantOnboardingRequest(final String accessToken, final String payload) {
        // Set up the headers
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // Create the HTTP entity
        final HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        // Send the POST request
        final ResponseEntity<String> response = restTemplate.postForEntity(masterServiceOnboardTenantUrl, entity,
                                                                           String.class);
        if (response.getStatusCode()
                .is2xxSuccessful()) {
            SeedService.LOGGER.info("Successfully onboarded tenant: {}", response.getBody());
        } else {
            SeedService.LOGGER.error("Failed to onboard tenant: {}", response.getStatusCode());
        }
    }
}
