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

package ai.revinci.platform.multitenancy.datasource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.multitenancy.data.model.experience.Tenant;

@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {
    /** Map that holds the tenant-identifier and the corresponding DataSource. */
    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();

    /** Map that holds the realm to tenant-id mapping. */
    private final Map<String, Tenant> realmTenantMap = new ConcurrentHashMap<>();

    /**
     * Constructor.
     */
    public RoutingDataSource() {
        setLenientFallback(false);
    }

    @Override
    protected String determineCurrentLookupKey() {
        // Get the realm from the TenantContext
        final TenantRealm currentTenantContext = TenantContext.get();
        if (Objects.isNull(currentTenantContext)) {
            return null;
        }

        // Refresh the tenant context if required.
        refreshTenantContextIfRequired(currentTenantContext);

        return currentTenantContext.getRealm();
    }

    @Override
    public void setTargetDataSources(@NonNull final Map<Object, Object> targetDataSources) {
        this.dataSources.putAll(targetDataSources);
        super.setTargetDataSources(dataSources);
    }


    public void addRealmMappings(@NonNull final Map<String, Tenant> realmTenantMap) {
        if (!CollectionUtils.isEmpty(realmTenantMap)) {
            this.realmTenantMap.putAll(realmTenantMap);
        }
    }


    public void addTenantDataSource(final String realm, final DataSource dataSource) {
        RoutingDataSource.LOGGER.info("Realm: {}. Received a request to add a data source.", realm);
        if (Objects.isNull(dataSource)) {
            RoutingDataSource.LOGGER.warn("Realm: {}. Received datasource is null", realm);
            return;
        }

        RoutingDataSource.LOGGER.debug("Realm: {}. No of data sources before adding a new datasource: {}. Keys: {}",
                                       realm, dataSources.size(), dataSources.keySet());
        this.dataSources.put(realm, dataSource);
        super.setTargetDataSources(dataSources);
        // Reinitialize the data sources
        super.afterPropertiesSet();
        RoutingDataSource.LOGGER.debug("Realm: {}. No of data sources after adding a new datasource: {}. Keys: {}",
                                       realm, dataSources.size(), dataSources.keySet());
    }

    /**
     * This method attempts to remove the data source for the specified {@code realm}.
     *
     * @param realm Tenant realm whose data source has to be removed.
     */
    public void removeTenantDataSource(final String realm) {
        if (StringUtils.isBlank(realm)) {
            RoutingDataSource.LOGGER.warn("Received a request to remove data source but tenant identifier is blank");
            return;
        }
        RoutingDataSource.LOGGER.info("Realm: {}. Received a request to remove the data source", realm);

        RoutingDataSource.LOGGER.debug("Realm: {}. No of data sources before removing the datasource: {}. Keys: {}",
                                       realm, dataSources.size(), dataSources.keySet());
        this.dataSources.remove(realm);
        super.setTargetDataSources(dataSources);
        // Reinitialize the data sources
        this.afterPropertiesSet();
        RoutingDataSource.LOGGER.debug("Realm: {}. No of data sources before removing the datasource: {}. Keys: {}",
                                       realm, dataSources.size(), dataSources.keySet());
    }

    /**
     * This method checks if the data source for the specified {@code realm} exists.
     *
     * @param realm Tenant realm.
     *
     * @return {@code true} if the data source exists, {@code false} otherwise.
     */
    public boolean hasTenantDataSource(final String realm) {
        if (StringUtils.isBlank(realm)) {
            RoutingDataSource.LOGGER.warn("No realm provided");
            return false;
        }

        return dataSources.containsKey(realm);
    }

    /**
     * This method refreshes the tenant context if required.
     * <p>
     * The refresh happens only if the {@link TenantRealm} in the {@link TenantContext} does not have the tenant
     * identifier.
     *
     * @param currentTenantContext Instance of type {@link TenantRealm} that indicates the current tenant context.
     */
    private void refreshTenantContextIfRequired(final TenantRealm currentTenantContext) {
        if (Objects.isNull(currentTenantContext) || StringUtils.isBlank(currentTenantContext.getRealm())) {
            return;
        }

        // Get the tenant data from the tenant-realm map
        final String realm = currentTenantContext.getRealm();
        final Tenant tenant = realmTenantMap.get(realm);
        // We refresh only if the current tenant context does not have the tenant-id.
        if (Objects.isNull(currentTenantContext.getTenantId()) && Objects.nonNull(tenant) && StringUtils.isNotBlank(
                tenant.getId())) {
            final UUID tenantId = UUID.fromString(tenant.getId());

            TenantContext.refresh(TenantRealm.builder()
                                          .realm(realm)
                                          .tenantId(tenantId)
                                          .build());
        }
    }
}
