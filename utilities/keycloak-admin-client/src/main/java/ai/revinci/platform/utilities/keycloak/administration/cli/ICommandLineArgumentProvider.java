
package ai.revinci.platform.utilities.keycloak.administration.cli;

import java.util.Collection;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * Base contract that implementors can use to provide the command line arguments.
 *
 */
public interface ICommandLineArgumentProvider {
    /**
     * This method provides a collection of command line arguments.
     *
     * @return Collection of command line arguments.
     */
    Collection<ICommandLineArgument> arguments();

    /**
     * This method provides the command line syntax, which will be used while printing help.
     *
     * @return Command line syntax.
     */
    String commandLineSyntax();

    /**
     * This method prints the help message for the command line arguments.
     *
     * @param options Instance of type {@link Options} that wraps all the command line arguments provided by this
     *                provider.
     */
    default void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(commandLineSyntax(), options);
    }

    /**
     * This method attempts to get the arguments from this provider instance and transforms each argument to an instance
     * of type {@link org.apache.commons.cli.Option} and returns an instance of {@link Options}.
     *
     * @return Instance of type {@link Options} that wraps all the command line arguments provided by this provider
     *         instance.
     */
    default Options asOptions() {
        final Options options = new Options();
        arguments().forEach(argument -> options.addOption(argument.asOption()));

        return options;
    }
}
