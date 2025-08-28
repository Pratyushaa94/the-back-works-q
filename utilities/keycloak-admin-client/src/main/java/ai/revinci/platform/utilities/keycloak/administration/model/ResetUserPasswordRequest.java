
package ai.revinci.platform.utilities.keycloak.administration.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An experience model (DTO) to capture the details for password reset of multiple users.
 *
 */
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ResetUserPasswordRequest {
    /** Name of the realm to which the users belong. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String realmName;

    /** Collection of users whose passwords have to be reset. */
    @ToString.Include
    @Builder.Default
    private Collection<UserCredential> users = new HashSet<>();

    /**
     * Checks if the request has users whose passwords have to be reset.
     *
     * @return {@code true} if the request has users whose passwords have to be reset, {@code false} otherwise.
     */
    @JsonIgnore
    public boolean hasUsers() {
        return Objects.nonNull(users) && !users.isEmpty();
    }

    /**
     * An experience model to capture the user's credentials.
     *
     */
    @ToString(onlyExplicitlyIncluded = true)
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class UserCredential {
        /** Email address of the user. */
        @ToString.Include
        @EqualsAndHashCode.Include
        private String email;

        /** Password of the user. */
        private String password;

        /** Flag to indicate if the password is temporary. */
        private boolean temporaryPassword;
    }
}
