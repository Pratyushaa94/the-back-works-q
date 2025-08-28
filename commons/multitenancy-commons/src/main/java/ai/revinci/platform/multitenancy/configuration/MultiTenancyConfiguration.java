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

package ai.revinci.platform.multitenancy.configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.multitenancy.data.model.experience.Tenant;
import ai.revinci.platform.multitenancy.data.repository.MultiTenantRepository;
import ai.revinci.platform.multitenancy.datasource.RoutingDataSource;
import ai.revinci.platform.multitenancy.filter.TenantFilter;
import ai.revinci.platform.multitenancy.service.TenantDataSourceRefreshListener;

@Slf4j
@ComponentScan(basePackageClasses = {MultiTenantRepository.class, TenantQueryProperties.class, TenantFilter.class,
        TenantDataSourceRefreshListener.class})
@Configuration
public class MultiTenancyConfiguration {

    /**
     * This method is responsible to create a bean instance of type {@link javax.sql.DataSource}, which acts as the
     * default data source.
     *
     * @param dataSourceProperties Instance of type
     *                             {@link org.springframework.boot.autoconfigure.jdbc.DataSourceProperties} that holds
     *                             the properties of the default data source.
     *
     * @return Instance of type {@link javax.sql.DataSource}.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource defaultDataSource(final DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder()
                .build();
    }

    /**
     * This method is responsible to create a bean instance of type
     * {@link org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource}.
     * <p>
     * This provides the functionality of routing the requests to the appropriate tenant-specific data source based on
     * the tenant identifier.
     *
     * @return Instance of type {@link org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource}.
     */
    @Bean
    @Primary
    public RoutingDataSource dataSource(@Autowired final MultiTenantRepository multiTenantRepository,
                                        @Autowired DataSourceProperties dataSourceProperties) {
        // 1. Create the routing data source.
        final RoutingDataSource routingDataSource = new RoutingDataSource();

        // 2. Load all the tenant-specific data sources.
        final Map<Object, Object> dataSources = new HashMap<>();
        multiTenantRepository.getDataSourcesForTenants(dataSources);

        // 3. Set the retrieved tenant-specific data sources on the routing data source so that the routing data source
        // can route the requests to the appropriate data source.
        routingDataSource.setTargetDataSources(dataSources);

        // 4. Let us also load the realm tenant mappings.
        final int count = multiTenantRepository.findCountOfTenants();
        if (count > 0) {
            // Find the tenants using this count as the pagination size.
            routingDataSource.addRealmMappings(multiTenantRepository.findTenants(0, count)
                                                       .stream()
                                                       .collect(Collectors.toMap(Tenant::getRealmName,
                                                                                 tenant -> tenant)));
        }

        // 5. Fetch the master data source and set it as the default data source.
        routingDataSource.setDefaultTargetDataSource(defaultDataSource(dataSourceProperties));

        return routingDataSource;
    }
}
