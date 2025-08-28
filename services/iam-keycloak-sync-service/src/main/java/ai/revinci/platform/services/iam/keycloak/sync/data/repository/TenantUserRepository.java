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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.revinci.platform.data.jpa.repository.ExtendedJpaRepository;
import ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence.TenantUserEntity;

@Repository
public interface TenantUserRepository extends ExtendedJpaRepository<TenantUserEntity, UUID> {
    /**
     * This method attempts to find a {@link TenantUserEntity} whose IAM user identifier matches the provided
     * {@code iamUserId} in a tenant identified by {@code tenantId}.
     *
     * @param tenantId  Unique identifier of the tenant.
     * @param iamUserId Unique identifier of the user in the IAM system (e.g., keycloak).
     *
     * @return An {@link Optional} wrapping the matching {@link TenantUserEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    @Query("""
            SELECT tue FROM TenantUserEntity tue
            WHERE tue.tenant.id = :tenantId AND tue.iamUserId = :iamUserId
            """)
    Optional<TenantUserEntity> findByIamUserId(@Param("tenantId") UUID tenantId, @Param("iamUserId") UUID iamUserId);

    /**
     * This method attempts to find a {@link TenantUserEntity} whose username matches the provided {@code username} in a
     * tenant identified by {@code tenantId}.
     *
     * @param tenantId Unique identifier of the tenant.
     * @param username Username of the user to find.
     *
     * @return An {@link Optional} wrapping the matching {@link TenantUserEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    @Query("""
            SELECT tue FROM TenantUserEntity tue
            WHERE tue.tenant.id = :tenantId AND tue.username = :username
            """)
    Optional<TenantUserEntity> findByUsername(@Param("tenantId") UUID tenantId, @Param("username") String username);

    /**
     * This method attempts to find a {@link TenantUserEntity} whose email address matches the provided {@code email} in
     * a tenant identified by {@code tenantId}.
     *
     * @param tenantId Unique identifier of the tenant.
     * @param email    Email address of the user to find.
     *
     * @return An {@link Optional} wrapping the matching {@link TenantUserEntity} if found, else returns an empty
     *         {@link Optional}.
     */
    @Query("""
            SELECT tue FROM TenantUserEntity tue
            WHERE tue.tenant.id = :tenantId AND tue.email = :email
            """)
    Optional<TenantUserEntity> findByEmail(@Param("tenantId") UUID tenantId, @Param("email") String email);

    /**
     * This method attempt to find all users who have the provided {@code iamRoleId} assigned to them in the tenant
     * identified by {@code tenantId}.
     *
     * @param tenantId  Unique identifier of the tenant.
     * @param iamRoleId Unique identifier of the role in the IAM system.
     *
     * @return A {@link List} of {@link TenantUserEntity} objects that have the provided {@code roleId} assigned to
     *         them.
     */
    @Query("""
            SELECT tue FROM TenantUserEntity tue
            INNER JOIN tue.roles ure
            WHERE tue.tenant.id = :tenantId AND ure.role.iamRoleId = :iamRoleId
            """)
    List<TenantUserEntity> findUsersAssignedToIamRoleId(@Param("tenantId") UUID tenantId,
                                                        @Param("iamRoleId") UUID iamRoleId);
}

