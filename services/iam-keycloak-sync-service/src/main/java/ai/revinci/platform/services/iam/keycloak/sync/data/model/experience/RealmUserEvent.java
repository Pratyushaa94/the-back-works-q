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

package ai.revinci.platform.services.iam.keycloak.sync.data.model.experience;

import java.util.List;

import org.springframework.util.CollectionUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class RealmUserEvent extends AbstractEvent<RealmUserEvent.Data> {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString(onlyExplicitlyIncluded = true)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class Data {
        /** Unique identifier of the user. */
        @ToString.Include
        @EqualsAndHashCode.Include
        private String id;

        /** Username of the user. */
        @EqualsAndHashCode.Include
        private String username;

        /** First name of the user. */
        private String firstName;

        /** Last name of the user. */
        private String lastName;

        /** Email address of the user. */
        private String email;

        /** Boolean indicating if the user is enabled. */
        private boolean enabled;

        /** Boolean indicating if the user's email is verified. */
        private boolean emailVerified;

        /** List of applications assigned to this user. */
        private List<String> applications;

        /**
         * This method checks if this user has any application keys assigned.
         *
         * @return {@code true} if the user has applications assigned, else {@code false}.
         */
        public boolean hasApplications() {
            return !CollectionUtils.isEmpty(applications);
        }
    }
}
