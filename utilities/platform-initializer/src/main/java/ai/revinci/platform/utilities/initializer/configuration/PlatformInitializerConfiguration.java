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

package ai.revinci.platform.utilities.initializer.configuration;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.utilities.initializer.configuration.properties.KeycloakProperties;

@Slf4j
@EnableConfigurationProperties(value = {KeycloakProperties.class})
@Configuration
@RequiredArgsConstructor
public class PlatformInitializerConfiguration {
    /**
     * Creates a {@link Keycloak} bean that will be used to interact with the Keycloak server.
     *
     * @param keycloakProperties The properties required to create the Keycloak instance.
     *
     * @return An instance of type {@link Keycloak}.
     */
    @Bean
    public Keycloak keycloak(@Autowired final KeycloakProperties keycloakProperties) {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.getServerUrl())
                .realm(keycloakProperties.getRealm())
                .clientId(keycloakProperties.getClientId())
                .grantType(keycloakProperties.getGrantType())
                .username(keycloakProperties.getUsername())
                .password(keycloakProperties.getPassword())
                .build();
    }

    /**
     * Creates a {@link RestTemplate} bean that will be used to interact with the RESTful services.
     *
     * @return An instance of type {@link RestTemplate}.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}