
package ai.revinci.platform.utilities.keycloak.administration;

import org.apache.commons.cli.CommandLine;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.utilities.keycloak.administration.cli.CommandLineArguments;
import ai.revinci.platform.utilities.keycloak.administration.service.ExecutionService;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class KeycloakAdministration implements CommandLineRunner {
    /** Service instance of type {@link ExecutionService}. */
    private final ExecutionService executionService;

    /**
     * Entry point method for the Keycloak Admin Client.
     *
     * @param args Arguments provided to the application.
     */
    public static void main(final String[] args) {
        try {
            // Get the command line arguments and validate them.
            final CommandLineArguments clArguments = CommandLineArguments.of(args, new CommandLineArgumentProvider());
            final CommandLine commandLine = clArguments.getCommandLine();
            final String[] arguments = clArguments.getArguments()
                    .stream()
                    .map(cla -> {
                        final String value = commandLine.getOptionValue(
                                cla.getLongOption());
                        return cla.asSpringArgument(value);
                    })
                    .toArray(String[]::new);

            // Pass the arguments as Spring Boot application arguments
            final SpringApplication app = new SpringApplication(KeycloakAdministration.class);
            app.run(arguments);
        } catch (final Exception e) {
            if (!(e instanceof ServiceException)) {
                KeycloakAdministration.LOGGER.error(e.getMessage(), e);
            }
            System.exit(1);
        }
    }

    @Override
    public void run(final String... args) {
        // Start the execution of the utility.
        executionService.start();
    }
}
