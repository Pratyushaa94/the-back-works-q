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

package ai.revinci.platform.tenant.data.jpa.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.data.jpa.listener.TenantListener;
import ai.revinci.platform.data.jpa.persistence.AbstractTenantAwareEntity;
import ai.revinci.platform.tenant.data.jpa.persistence.lookup.DataTypeEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(value = {TenantListener.class})
@Entity
@Table(name = UserMetadataEntity.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id",
        "user_id", "key"})})
public class UserMetadataEntity extends AbstractTenantAwareEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant_user_metadata";

    /** Reference back to the tenant to which this resource is associated to. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** User entity reference. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private TenantUserEntity user;

    /** Key for the user. */
    @ToString.Include
    @Column(name = "key", length = 128, nullable = false)
    private String key;

    /** Display name of the user. */
    @ToString.Include
    @Column(name = "display_name", length = 64, nullable = false)
    private String displayName;

    /** Value of the user. */
    @Column(name = "value", nullable = false)
    private String value;

    /** Data type of the value. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_type")
    private DataTypeEntity valueType;

    /**
     * This method initializes the parent of this user metadata record to the provided {@code user}.
     *
     * @param user User record that will be used as the parent of this user metadata record.
     */
    @JsonIgnore
    public void initializeParent(final TenantUserEntity user) {
        // Set the parent to the provided user.
        if (Objects.isNull(this.user)) {
            this.user = user;
        }
    }
}
