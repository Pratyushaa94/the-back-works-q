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

package ai.revinci.platform.services.iam.keycloak.sync.data.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.revinci.platform.data.jpa.repository.ExtendedJpaRepository;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.RoleEntity;

@Repository
public interface RoleRepository extends ExtendedJpaRepository<RoleEntity, UUID> {
    /**
     * This method attempts to find a {@link RoleEntity} whose IAM role identifier matches the provided
     * {@code iamRoleId} in a tenant identified by {@code tenantId}.
     *
     * @param tenantId  Unique identifier of the tenant.
     * @param iamRoleId Unique identifier of the role in the IAM system (e.g., keycloak).
     *
     * @return An {@link Optional} wrapping the matching {@link RoleEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    @Query("""
            SELECT re FROM RoleEntity re
            WHERE re.tenant.id = :tenantId AND re.iamRoleId = :iamRoleId
            """)
    Optional<RoleEntity> findByIamRoleId(@Param("tenantId") UUID tenantId, @Param("iamRoleId") UUID iamRoleId);

    /**
     * This method attempts to find a {@link RoleEntity} whose name matches the provided {@code name} in a tenant
     * identified by {@code tenantId}.
     *
     * @param tenantId Unique identifier of the tenant.
     * @param name     Name of the role to find.
     *
     * @return An {@link Optional} wrapping the matching {@link RoleEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    @Query("""
            SELECT re FROM RoleEntity re
            WHERE re.tenant.id = :tenantId AND re.name = :name
            """)
    Optional<RoleEntity> findByName(@Param("tenantId") UUID tenantId, @Param("name") String name);

    /**
     * This method attempts to find {@link RoleEntity} objects whose name matches the provided {@code names} in a tenant
     * identified by {@code tenantId}.
     *
     * @param tenantId Unique identifier of the tenant.
     * @param names    Collection of role names to find.
     *
     * @return An {@link Optional} wrapping the matching {@link RoleEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    @Query("""
            SELECT re FROM RoleEntity re
            WHERE re.tenant.id = :tenantId AND re.name IN (:names)
            """)
    Collection<RoleEntity> findByNames(@Param("tenantId") UUID tenantId, @Param("names") Collection<String> names);
}
