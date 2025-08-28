
package ai.revinci.platform.utilities.keycloak.administration.configuration;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.utilities.keycloak.administration.CommandLineArgument;

/**
 * A {@link Configuration} class that creates the necessary beans required by the Keycloak Admin Client.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KeycloakAdministrationConfiguration {
    /** Environment instance. */
    private final Environment environment;

    /**
     * Creates a {@link Keycloak} bean that will be used to interact with the Keycloak server.
     *
     * @return An instance of type {@link Keycloak}.
     */
    @Bean
    public Keycloak keycloak() {
        final String serverUrl = environment.getProperty(CommandLineArgument.SERVER_URL.getSpringArgument());
        final String realm = environment.getProperty(CommandLineArgument.REALM.getSpringArgument());
        final String grantType = environment.getProperty(CommandLineArgument.GRANT_TYPE.getSpringArgument());
        final String clientId = environment.getProperty(CommandLineArgument.CLIENT_ID.getSpringArgument());
        final String username = environment.getProperty(CommandLineArgument.USER.getSpringArgument());
        final String password = environment.getProperty(CommandLineArgument.PASSWORD.getSpringArgument());

        return KeycloakBuilder.builder()
                              .serverUrl(serverUrl)
                              .realm(realm)
                              .grantType(grantType)
                              .username(username)
                              .password(password)
                              .clientId(clientId)
                              .build();
    }
}
