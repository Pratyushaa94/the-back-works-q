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

package ai.revinci.platform.services.platform.data.model.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.common.tenant.configuration.TenantConfiguration;
import ai.revinci.platform.common.util.SaltGenerator;
import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;
import ai.revinci.platform.services.platform.data.model.persistence.lookup.TenantCategoryEntity;
import ai.revinci.platform.services.platform.data.model.persistence.lookup.TenantStatusEntity;
import ai.revinci.platform.services.platform.data.model.persistence.lookup.TenantTypeEntity;
import ai.revinci.platform.services.platform.enums.TenantStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@EntityListeners(value = {AuditingEntityListener.class, TenantEntity.TenantEntityPrePersistListener.class})
@Entity
@Table(name = TenantEntity.TABLE_NAME)
@NoArgsConstructor
public class TenantEntity extends AbstractUUIDEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant";

    /** Realm name of the tenant. */
    @ToString.Include
    @Column(name = "realm_name", nullable = false, unique = true)
    private String realmName;

    /** Name of the tenant. */
    @ToString.Include
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** Brief description about the tenant. */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Tenant logo. */
    @Column(name = "logo", length = 1024)
    private String logo;

    /** Secret (salt) of the tenant. */
    @Column(name = "secret", columnDefinition = "text")
    private String secret;

    /** Address of the tenant. */
    @Column(name = "address", length = 512)
    private String address;

    /** Type of the tenant - Company or Individual. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", nullable = false)
    private TenantTypeEntity type;

    /** Category of the tenant - Small, Medium or Enterprise. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category", nullable = false)
    private TenantCategoryEntity category;

    /** Status of the tenant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    private TenantStatusEntity status;

    /** Boolean indicating if the tenant is a master tenant or not. */
    @Column(name = "master", nullable = false)
    private boolean master = false;

    /** Tenant specific configuration. */
    @Type(value = JsonType.class)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private TenantConfiguration configuration;

    /** Collection of resources associated with the tenant. */
    @OneToMany(mappedBy = "tenant", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<TenantResourceEntity> resources = new ArrayList<>();

    /** Collection of contacts associated with the tenant. */
    @OneToMany(mappedBy = "tenant", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<TenantContactEntity> contacts = new ArrayList<>();


    /**
     * This method returns a boolean indicating if the tenant has any contacts associated with it.
     *
     * @return {@code true} if the tenant has contacts, {@code false} otherwise.
     */
    public boolean hasContacts() {
        return Objects.nonNull(contacts) && !contacts.isEmpty();
    }


    /**
     * This method compares the provided status with the status of this tenant instance. If they match, true is
     * returned.
     *
     * @param tenantStatus Status to compare with.
     *
     * @return {@code true} if the status matches, {@code false} otherwise.
     */
    public boolean hasStatus(final TenantStatus tenantStatus) {
        if (Objects.isNull(tenantStatus)) {
            return false;
        }

        return Objects.nonNull(status) && tenantStatus.name()
                .equals(status.getCode());
    }

    /**
     * Pre-persist hook to generate the secret (salt) for the tenant.
     */
    @Override
    protected void onCreate() {
        super.onCreate();
        if (StringUtils.isBlank(secret)) {
            secret = SaltGenerator.generateSalt();
        }
    }

    /**
     * Entity listener for the {@link TenantEntity}.
     */
    public static class TenantEntityPrePersistListener {
        @PrePersist
        public void prePersist(final TenantEntity tenant) {
            tenant.onCreate();
        }
    }
}
