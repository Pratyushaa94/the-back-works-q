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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.services.iam.keycloak.sync.enums.OperationType;
import ai.revinci.platform.services.iam.keycloak.sync.enums.ResourceType;
import com.fasterxml.jackson.annotation.JsonIgnore;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractEvent<T> {
    /** The operation that this event represents. For example: CREATE, UPDATE, DELETE. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private OperationType operation;

    /** The type of the resource that this event represents. For example: REALM_ROLE, USER, REALM_ROLE_MAPPING. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private ResourceType resourceType;

    /** Name of the realm. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String realm;

    /** Keycloak's auth server url. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String authServerUrl;

    /** Event data. */
    @ToString.Include
    @EqualsAndHashCode.Include
    private T data;

    /**
     * This method returns a boolean indicating if the {@code operation} represents a {@code DELETE} operation.
     *
     * @return {@code true} if the operation is {@code DELETE}, {@code false} otherwise.
     */
    @JsonIgnore
    public boolean isDeleteOperation() {
        return OperationType.DELETE.equals(operation);
    }
}
