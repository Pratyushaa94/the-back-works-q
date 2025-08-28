package ai.revinci.platform.utilities.keycloak.administration.cli.error;

import ai.revinci.platform.common.error.ErrorMessageProvider;
import ai.revinci.platform.common.error.IError;
import ai.revinci.platform.common.error.IErrorMessageProvider;


/**
 * Enum constants that represent the CLI error codes and messages that can be used across the application.
 * <p>
 * For more details, see the documentation on {@link IError} contract.
 *
 */
public enum CLIErrors implements IError {
    // NOTE:
    // Whenever a new constant is added here, ensure that the error message for the same constant is added in
    // src/main/resources/l10n/cli_error_messages.properties
    MISSING_REQUIRED_ARGUMENT,
    MISSING_REQUIRED_ARGUMENTS;

    /** Reference to {@link IErrorMessageProvider}, which holds the error messages. */
    private static final ErrorMessageProvider ERROR_MESSAGE_PROVIDER = ErrorMessageProvider.instance(
            "l10n/cli_error_messages", CLIErrors.class.getClassLoader());

    @Override
    public IErrorMessageProvider errorMessageProvider() {
        return CLIErrors.ERROR_MESSAGE_PROVIDER;
    }
}
