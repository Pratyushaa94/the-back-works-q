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

package ai.revinci.platform.services.notification.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import ai.revinci.platform.data.jpa.repository.ExtendedJpaRepository;
import ai.revinci.platform.services.notification.data.model.persistence.TenantEntity;

@Repository
public interface TenantRepository extends ExtendedJpaRepository<TenantEntity, UUID> {
    /**
     * This method attempts to find a {@link TenantEntity} whose realm name matches the provided {@code realmName}.
     *
     * @param realmName Realm name of the tenant to find.
     *
     * @return An {@link Optional} wrapping the matching {@link TenantEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    Optional<TenantEntity> findByRealmName(String realmName);
}
