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

package ai.revinci.platform.security.data.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.revinci.platform.data.jpa.repository.LookupRepository;
import ai.revinci.platform.security.data.model.persistence.PermissionEntity;
@Repository
public interface PermissionRepository  extends LookupRepository<PermissionEntity> {
    @Query(value = """
            SELECT p.* FROM permission p
            INNER JOIN role_permission rp ON rp.permission_code = p.code
            INNER JOIN user_role ur ON ur.role_id = rp.role_id
            INNER JOIN tenant_user tu ON tu.id = ur.user_id
            INNER JOIN tenant t ON t.id = tu.tenant_id                       
            WHERE t.realm_name = :realm AND tu.email = :email
            ORDER BY p.name ASC
            """, nativeQuery = true)
    Page<PermissionEntity> findUserPermissions(@Param("realm") String realm, @Param("email") String email,
                                               Pageable pageable);

    @Query(value = """
            SELECT p.code FROM permission p
            INNER JOIN role_permission rp ON rp.permission_code = p.code
            INNER JOIN user_role ur ON ur.role_id = rp.role_id
            INNER JOIN tenant_user tu ON tu.id = ur.user_id
            INNER JOIN tenant t ON t.id = tu.tenant_id                       
            WHERE t.realm_name = :realm AND tu.email = :email
            ORDER BY p.name ASC
            """, nativeQuery = true)
    List<String> findUserPermissionCodes(@Param("realm") String realm, @Param("email") String email);

}
