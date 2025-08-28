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

package ai.revinci.platform.multitenancy.data.repository;

import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.multitenancy.configuration.TenantQueryProperties;
import ai.revinci.platform.multitenancy.data.model.experience.Tenant;
import ai.revinci.platform.multitenancy.error.MultiTenancyErrors;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Slf4j
@Repository
public class MultiTenantRepository extends NamedParameterJdbcDaoSupport {
    private final DataSource defaultDataSource;

    private final TenantQueryProperties tenantQueryProperties;

    public MultiTenantRepository(@Qualifier("defaultDataSource") final DataSource defaultDataSource,
                                 final TenantQueryProperties tenantQueryProperties) {
        this.defaultDataSource = defaultDataSource;
        this.tenantQueryProperties = tenantQueryProperties;
    }

    public DataSource getDataSourceForTenant(final UUID tenantId, final String realm) {
        // 1. Create NonMasterTenant instance.
        final Tenant tenant = findTenant(tenantId);
        if (Objects.isNull(tenant)) {
            MultiTenantRepository.LOGGER.error("Tenant: {}. Realm: {}. Unable to find the tenant", tenantId, realm);
            throw ServiceException.of(CommonErrors.TENANT_NOT_FOUND, tenantId.toString());
        }

        // 2. Create the datasource for the tenant.
        final Map<Object, Object> datasourceMap = new HashMap<>();
        findDataSourcesForTenants(List.of(tenant), datasourceMap);

        // 3. Return the datasource for the tenant.
        return (datasourceMap.get(realm) instanceof DataSource ds) ?
                ds :
                null;
    }

    public Tenant findTenant(final UUID tenantId) {
        // Get tenant IDs for the current page
        final SqlParameterSource paramSource = new MapSqlParameterSource().addValue(Key.TENANT_ID.value(), tenantId,
                                                                                    Types.OTHER);

        final String query = tenantQueryProperties.getQueryForTenant();
        MultiTenantRepository.LOGGER.debug("Query to find tenant: {}", query);

        final List<Tenant> data = getNamedParameterJdbcTemplateOrThrow().query(query, paramSource,
                                                                               (rs, rowNum) -> transform(rs));
        return data.isEmpty() ?
                null :
                data.getFirst();
    }

    private Tenant transform(final ResultSet rs) throws SQLException {
        return Tenant.builder()
                .id(rs.getString("id"))
                .realmName(rs.getString("realm_name"))
                .secret(rs.getString("secret"))
                  .build();
    }

