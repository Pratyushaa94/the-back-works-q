
package ai.revinci.platform.utilities.keycloak.administration.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * An experience model (DTO) to capture the details of a new user that needs to be created within the realm.
 *
 */
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AddRealmUserRequest {
    /** Username of the user. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String username;

    /** Email address of the user. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String email;

    /** Firstname of the user */
    @ToString.Include
    private String firstname;

    /** Lastname of the user. */
    @ToString.Include
    private String lastname;

    /** Password of the user. */
    private String password;

    /** Flag to indicate if the password is temporary. */
    private boolean temporaryPassword;

    /** Collection of roles to be assigned to the user. */
    @Builder.Default
    private Collection<AssignRole> roles = new HashSet<>();


    /**
     * Checks if the user has roles to be assigned.
     *
     * @return {@code true} if the user has roles to be assigned, {@code false} otherwise.
     */
    public boolean hasRoles() {
        return !roles.isEmpty();
    }

    /**
     * Returns the names of the roles that need to be assigned to the new user being created.
     *
     * @return Collection of role names to assign.
     */
    public List<String> roleNamesToAssign() {
        return roles.stream()
                    .map(AssignRole::getRoleName)
                    .collect(Collectors.toList());
    }

    /**
     * An experience model (DTO) to capture the details of the role that needs to be assigned to the new user being
     * created.
     *
     */
    @ToString(onlyExplicitlyIncluded = true)
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class AssignRole {
        /** Name of the role to be assigned to the new user. */
        private String roleName;
    }
}
