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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.security.util.AuthenticationUtils;
import ai.revinci.platform.services.tenant.data.mapper.TenantUserMapper;
import ai.revinci.platform.services.tenant.data.model.experience.tenant.TenantUser;
import ai.revinci.platform.tenant.data.jpa.persistence.user.TenantUserEntity;
import ai.revinci.platform.tenant.data.jpa.repository.TenantUserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantUserService {
    /** A repository implementation of type {@link TenantUserRepository}. */
    private final TenantUserRepository userRepository;

    /** A mapper implementation of type {@link TenantUserMapper}. */
    private final TenantUserMapper tenantUserMapper;

    @Instrumentation
    @Transactional(readOnly = true)
    public TenantUser getUserProfile() {
        // 1. Get the current authentication details.
        final String email = AuthenticationUtils.getPrincipalOrThrow();

        // 2. Find the user by email.
        final TenantUserEntity user = userRepository.findByEmailOrUsernameOrThrow(email);

        // 3. Transform and return.
        return tenantUserMapper.transform(user);
    }
}
