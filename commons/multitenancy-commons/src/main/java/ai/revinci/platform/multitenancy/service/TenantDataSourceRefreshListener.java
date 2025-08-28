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

package ai.revinci.platform.multitenancy.service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.multitenancy.data.model.experience.Tenant;
import ai.revinci.platform.multitenancy.data.repository.MultiTenantRepository;
import ai.revinci.platform.multitenancy.datasource.RoutingDataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantDataSourceRefreshListener {
    /** A datasource instance of type {@link RoutingDataSource}. */
    private final RoutingDataSource routingDataSource;

    /** A repository implementation of type {@link MultiTenantRepository}. */
    private final MultiTenantRepository multiTenantRepository;

    /**
     * This method is called whenever a new tenant is provisioned.
     *
     * @param tenantId Unique identifier of the new tenant that was provisioned.
     * @param realm    Realm of the tenant.
     */
    public void tenantProvisioned(final UUID tenantId, final String realm) {
        TenantDataSourceRefreshListener.LOGGER.info("Tenant: {}. Realm: {}. Attempting to add the datasource", tenantId,
                                                    realm);
        // 1. Find the datasource for the newly provisioned tenant.
        final DataSource dataSource = multiTenantRepository.getDataSourceForTenant(tenantId, realm);

        // 2. Do we have one?
        if (Objects.isNull(dataSource)) {
            TenantDataSourceRefreshListener.LOGGER.warn("Tenant: {}. Realm: {}. No datasource found for the tenant",
                                                        tenantId, realm);
            return;
        }

        // 3. Add the new datasource to in-memory
        routingDataSource.addTenantDataSource(realm, dataSource);
        TenantDataSourceRefreshListener.LOGGER.info("Tenant: {}. Realm: {}. Successfully added the datasource",
                                                    tenantId, realm);

        // 4. Add the realm mapping as well.
        final Tenant tenant = multiTenantRepository.findTenant(tenantId);
        if (Objects.nonNull(tenant)) {
            TenantDataSourceRefreshListener.LOGGER.info("Tenant: {}. Realm: {}. Successfully added the realm mapping",
                                                        tenantId, realm);
            routingDataSource.addRealmMappings(Map.of(tenant.getRealmName(), tenant));
        }
    }
}
