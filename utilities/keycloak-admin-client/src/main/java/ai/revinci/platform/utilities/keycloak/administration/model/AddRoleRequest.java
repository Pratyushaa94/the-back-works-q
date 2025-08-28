
package ai.revinci.platform.utilities.keycloak.administration.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * An experience model (DTO) to capture the details of a new role that needs to be created.
 *
 */
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AddRoleRequest {
    /** Name of the role to be created. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String name;

    /** Brief description about the role. */
    private String description;
}