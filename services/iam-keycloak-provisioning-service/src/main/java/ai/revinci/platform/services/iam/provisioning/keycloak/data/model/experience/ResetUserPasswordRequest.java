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

package ai.revinci.platform.services.iam.provisioning.keycloak.data.model.experience;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ResetUserPasswordRequest {
    /** Name of the realm to which the users belong. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String realmName;

    /** Collection of users whose passwords have to be reset. */
    @ToString.Include
    @Builder.Default
    private Collection<UserCredential> users = new HashSet<>();

    /**
     * Checks if the request has users whose passwords have to be reset.
     *
     * @return {@code true} if the request has users whose passwords have to be reset, {@code false} otherwise.
     */
    @JsonIgnore
    public boolean hasUsers() {
        return Objects.nonNull(users) && !users.isEmpty();
    }

    /**
     * An experience model to capture the user's credentials.
     *
     */
    @ToString(onlyExplicitlyIncluded = true)
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class UserCredential {
        /** Email address of the user. */
        @ToString.Include
        @EqualsAndHashCode.Include
        private String email;

        /** Password of the user. */
        private String password;

        /** Flag to indicate if the password is temporary. */
        private boolean temporaryPassword;
    }
}
