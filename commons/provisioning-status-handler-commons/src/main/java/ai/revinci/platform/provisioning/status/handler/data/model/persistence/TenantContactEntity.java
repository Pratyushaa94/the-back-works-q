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

package ai.revinci.platform.provisioning.status.handler.data.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@Entity
@Table(name = TenantContactEntity.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id",
        "username", "email"})})
@NoArgsConstructor
public class TenantContactEntity extends AbstractUUIDEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant_contact";

    /** Reference back to the tenant to which this instance is associated to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    /** Username of the contact. */
    @EqualsAndHashCode.Include
    @Column(name = "username", length = 254, nullable = false)
    private String username;

    /** Email address of the contact. */
    @EqualsAndHashCode.Include
    @Column(name = "email", length = 254, nullable = false)
    private String email;

    /** Firstname of the contact. */
    @ToString.Include
    @Column(name = "firstname", length = 64, nullable = false)
    private String firstname;

    /** Lastname of the contact. */
    @ToString.Include
    @Column(name = "lastname", length = 64, nullable = false)
    private String lastname;

    /** Mobile number of the contact. */
    @Column(name = "mobile", length = 24)
    private String mobile;

    /** Generated temporary password of the contact. */
    @Column(name = "temp_password", nullable = false)
    private String tempPassword;
}

