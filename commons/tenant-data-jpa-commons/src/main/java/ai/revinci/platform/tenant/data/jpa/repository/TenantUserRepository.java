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

package ai.revinci.platform.tenant.data.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.data.jpa.repository.ExtendedJpaRepository;
import ai.revinci.platform.security.util.AuthenticationUtils;
import ai.revinci.platform.tenant.data.jpa.persistence.user.TenantUserEntity;
import ai.revinci.platform.tenant.data.jpa.projection.UsernameIdProjection;

@Repository
public interface TenantUserRepository extends ExtendedJpaRepository<TenantUserEntity, UUID> {
    default TenantUserEntity findAuthenticatedUserOrThrow() {
        return findById(AuthenticationUtils.getAuthenticatedUserIdOrThrow()).orElseThrow(
                () -> ServiceException.of(CommonErrors.MISSING_AUTHENTICATION));
    }

    @Query(value = """
            SELECT
                tue.id AS id,
                tue.username AS username
            FROM TenantUserEntity tue
            WHERE tue.username IN (:usernames) AND
                tue.deleted = FALSE AND
                tue.locked = FALSE AND
                tue.active = TRUE
            """)
    Collection<UsernameIdProjection> findUserIdsByUsernames(@Param("usernames") Collection<String> usernames);

    @Query(value = """
            SELECT tue FROM TenantUserEntity tue
            WHERE (tue.email IN (:emailOrUsernames) OR tue.username IN (:emailOrUsernames)) AND
                tue.deleted = FALSE AND
                tue.locked = FALSE AND
                tue.active = TRUE
            """)
    List<TenantUserEntity> findByEmailOrUsernames(@Param("emailOrUsernames") Collection<String> emailOrUsernames);

    @Query(value = """
            SELECT tue FROM TenantUserEntity tue
            WHERE (tue.email = :emailOrUsername OR tue.username = :emailOrUsername) AND 
                tue.deleted = FALSE AND 
                tue.locked = FALSE AND
                tue.active = TRUE
            """)
    Optional<TenantUserEntity> findByEmailOrUsername(@Param("emailOrUsername") String emailOrUsername);

    default TenantUserEntity findByEmailOrUsernameOrThrow(final String emailOrUsername) {
        return findByEmailOrUsername(emailOrUsername).orElseThrow(
                () -> ServiceException.of(CommonErrors.RESOURCE_NOT_FOUND));
    }


}
