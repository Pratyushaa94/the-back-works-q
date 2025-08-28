
package ai.revinci.platform.utilities.keycloak.administration.error;

import ai.revinci.platform.common.error.ErrorMessageProvider;
import ai.revinci.platform.common.error.IError;
import ai.revinci.platform.common.error.IErrorMessageProvider;


/**
 * Enum constants that represent the keycloak error codes and messages that can be used across the application.
 * <p>
 * For more details, see the documentation on {@link IError} contract.

 */
public enum KeycloakAdministrationErrors implements IError {
    // NOTE:
    // Whenever a new constant is added here, ensure that the error message for the same constant is added in
    // src/main/resources/l10n/keycloak_administration_error_messages.properties
    ADD_USER_FAILED,
    INVALID_OPERATION,
    MULTIPLE_USERS_WITH_MATCHING_USERNAME,
    REALM_EXISTS;

    /** Reference to {@link IErrorMessageProvider}, which holds the error messages. */
    private static final ErrorMessageProvider ERROR_MESSAGE_PROVIDER = ErrorMessageProvider.instance(
            "l10n/keycloak_administration_error_messages", KeycloakAdministrationErrors.class.getClassLoader());

    @Override
    public IErrorMessageProvider errorMessageProvider() {
        return KeycloakAdministrationErrors.ERROR_MESSAGE_PROVIDER;
    }
}
