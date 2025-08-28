
package ai.revinci.platform.utilities.keycloak.administration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import ai.revinci.platform.utilities.keycloak.administration.cli.ICommandLineArgument;

/**
 * An enumerated data type to capture the various command line options supported by this utility.
 *
 */
@AllArgsConstructor
public enum CommandLineArgument implements ICommandLineArgument {
    CLIENT_ID("c", "client-id", "Client identifier", true, true, "clientId", "--clientId={0}"),
    CLIENT_SECRET("s", "client-secret", "Client secret", true, true, "clientSecret", "--clientSecret={0}"),
    GRANT_TYPE("g", "grant-type", "Grant type", true, false, "grantType", "--grantType={0}"),
    INPUT_FILE("i", "input-file", "Input JSON file", true, true, "inputFile", "--inputFile={0}"),
    OPERATION("o", "operation", "Operation name (onboard-realm, add-bulk-users)", true, true, "operation",
              "--operation={0}"),
    PASSWORD("p", "password", "Keycloak password", true, true, "password", "--password={0}"),
    REALM("r", "realm", "Keycloak realm", true, false, "realm", "--realm={0}"),
    SERVER_URL("h", "server-url", "Keycloak server URL", true, true, "serverUrl", "--serverUrl={0}"),
    USER("u", "username", "Keycloak username", true, true, "username", "--username={0}");

    /** Short single-character name of the option. */
    @Getter
    private final String option;

    /** Long name of the option. */
    @Getter
    private final String longOption;

    /** Description of the option. */
    @Getter
    private final String description;

    /** Boolean indicating if this option has an argument. */
    private final boolean argument;

    /** Boolean indicating if this option is a required option. */
    @Getter
    private final boolean required;

    /** Format of the argument that Spring framework expects of program arguments. */
    @Getter
    private final String springArgument;

    /** Format of the argument and its value adhering to the expectations of Spring framework. */
    @Getter
    private final String springArgumentTemplate;

    @Override
    public boolean hasArgument() {
        return argument;
    }
}
