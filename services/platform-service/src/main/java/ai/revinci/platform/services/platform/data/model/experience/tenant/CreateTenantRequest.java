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

package ai.revinci.platform.services.platform.data.model.experience.tenant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collection;

import org.springframework.util.CollectionUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.common.tenant.policy.TenantPasswordPolicy;
import ai.revinci.platform.common.tenant.policy.UserRegistrationPolicy;
import ai.revinci.platform.common.xss.ContentType;
import ai.revinci.platform.common.xss.XssProtect;
import ai.revinci.platform.services.platform.enums.TenantCategory;
import ai.revinci.platform.services.platform.enums.TenantType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@XssProtect
@NoArgsConstructor
public class CreateTenantRequest {
    /** A realm-name for the new tenant, which happens to be unique within the hear@ai platform. */
    @ToString.Include
    @EqualsAndHashCode.Include
    @XssProtect(ContentType.PLAIN_TEXT)
    @NotNull(message = "{CreateTenantRequest.realmName.notnull}")
    @Size(min = 2, max = 255, message = "{CreateTenantRequest.realmName.size}")
    private String realmName;

    /** Name of the tenant. */
    @ToString.Include
    @XssProtect(ContentType.PLAIN_TEXT)
    private String name;

    /** Brief description about the tenant. */
    @XssProtect(ContentType.PLAIN_TEXT)
    private String description;

    /** Address of the tenant. */
    @XssProtect(ContentType.PLAIN_TEXT)
    private String address;

    /** Type of the tenant. */
    private TenantType type;

    /** Tenant category i.e., small, medium or enterprise. */
    private TenantCategory category;

    /** Contacts associated with the tenant. */
    private Collection<AddTenantContact> contacts;

    /** Password policy for the tenant. */
    private TenantPasswordPolicy passwordPolicy;

    /** User registration policy for the tenant. */
    private UserRegistrationPolicy userRegistrationPolicy;

}
