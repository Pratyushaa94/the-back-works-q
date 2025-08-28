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

package ai.revinci.platform.provisioning.iam.data.mapper.decorator;

import java.util.List;

import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.provisioning.iam.data.mapper.TenantContactMapper;
import ai.revinci.platform.provisioning.iam.data.model.experience.OnboardRealmRequest;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantContactEntity;

@Slf4j
public abstract class TenantContactMapperDecorator implements TenantContactMapper {
    /** Default delegate. */
    @Autowired
    @Qualifier("delegate")
    private TenantContactMapper delegate;

    /** Password generator that will be used to generate the password for the provided contact. */
    @Autowired
    private PasswordGenerator passwordGenerator;

    @Override
    public OnboardRealmRequest.RealmUser transform(final TenantContactEntity source) {
        // Delegate to the default mapper.
        final OnboardRealmRequest.RealmUser target = delegate.transform(source);

        // Use the generated the password.
        target.setPassword(Strings.decrypt(source.getTempPassword(), source.getTenant()
                .getSecret()
                .getBytes()));
        target.setTemporaryPassword(true);
        // Tenant contact is an admin by default.
        target.setRoles(getRoles(source.getTenant()
                                         .getRealmName()));
        return target;
    }

    /**
     * This method returns the roles that need to be assigned to the user based on the realm.
     *
     * @param realm The realm where the users belong and for whom the roles need to be assigned.
     *
     * @return A list of roles that need to be assigned to the user.
     */
    private List<OnboardRealmRequest.AssignRole> getRoles(final String realm) {
        final String adminRole = PlatformDefaults.ROLE_ADMIN.value();
        final OnboardRealmRequest.AssignRole admin = OnboardRealmRequest.AssignRole.builder()
                .roleName(adminRole)
                .build();
        if (PlatformDefaults.REALM_RVC.value()
                .equals(realm)) {
            return List.of(admin, OnboardRealmRequest.AssignRole.builder()
                    .roleName(PlatformDefaults.ROLE_SUPER_ADMIN.value())
                    .build());
        }

        return List.of(admin);
    }
}
