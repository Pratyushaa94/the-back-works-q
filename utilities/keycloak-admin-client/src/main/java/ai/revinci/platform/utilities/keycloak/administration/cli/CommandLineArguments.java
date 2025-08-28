
package ai.revinci.platform.utilities.keycloak.administration.cli;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.utilities.keycloak.administration.cli.error.CLIErrors;


/**
 * Implementation class that wraps the command line arguments.
 */
@Slf4j
@Getter
public class CommandLineArguments {
    /** Instance of type {@link CommandLine}. */
    private final CommandLine commandLine;

    /** Options that have to be supported. */
    private final Collection<ICommandLineArgument> arguments = new LinkedHashSet<>();

    /**
     * Private constructor.
     *
     * @param arguments   Collection of arguments expected.
     * @param commandLine Instance of type {@link CommandLine}.
     */
    private CommandLineArguments(final Collection<? extends ICommandLineArgument> arguments,
                                 final CommandLine commandLine) {
        this.commandLine = commandLine;
        this.arguments.addAll(arguments);
    }

    /**
     * Factory method to create an instance of type {@link CommandLineArguments}, which extracts the arguments from the
     * provided {@code args} and asserts with the arguments from the {@code argumentProvider}.
     *
     * @param args             Arguments provided to the program.
     * @param argumentProvider Arguments provider, which provides the arguments that are expected.
     *
     * @return Instance of type {@link CommandLineArguments}.
     */
    public static CommandLineArguments of(final String[] args, final ICommandLineArgumentProvider argumentProvider) {
        // 1. Get the arguments that have to be supported from the provider.
        final Options options = argumentProvider.asOptions();

        try {
            // 2. Parse the arguments.
            final CommandLineParser commandLineParser = new DefaultParser();
            final CommandLine commandLine = commandLineParser.parse(options, args);

            // 3. Validate the arguments.
            CommandLineArguments.validateArgumentsOrThrowException(commandLine);

            // 4. Construct the object and return.
            return new CommandLineArguments(argumentProvider.arguments(), commandLine);
        } catch (final ParseException pe) {
            argumentProvider.printHelp(options);
            throw ServiceException.of(CLIErrors.MISSING_REQUIRED_ARGUMENTS);
        } catch (final ServiceException se) {
            argumentProvider.printHelp(options);
            throw se;
        }
    }

    /**
     * This method attempts to validate the command line arguments provided in the {@code commandLine} instance.
     *
     * @param commandLine Instance of type {@link CommandLine} which contains the arguments that have to be validated.
     */
    private static void validateArgumentsOrThrowException(final CommandLine commandLine) {
        final Option[] options = commandLine.getOptions();
        for (final Option option : options) {
            // Get the option value.
            final String shortOption = option.getOpt();
            final String longOption = option.getLongOpt();
            final String optionValue = commandLine.getOptionValue(shortOption);
            // If it is required and is missing, throw an exception.
            if (option.isRequired() && StringUtils.isBlank(optionValue)) {
                // Throw an exception.
                CommandLineArguments.LOGGER.error("Required argument -{} (-{}) is missing", longOption, shortOption);
                throw ServiceException.of(CLIErrors.MISSING_REQUIRED_ARGUMENT, longOption, shortOption);
            }
        }
    }
}
