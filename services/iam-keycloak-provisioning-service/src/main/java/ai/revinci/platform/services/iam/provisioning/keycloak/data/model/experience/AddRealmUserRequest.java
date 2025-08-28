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
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AddRealmUserRequest {
    /** Username of the user. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String username;

    /** Email address of the user. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String email;

    /** Firstname of the user */
    @ToString.Include
    private String firstname;

    /** Lastname of the user. */
    @ToString.Include
    private String lastname;

    /** Password of the user. */
    private String password;

    /** Flag to indicate if the password is temporary. */
    private boolean temporaryPassword;

    /** Collection of roles to be assigned to the user. */
    @Builder.Default
    private Collection<AssignRole> roles = new HashSet<>();

    /**
     * Checks if the user has roles to be assigned.
     *
     * @return {@code true} if the user has roles to be assigned, {@code false} otherwise.
     */
    public boolean hasRoles() {
        return !roles.isEmpty();
    }

    /**
     * Returns the names of the roles that need to be assigned to the new user being created.
     *
     * @return Collection of role names to assign.
     */
    public List<String> roleNamesToAssign() {
        return roles.stream()
                .map(AssignRole::getRoleName)
                .collect(Collectors.toList());
    }

    /**
     * An experience model (DTO) to capture the details of the role that needs to be assigned to the new user being
     * created.
     *
     */
    @ToString(onlyExplicitlyIncluded = true)
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class AssignRole {
        /** Name of the role to be assigned to the new user. */
        private String roleName;
    }
}
