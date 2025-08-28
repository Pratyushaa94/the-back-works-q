
package ai.revinci.platform.utilities.keycloak.administration;

import java.util.Arrays;
import java.util.Collection;

import ai.revinci.platform.utilities.keycloak.administration.cli.ICommandLineArgument;
import ai.revinci.platform.utilities.keycloak.administration.cli.ICommandLineArgumentProvider;


/**
 * An implementation of {@link ICommandLineArgumentProvider} that provides the arguments to be used by the utility.
 *
 */
public class CommandLineArgumentProvider implements ICommandLineArgumentProvider {
    @Override
    public Collection<ICommandLineArgument> arguments() {
        return Arrays.asList(CommandLineArgument.SERVER_URL, CommandLineArgument.REALM, CommandLineArgument.GRANT_TYPE,
                             CommandLineArgument.USER, CommandLineArgument.PASSWORD, CommandLineArgument.CLIENT_ID,
                             CommandLineArgument.OPERATION, CommandLineArgument.INPUT_FILE);
    }

    @Override
    public String commandLineSyntax() {
        return "java -jar keycloak-admin-client-<version>.jar <options> where <options> are:";
    }
}
