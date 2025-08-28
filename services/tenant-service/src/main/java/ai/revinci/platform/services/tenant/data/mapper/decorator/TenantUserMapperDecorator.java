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

package ai.revinci.platform.services.tenant.data.mapper.decorator;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.services.tenant.data.mapper.TenantUserMapper;
import ai.revinci.platform.services.tenant.data.model.experience.tenant.TenantUser;
import ai.revinci.platform.tenant.data.jpa.persistence.user.TenantUserEntity;

@Slf4j
public abstract class TenantUserMapperDecorator implements TenantUserMapper {
    /** Default delegate. */
    @Autowired
    @Qualifier("delegate")
    private TenantUserMapper delegate;

    @Override
    public TenantUser transform(final TenantUserEntity source) {
        // 1. Apply the default transformation.
        final TenantUser target = delegate.transform(source);

        // 2. Apply additional transformation.
        target.setLastLogin(Optional.ofNullable(source.getLastLogin())
                                    .orElse(0L));
        target.setFullName(source.getFullName());

        return target;
    }
}
