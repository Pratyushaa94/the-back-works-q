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

package ai.revinci.platform.services.tenant.service;

import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.configuration.TenantConfiguration;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.security.util.AuthenticationUtils;
import ai.revinci.platform.tenant.data.jpa.persistence.TenantEntity;
import ai.revinci.platform.tenant.data.jpa.repository.TenantRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {
    /** A repository implementation of type {@link TenantRepository}. */
    private final TenantRepository tenantRepository;

    /**
     * This method attempts to retrieve the tenant and realm details for the current logged-on user.
     * <p>
     * An exception is thrown if the user has not authenticated or if a tenant with the {@code realm} extracted from
     * user's authentication is not found.
     *
     * @return An instance of type {@link TenantRealm} that encapsulates the tenant and realm details.
     */
    @Instrumentation
    @Transactional(readOnly = true)
    public TenantRealm getUserTenantRealm() {
        final String realm = AuthenticationUtils.getRealmOrThrow();
        final TenantEntity tenant = tenantRepository.findByRealmNameOrThrow(realm);
        return TenantRealm.builder()
                .tenantId(tenant.getId())
                .realm(realm)
                .build();
    }


    @Instrumentation
    @Transactional
    public void syncNewTenant(@NonNull final UUID tenantId, @NonNull final String realm, @NonNull final String name,
                              @NonNull final String secret, final TenantConfiguration configuration) {

        // 2. Sync the tenant.
        syncTenant(tenantId, realm, name, secret, configuration);

    }


    private void syncTenant(@NonNull UUID tenantId, @NonNull final String realm, @NonNull final String name,
                            @NonNull final String secret, final TenantConfiguration configuration) {
        // 1. Build the TenantEntity.
        final TenantEntity newTenant = new TenantEntity();
        newTenant.setId(tenantId);
        newTenant.setRealmName(realm);
        newTenant.setName(name);
        newTenant.setSecret(secret);
        newTenant.setConfiguration(configuration);

        // 2. Save the data.
        TenantService.LOGGER.info("Tenant: {}. Realm: {}. Adding the newly provisioned tenant with name {}", tenantId,
                                  realm, name);
        final TenantEntity createdTenant = tenantRepository.save(newTenant);
        TenantService.LOGGER.info("Tenant: {}. Realm: {}. Added the newly provisioned tenant with name {}", tenantId,
                                  realm, createdTenant.getName());
    }


}
