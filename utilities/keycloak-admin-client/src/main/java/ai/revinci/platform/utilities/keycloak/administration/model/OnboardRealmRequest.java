
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
 * An experience model (DTO) to capture the details of a new realm that needs to be created / onboarded.
 *
 * <ul>
 *     <li>If the request has roles to be created, the utility shall create the new roles.</li>
 *     <li>Likewise, if the request has users to be created, the utility shall create the new users.</li>
 * </ul>
 *
 */
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OnboardRealmRequest {
    /** Name of the realm to create. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String realmName;

    /** Application Url that will be used in redirect urls, etc. */
    private String applicationUrl;

    /** Collection of roles to be created. */
    @ToString.Include
    @Builder.Default
    private Collection<AddRoleRequest> roles = new HashSet<>();

    /** Collection of new users to be created. */
    @ToString.Include
    @Builder.Default
    private Collection<AddRealmUserRequest> users = new HashSet<>();

    /**
     * Checks if the request has roles to create.
     *
     * @return {@code true} if the request has roles to create, {@code false} otherwise.
     */
    @JsonIgnore
    public boolean hasRoles() {
        return Objects.nonNull(roles) && !roles.isEmpty();
    }

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
