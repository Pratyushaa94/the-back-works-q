
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
 * An experience model (DTO) to capture the details of the new users who have to be onboarded within a specific realm.
 *
 */
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AddBulkUsersRequest {
    /** Name of the realm to create. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String realmName;

    /** Collection of new users to be created. */
    @ToString.Include
    @Builder.Default
    private Collection<AddRealmUserRequest> users = new HashSet<>();

    /**
     * Checks if the request has users to create.
     *
     * @return {@code true} if the request has users to create, {@code false} otherwise.
     */
    @JsonIgnore
    public boolean hasUsers() {
        return Objects.nonNull(users) && !users.isEmpty();
    }
}