    private void findDataSourcesForTenants(final Collection<Tenant> tenants, final Map<Object, Object> dataSources) {
        // Get database configuration details for the retrieved tenant IDs
        final Collection<UUID> tenantUUIDs = tenants.stream()
                .map(nmt -> UUID.fromString(nmt.getId()))
                .collect(Collectors.toSet());
        final SqlParameterSource paramSource = new MapSqlParameterSource().addValue(Key.TENANT_IDS.value(),
                                                                                    tenantUUIDs);
        final String query = tenantQueryProperties.getQueryForTenantDatabaseConfigurations();
        MultiTenantRepository.LOGGER.debug("Query to retrieve data sources for active tenants: {}", query);

        final SqlRowSet rowSet = getNamedParameterJdbcTemplateOrThrow().queryForRowSet(query, paramSource);

        // Read from the database and transform the records into a Map where the key is the tenant identifier and the
        // value is a Map containing key-value pairs representing the database configuration details.
        // For example: In the case of database resource, the keys can be jdbcUrl, username, password, schema, etc.
        final Map<String, Map<String, String>> tenantDbConfigMap = new HashMap<>();
        while (rowSet.next()) {
            final String realmName = rowSet.getString("realm_name");
            final String key = rowSet.getString("configuration_key");
            final String value = rowSet.getString("configuration_value");

            tenantDbConfigMap.computeIfAbsent(realmName, k -> new HashMap<>())
                    .put(key, value);
        }

        // Transform the non-master tenants into a Map where the key is the realm-name and the value is the
        // non-master tenant.
        final Map<String, Tenant> realmMap = tenants.stream()
                .collect(Collectors.toMap(Tenant::getRealmName, nmt -> nmt));
        // Now, transform the above Map into a collection of DataSource objects, which will be held in-memory
        // and used for data source routing.
        for (final Map.Entry<String, Map<String, String>> entry : tenantDbConfigMap.entrySet()) {
            final String realm = entry.getKey();
            final Map<String, String> config = entry.getValue();
            final DataSource dataSource = createDataSource(PlatformDefaults.POSTGRESQL_DRIVER_CLASSNAME.value(),
                                                           config.get(Key.JDBC_URL.value()),
                                                           config.get(Key.USERNAME.value()),
                                                           Strings.decrypt(config.get(Key.PASSWORD.value()),
                                                                           realmMap.get(realm)
                                                                                   .getSecret()
                                                                                   .getBytes()),
                                                           config.get(Key.SCHEMA.value()));
            dataSources.put(realm, dataSource);
        }

        MultiTenantRepository.LOGGER.info("Number of tenant data sources loaded: {}", dataSources.size());
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplateOrThrow() {
        final NamedParameterJdbcTemplate jdbcTemplate = getNamedParameterJdbcTemplate();
        if (Objects.isNull(jdbcTemplate)) {
            MultiTenantRepository.LOGGER.error("NamedParameterJdbcTemplate is null.");
            throw ServiceException.of(MultiTenancyErrors.INVALID_JDBC_TEMPLATE);
        }

        return jdbcTemplate;
    }

    private DataSource createDataSource(final String driverClassName, final String url, final String username,
                                        final String password, final String schema) {
        // Use HikariCP for connection pooling
        final HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // Set tenant-specific connection pool properties

        // Max number of connections in the pool
        config.setMaximumPoolSize(10);
        // Minimum idle connections
        config.setMinimumIdle(2);
        // Idle timeout in milliseconds (e.g., 30 seconds)
        config.setIdleTimeout(30000);
        // Max lifetime of a connection in milliseconds (e.g., 10 minutes)
        config.setMaxLifetime(600000);
        // Connection timeout in milliseconds (e.g., 20 seconds)
        config.setConnectionTimeout(20000);
        // Set the search_path to point to the tenant-specific schema.
        if (StringUtils.isNotBlank(schema)) {
            config.setConnectionInitSql(PatternTemplate.DB_SEARCH_PATH.format(schema));
        }

        // Create and return the HikariDataSource
        return new HikariDataSource(config);
    }

    public int findCountOfTenants() {
        final String query = tenantQueryProperties.getQueryForCountOfTenants();
        MultiTenantRepository.LOGGER.debug("Query to find count of tenant identifiers: {}", query);

        final Integer count = getJdbcTemplateOrThrow().queryForObject(query, Integer.class);
        return Optional.ofNullable(count)
                .orElse(0);
    }

    private JdbcTemplate getJdbcTemplateOrThrow() {
        final JdbcTemplate jdbcTemplate = getJdbcTemplate();
        if (Objects.isNull(jdbcTemplate)) {
            MultiTenantRepository.LOGGER.error("JdbcTemplate is null.");
            throw ServiceException.of(MultiTenancyErrors.INVALID_JDBC_TEMPLATE);
        }

        return jdbcTemplate;
    }

    public void getDataSourcesForTenants(final Map<Object, Object> dataSources) {
        // Get the count of non-master tenant identifiers
        final int count = findCountOfTenants();
        MultiTenantRepository.LOGGER.info("Count of tenants: {}", count);

        // Page size to use.
        final int pageSize = 20;
        // How many pages do we need to traverse?
        final int pageCount = pageCount(count, pageSize);
        // Loop through the pages.
        for (int page = 0; page < pageCount; page++) {
            final int offset = offsetForPage(page, pageSize);
            MultiTenantRepository.LOGGER.debug("Iterating page {} with page size {}", page, pageSize);
            // Get tenant IDs for the current page
            final Collection<Tenant> tenants = findTenants(offset, pageSize);
            if (!tenants.isEmpty()) {
                MultiTenantRepository.LOGGER.debug("Number of tenant ids in page {}, page size {}: {}", page, pageSize,
                                                   tenants.size());
                findDataSourcesForTenants(tenants, dataSources);
            }
        }
    }

    private int offsetForPage(final int pageNumber, final int pageSize) {
        return pageNumber * pageSize;
    }


    public Collection<Tenant> findTenants(final int offset, final int limit) {
        // Get tenant IDs for the current page
        final SqlParameterSource paramSource = new MapSqlParameterSource().addValue(Key.OFFSET.value(), offset,
                                                                                    Types.INTEGER)
                .addValue(Key.LIMIT.value(), limit,
                          Types.INTEGER);

        final String query = tenantQueryProperties.getQueryForTenants();
        MultiTenantRepository.LOGGER.debug("Query to find tenant identifiers: {}", query);

        return getNamedParameterJdbcTemplateOrThrow().query(query, paramSource, (rs, rowNum) -> transform(rs));
    }

    private int pageCount(final int totalRecords, final int pageSize) {
        return (int) Math.ceil((double) totalRecords / pageSize);
    }


}
