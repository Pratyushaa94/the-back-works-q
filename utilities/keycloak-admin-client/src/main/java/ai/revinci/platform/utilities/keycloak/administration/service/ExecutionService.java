
package ai.revinci.platform.utilities.keycloak.administration.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.utilities.keycloak.administration.CommandLineArgument;
import ai.revinci.platform.utilities.keycloak.administration.Operation;
import ai.revinci.platform.utilities.keycloak.administration.error.KeycloakAdministrationErrors;
import ai.revinci.platform.utilities.keycloak.administration.model.AddBulkUsersRequest;
import ai.revinci.platform.utilities.keycloak.administration.model.OnboardRealmRequest;
import ai.revinci.platform.utilities.keycloak.administration.model.ResetUserPasswordRequest;


/**
 * A {@link Service} implementation that acts as an entry point into the utility execution.
 * <p>
 * This implementation acts as a facade for the various operations executed during a specific run.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {
    /** Environment instance. */
    private final Environment environment;

    /** Instance of type {@link RealmService}. */
    private final RealmService realmService;

    /**
     * This method starts the execution process of this utility.
     */
    public void start() {
        // 1. Get the serverUrl, operation and input file.
        final String serverUrl = environment.getProperty(CommandLineArgument.SERVER_URL.getSpringArgument());
        final String operation = environment.getProperty(CommandLineArgument.OPERATION.getSpringArgument());
        final String inputFile = environment.getProperty(CommandLineArgument.INPUT_FILE.getSpringArgument());
        ExecutionService.LOGGER.info("Executing with operation = {}, inputFile = {}", operation, inputFile);

        // 2. Convert the operation to an enum type.
        final Operation op = Operation.of(operation)
                                      .orElseThrow(
                                              () -> ServiceException.of(KeycloakAdministrationErrors.INVALID_OPERATION,
                                                                        operation));

        // 2. Get the value of the input-file argument.
        switch (op) {
            case ONBOARD_REALM:
                onboardRealm(serverUrl, inputFile);
                break;
            case ADD_USERS:
                addUsers(inputFile);
                break;
            case RESET_USER_PASSWORD:
                resetUserPasswords(inputFile);
                break;
            default:
                ExecutionService.LOGGER.warn("Operation not supported: {}", op);
        }
    }

    /**
     * Onboards a realm with the given input file.
     *
     * @param serverUrl The server URL.
     * @param inputFile The input file.
     */
    protected void onboardRealm(final String serverUrl, final String inputFile) {
        final OnboardRealmRequest payload = JsonUtils.parseFileAsJson(inputFile, OnboardRealmRequest.class);
        ExecutionService.LOGGER.info("Onboarding realm with payload = {}", payload);
        realmService.onboardRealm(serverUrl, payload);
    }

    /**
     * Adds users to the realm.
     *
     * @param inputFile The input file.
     */
    protected void addUsers(final String inputFile) {
        ExecutionService.LOGGER.info("Adding bulk users");
        final AddBulkUsersRequest payload = JsonUtils.parseFileAsJson(inputFile, AddBulkUsersRequest.class);
        realmService.onboardUsers(payload);
    }

    /**
     * Resets the password for users.
     *
     * @param inputFile The input file.
     */
    protected void resetUserPasswords(final String inputFile) {
        ExecutionService.LOGGER.info("Reset password for users");
        final ResetUserPasswordRequest payload = JsonUtils.parseFileAsJson(inputFile, ResetUserPasswordRequest.class);
        realmService.resetPasswordForUsers(payload);
    }
}