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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.lookup.TenantResourceStatusEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.lookup.TenantResourceTypeEntity;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = TenantResourceEntity.TABLE_NAME)
@NoArgsConstructor
public class TenantResourceEntity extends AbstractUUIDEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant_resource";

    private static final String MASTER_PLATFORM_DB_INSTANCE_TEMPLATE_NAME = "{0}-master-rvc-platform-db";

    private static final String TENANT_PLATFORM_DB_INSTANCE_TEMPLATE_NAME = "{0}{1}-{2}-rvc-platform-db";

    private static final String TENANT_PLATFORM_DB_TEMPLATE_NAME = "{0}_{1}_rvc_platform_db";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_type", nullable = false)
    private TenantResourceTypeEntity resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_status", nullable = false)
    private TenantResourceStatusEntity resourceStatus;

    @Column(name = "failure_message")
    private String failureMessage;

    @Builder.Default
    @OneToMany(mappedBy = "resource", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<TenantResourceConfigurationEntity> resourceConfigurations = new ArrayList<>();

    public Optional<String> findConfigurationKey(@NonNull final String key) {
        if (StringUtils.isBlank(key) || CollectionUtils.isEmpty(resourceConfigurations)) {
            return Optional.empty();
        }

        return resourceConfigurations.stream()
                .filter(rc -> rc.getKey()
                        .equals(key))
                .map(TenantResourceConfigurationEntity::getValue)
                .findFirst();
    }

    public String findConfigurationKey(@NonNull final String key, final String defaultValue) {
        return findConfigurationKey(key).orElse(defaultValue);
    }

    public void updateResourceConfiguration(final String key, final String value) {
        if (StringUtils.isBlank(key) || CollectionUtils.isEmpty(resourceConfigurations)) {
            return;
        }

        // Find the matching resource configuration that holds the provided key.
        final Optional<TenantResourceConfigurationEntity> config = resourceConfigurations.stream()
                .filter(rc -> rc.getKey()
                        .equals(key))
                .findFirst();
        config.ifPresent(trce -> trce.setValue(value));
    }


}
