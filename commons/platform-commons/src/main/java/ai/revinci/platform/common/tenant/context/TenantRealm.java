
package ai.revinci.platform.common.tenant.context;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * An experience model to capture the {@code realm}, {@code tenantId} or all of them to indicate
 * the tenant and application combination against which the request is made.
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
public class TenantRealm {
    /** Realm name. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private final String realm;

    /** Unique identifier of the tenant. */
    private final UUID tenantId;

}
