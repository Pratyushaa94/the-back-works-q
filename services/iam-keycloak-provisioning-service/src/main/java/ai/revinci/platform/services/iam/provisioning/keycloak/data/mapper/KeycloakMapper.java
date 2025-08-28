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

package ai.revinci.platform.services.iam.provisioning.keycloak.data.mapper;

import java.util.Collection;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import ai.revinci.platform.provisioning.iam.data.model.experience.OnboardRealmRequest;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.mapper.decorator.KeycloakMapperDecorator;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.model.experience.AddRealmUserRequest;
import ai.revinci.platform.services.iam.provisioning.keycloak.data.model.experience.AddRoleRequest;

@DecoratedWith(KeycloakMapperDecorator.class)
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface KeycloakMapper {
    /**
     * Transforms an instance of type {@link AddRealmUserRequest} to an instance of type {@link UserRepresentation}.
     *
     * @param source Instance of type {@link AddRealmUserRequest} that needs to be transformed to an instance of type
     *               {@link UserRepresentation}.
     *
     * @return Transformed instance of type {@link UserRepresentation}.
     */
    @Mapping(source = "firstname", target = "firstName")
    @Mapping(source = "lastname", target = "lastName")
    UserRepresentation transform(AddRealmUserRequest source);

    /**
     * Transforms an instance of type {@link OnboardRealmRequest.RealmRole} to an instance of type
     * {@link AddRoleRequest}.
     *
     * @param source Instance of type {@link OnboardRealmRequest.RealmRole} that needs to be transformed to an instance
     *               of type {@link AddRoleRequest}.
     *
     * @return Transformed instance of type {@link AddRoleRequest}.
     */
    AddRoleRequest transform(OnboardRealmRequest.RealmRole source);

    /**
     * Transforms an instance of type {@link OnboardRealmRequest.RealmUser} to an instance of type
     * {@link AddRealmUserRequest}.
     *
     * @param source Instance of type {@link OnboardRealmRequest.RealmUser} that needs to be transformed to an instance
     *               of type {@link AddRealmUserRequest}.
     *
     * @return Transformed instance of type {@link AddRealmUserRequest}.
     */
    AddRealmUserRequest transform(OnboardRealmRequest.RealmUser source);

    /**
     * This method converts / transforms the provided collection of {@link OnboardRealmRequest.RealmRole} instances to a
     * collection of instances of type {@link AddRoleRequest}.
     *
     * @param source Instances of type {@link OnboardRealmRequest.RealmRole} that needs to be transformed to
     *               {@link AddRoleRequest}.
     *
     * @return Collection of instances of type {@link AddRoleRequest}.
     */
    default Collection<AddRoleRequest> transformRoles(Collection<OnboardRealmRequest.RealmRole> source) {
        return source.stream()
                .map(this::transform)
                .collect(Collectors.toSet());
    }

    /**
     * This method converts / transforms the provided collection of {@link OnboardRealmRequest.RealmUser} instances to a
     * collection of instances of type {@link AddRealmUserRequest}.
     *
     * @param source Instances of type {@link OnboardRealmRequest.RealmUser} that needs to be transformed to
     *               {@link AddRealmUserRequest}.
     *
     * @return Collection of instances of type {@link AddRealmUserRequest}.
     */
    default Collection<AddRealmUserRequest> transformUsers(Collection<OnboardRealmRequest.RealmUser> source) {
        return source.stream()
                .map(this::transform)
                .collect(Collectors.toSet());
    }
}
