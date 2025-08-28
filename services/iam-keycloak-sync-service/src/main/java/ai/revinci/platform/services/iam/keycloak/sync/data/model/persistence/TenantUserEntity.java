/*
 *  Copyright (c) 2025 Revinci AI.
 *
 *  All rights reserved. This software is proprietary to and embodies the
 *  confidential technology of Revinci AI. Possession,
 *  use, duplication, or dissemination of the software and media is
 *  authorized only pursuant to a valid written license from Revinci AI.
 *
 *  Unauthorized use of this software is strictly prohibited.
 *
 *  THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL REVINCI AI BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author
 *
 */

package ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.CollectionUtils;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@EntityListeners(value = {AuditingEntityListener.class})
@Entity
@Table(name = TenantUserEntity.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id",
        "email"}), @UniqueConstraint(columnNames = {"tenant_id", "username"})})
@SuperBuilder
@NoArgsConstructor
public class TenantUserEntity extends AbstractUUIDEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant_user";

    /** Reference back to the tenant to which this resource is associated to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    /** Unique identifier of the user in the IAM provider (e.g., Keycloak). */
    @ToString.Include
    @Column(name = "iam_user_id")
    private UUID iamUserId;

    /** Username of the user. */
    @ToString.Include
    @Column(name = "username", nullable = false)
    private String username;

    /** Email address of the user. */
    @Column(name = "email", nullable = false)
    private String email;

    /** First name of the user. */
    @ToString.Include
    @Column(name = "firstname", length = 150)
    private String firstname;

    /** Last name of the user. */
    @ToString.Include
    @Column(name = "lastname", length = 150)
    private String lastname;

    /** Boolean indicating if the user is an active user. */
    @Column(name = "active")
    private Boolean active;

    /** Boolean indicating if the user account is locked. */
    @Column(name = "locked")
    private Boolean locked;

    /** Boolean indicating if the user account is deleted. */
    @Column(name = "deleted")
    private Boolean deleted;

    /** Last logged in time of the user (expressed in epoch format). */
    @Column(name = "last_login")
    private Long lastLogin;

    /** List of roles assigned to this user. */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRoleEntity> roles = new ArrayList<>();

    /**
     * Checks if the user has the specified role.
     *
     * @param iamRoleId Unique identifier of the role in the IAM system.
     *
     * @return {@code true} if the user has the role, else {@code false}.
     */
    @JsonIgnore
    public boolean hasIamRoleId(final UUID iamRoleId) {
        return Optional.ofNullable(roles)
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(role -> role.getRole()
                        .getIamRoleId()
                        .equals(iamRoleId));
    }

    /**
     * Adds the role to the user if it is not already present.
     *
     * @param iamRoleId Unique identifier of the role in the IAM system.
     * @param role      Tenant role.
     *
     * @return {@code true} if the role was added, else {@code false}.
     */
    @JsonIgnore
    public boolean addRoleIfAbsent(final UUID iamRoleId, final RoleEntity role) {
        if (hasIamRoleId(iamRoleId)) {
            return false;
        }

        roles.add(UserRoleEntity.builder()
                          .id(UserRoleId.builder()
                                      .userId(getId())
                                      .roleId(iamRoleId)
                                      .build())
                          .user(this)
                          .role(role)
                          .build());
        return true;
    }

    /**
     * Removes the role from the user if it is present.
     *
     * @param iamRoleId Unique identifier of the role.
     *
     * @return {@code true} if the role was removed, else {@code false}.
     */
    @JsonIgnore
    public boolean removeRoleIfPresent(final UUID iamRoleId) {
        if (CollectionUtils.isEmpty(roles)) {
            return false;
        }

        roles.removeIf(r -> r.getRole()
                .getIamRoleId()
                .equals(iamRoleId));

        return true;
    }


}
