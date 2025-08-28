
package ai.revinci.platform.utilities.keycloak.administration.cli;

import java.text.MessageFormat;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Base contract that acts as a definition for command line argument.
 *
 */
public interface ICommandLineArgument {
    /**
     * This method transforms the provided {@code option} to an instance of type {@link Option}.
     *
     * @param option {@link ICommandLineArgument} object that has to be transformed to {@link Option}.
     *
     * @return Instance of type {@link Option}.
     */
    static Option transform(final ICommandLineArgument option) {
        return Option.builder()
                     .option(option.getOption())
                     .longOpt(option.getLongOption())
                     .desc(option.getDescription())
                     .hasArg(option.hasArgument())
                     .required(option.isRequired())
                     .build();
    }

    /**
     * This method attempts to transform the provided array of {@code options} to an instance of type {@link Options}.
     *
     * @param options Array of {@link ICommandLineArgument} objects that have to be transformed to {@link Options}.
     *
     * @return Instance of type {@link Options}.
     */
    static Options transform(final ICommandLineArgument... options) {
        final Options commandLineOptions = new Options();
        for (final ICommandLineArgument clo : options) {
            commandLineOptions.addOption(ICommandLineArgument.transform(clo));
        }

        return commandLineOptions;
    }

    /**
     * This method returns the short single-character name of the option (e.g., 'u' for user, 'p' for password, etc.).
     *
     * @return Short single-character name of the option.
     */
    String getOption();

    /**
     * This method returns the long name of the option (e.g., 'user' for user, 'password' for password, etc.).
     *
     * @return Long name of the option.
     */
    String getLongOption();

    /**
     * This method returns the description of the option.
     *
     * @return Description of the option.
     */
    String getDescription();

    /**
     * This method returns a boolean indicating if this option has an argument.
     * <p>
     * If true, what follows this option is the argument. If false, what follows next is a new argument.
     * <p>
     * For example: -enable-xyz itself indicates a boolean flag and there is no need to follow it up with a argument
     * value.
     *
     * @return True if this option has an argument; false otherwise.
     */
    boolean hasArgument();

    /**
     * This method returns a boolean indicating if this option is a required option.
     *
     * @return True if this option is required; false otherwise.
     */
    boolean isRequired();

    /**
     * This method returns the argument name adhering to the spring naming convention.
     *
     * @return Argument name that Spring framework looks for.
     */
    String getSpringArgument();

    /**
     * This method returns the argument and its value adhering to the format expected by Spring framework.
     *
     * @return Format of the argument and its value adhering to Spring expectations.
     */
    String getSpringArgumentTemplate();

    /**
     * This method transforms this instance to an instance of type {@link Option}.
     *
     * @return Instance of type {@link Option}.
     */
    default Option asOption() {
        return Option.builder()
                     .option(getOption())
                     .longOpt(getLongOption())
                     .desc(getDescription())
                     .hasArg(hasArgument())
                     .required(isRequired())
                     .build();
    }

    /**
     * This method returns the argument in the Spring framework format replacing the placeholders with the provided
     * arguments.
     *
     * @param args Arguments that have to be replaced in the template.
     *
     * @return Argument in the Spring framework format.
     */
    default String asSpringArgument(final Object... args) {
        return MessageFormat.format(getSpringArgumentTemplate(), args);
    }
}
