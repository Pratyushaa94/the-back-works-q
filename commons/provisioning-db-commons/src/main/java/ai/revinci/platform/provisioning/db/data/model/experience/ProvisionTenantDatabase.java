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

package ai.revinci.platform.provisioning.db.data.model.experience;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.common.enums.CloudHypervisor;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.enums.Tag;
import ai.revinci.platform.common.tenant.configuration.TenantConfiguration;
import ai.revinci.platform.common.util.JsonUtils;
import ai.revinci.platform.provisioning.db.enums.DatabaseEngine;
import ai.revinci.platform.provisioning.status.handler.enums.DeploymentEnvironment;
import ai.revinci.platform.provisioning.status.handler.enums.TenantCategory;

@Data
@SuperBuilder
@NoArgsConstructor
public class ProvisionTenantDatabase {
    /** Cloud provider type - AWS, GCP, Azure. */
    private CloudHypervisor cloudHypervisor;

    /** Cloud provider region where the database instance shall be provisioned. */
    private String region;

    /** Account / Project / Subscription id in the cloud. */
    private String accountId;

    /** Environment where the database needs to be provisioned (e.g., dev, qa, staging, demo, prod). */
    private DeploymentEnvironment deploymentEnvironment;

    /** Details of the tenant for whom the database needs to be provisioned. */
    private Tenant tenant;

    /** Details of the database that needs to be provisioned. */
    private Database database;

    /**
     * Returns the labels, which will be used as the labels / key-value pairs on the provisioned instance.
     *
     * @return A {@link Map} of key-value pairs, which will be used as labels on the resource being provisioned..
     */
    public Map<String, String> labels() {
        // @formatter:off
        return Map.of(Tag.TENANT_ID.value(), tenant.getId()
                              .toString()
                              .toLowerCase(),
                      Tag.REALM.value(), tenant.getRealmName()
                              .toLowerCase(),
                      Tag.TENANT_CATEGORY.value(), tenant.getCategory()
                              .name()
                              .toLowerCase(),
                      Tag.ENVIRONMENT.value(), deploymentEnvironment.value()
                              .toLowerCase());
        // @formatter:on
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class Tenant {
        /** Unique identifier of the tenant. */
        private UUID id;

        /** Unique identifier of the tenant resource in the RVC platform. */
        private UUID resourceId;

        /** Tenant realm name. */
        private String realmName;

        /** Name of the tenant. */
        private String name;

        /** Tenant secret. */
        private String secret;

        /** Categorization of the tenant i.e., small, medium or enterprise tenant. */
        private TenantCategory category;

        /** Tenant specific configuration. */
        private TenantConfiguration configuration;

        /**
         * Returns {@code true} if the tenant is an enterprise tenant; {@code false} otherwise.
         *
         * @return True if the tenant is an enterprise tenant, false otherwise.
         */
        public boolean isEnterpriseTenant() {
            return TenantCategory.ENTERPRISE.is(category);
        }

        /**
         * Returns the tenant details as a {@link Map}.
         *
         * @return A {@link Map} of key-value pairs, which will be used for use-cases like message publishing, etc.
         */
        public Map<String, Object> asMap() {

            final String tenantConfiguration = JsonUtils.serialize(Optional.ofNullable(configuration)
                                                                           .orElse(TenantConfiguration.builder()
                                                                                           .build()));
            return Map.of(Key.TENANT_ID.value(), id, Key.REALM.value(),
                          realmName, Key.NAME.value(), name,
                          Key.SECRET.value(), secret,
                          Key.TENANT_CONFIGURATION.value(), tenantConfiguration);
        }
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class Database {
        /** Details of the database engine that needs to be provisioned. */
        private DatabaseEngine engine;

        /** Database instance name. */
        private String instanceName;

        /** Schema name. */
        private String schema;

        /** Database name. */
        private String dbName;

        /** Database username. */
        private String username;

        /** Root password. */
        private byte[] rootPassword;

        /** JDBC URL to connect to the database. */
        private String jdbcUrl;
    }

}
