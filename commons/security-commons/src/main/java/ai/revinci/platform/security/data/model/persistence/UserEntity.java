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

package ai.revinci.platform.security.data.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@EntityListeners(value = {AuditingEntityListener.class})
@Entity
@Table(name = UserEntity.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "email"}),
        @UniqueConstraint(columnNames = {"tenant_id", "username"})})
@SuperBuilder
@NoArgsConstructor
public class UserEntity extends AbstractUUIDEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant_user";

    /** Reference back to the tenant to which this resource is associated to. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

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

    /** Epoch time indicating the last-login time of the user. */
    @Column(name = "last_login")
    private Long lastLogin;

    /**
     * Returns the full name of the user.
     *
     * @return Full name of the user.
     */
    @JsonIgnore
    public String getFullName() {
        return Strings.getFullName(firstname, lastname);
    }
}
