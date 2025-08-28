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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.NonNull;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.common.tenant.configuration.TenantConfiguration;
import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.lookup.TenantCategoryEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.lookup.TenantStatusEntity;
import ai.revinci.platform.provisioning.status.handler.enums.TenantCategory;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceType;
import io.hypersistence.utils.hibernate.type.json.JsonType;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@SuperBuilder
@EntityListeners(value = {AuditingEntityListener.class})
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

    /** Secret (salt) of the tenant. */
    @Column(name = "secret", columnDefinition = "text")
    private String secret;

    /** Category of the tenant - Small, Medium or Enterprise. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category", nullable = false)
    private TenantCategoryEntity category;

    /** Status of the tenant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    private TenantStatusEntity status;

    /** Collection of resources associated with the tenant. */
    @Builder.Default
    @OneToMany(mappedBy = "tenant", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<TenantResourceEntity> resources = new ArrayList<>();

    /** Collection of contacts associated with the tenant. */
    @Builder.Default
    @OneToMany(mappedBy = "tenant", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<TenantContactEntity> contacts = new ArrayList<>();

    /** Tenant specific configuration. */
    @Type(value = JsonType.class)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private TenantConfiguration configuration;

    public boolean hasContacts() {
        return Objects.nonNull(contacts) && !contacts.isEmpty();
    }

    public Optional<TenantResourceEntity> findResourceType(@NonNull final TenantResourceType resourceType) {
        return Optional.ofNullable(resources)
                .orElse(Collections.emptyList())
                .stream()
                .filter(tre -> resourceType.name()
                        .equals(tre.getResourceType()
                                        .getCode()))
                .findFirst();
    }

    public Optional<TenantResourceEntity> findResourceById(final UUID resourceId) {
        if (Objects.isNull(resourceId)) {
            return Optional.empty();
        }

        return Optional.ofNullable(resources)
                .orElse(Collections.emptyList())
                .stream()
                .filter(tre -> resourceId.equals(tre.getId()))
                .findFirst();
    }

    public Optional<TenantResourceEntity> findResourceWithConfigurationKey(final String key, final String value) {
        if (StringUtils.isAnyBlank(key, value)) {
            return Optional.empty();
        }

        return Optional.ofNullable(resources)
                .orElse(Collections.emptyList())
                .stream()
                .filter(tre -> tre.findConfigurationKey(key)
                        .map(value::equals)
                        .orElse(false))
                .findFirst();
    }

    public boolean is(final TenantCategory tenantCategory) {
        return tenantCategory.equals(TenantCategory.valueOf(category.getCode()));
    }
}
