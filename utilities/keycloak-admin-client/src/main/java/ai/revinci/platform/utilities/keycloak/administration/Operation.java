
package ai.revinci.platform.utilities.keycloak.administration;

import java.util.Optional;

import lombok.AllArgsConstructor;

import ai.revinci.platform.common.enums.IEnumValueProvider;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * An enumerated data type that captures the supported operations for this utility.
 *
 */
@AllArgsConstructor
public enum Operation implements IEnumValueProvider {
    ADD_USERS("add-users"),
    ONBOARD_REALM("onboard-realm"),
    RESET_USER_PASSWORD("reset-password");

    /** Value for this enum constant. */
    private final String value;

    /**
     * Returns the {@link Operation} enum constant for the given value.
     *
     * @param value The value for which the corresponding {@link Operation} enum constant is to be returned.
     *
     * @return The {@link Operation} enum constant for the given value. If not found, an empty {@link Optional} is
     *         returned.
     */
    @JsonCreator
    public static Optional<Operation> of(final String value) {
        for (final Operation operation : Operation.values()) {
            if (operation.value()
                         .equals(value)) {
                return Optional.of(operation);
            }
        }
        return Optional.empty();
    }

    @JsonValue
    @Override
    public String value() {
        return value;
    }
}
