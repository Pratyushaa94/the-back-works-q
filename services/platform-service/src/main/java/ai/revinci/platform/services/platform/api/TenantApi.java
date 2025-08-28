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

package ai.revinci.platform.services.platform.api;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import ai.revinci.platform.services.platform.data.model.experience.tenant.BasicTenantDetails;
import ai.revinci.platform.services.platform.data.model.experience.tenant.CreateTenantRequest;
import ai.revinci.platform.services.platform.data.model.experience.tenant.Tenant;
import ai.revinci.platform.services.platform.service.TenantService;
import ai.revinci.platform.web.api.AbstractApi;
import ai.revinci.platform.web.configuration.properties.OpenApiDocumentationSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(TenantApi.ROOT_ENDPOINT)
@RequiredArgsConstructor
public class TenantApi extends AbstractApi {
    public static final String API_TAG = "Tenants";

    public static final String ROOT_ENDPOINT = "/api/v1/tenants";

    private final TenantService tenantService;

    @Operation(method = "onboardTenant",
               summary = "Onboard a new tenant in the system.",
               description = "This API is used to onboard a new tenant in the system.",
               tags = {TenantApi.API_TAG},
               security = {
                       @SecurityRequirement(name =
                               OpenApiDocumentationSettings.ApiSecurityScheme.DEFAULT_SECURITY_SCHEME_NAME)
               })
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "201",
                                 description = "Successfully created a new tenant in the system.",
                                 content = @Content),
                    @ApiResponse(responseCode = "403",
                                 description = "You do not have permissions to perform this operation.",
                                 content = @Content)
            })
    // @formatter:on
    @PreAuthorize("hasRole('ROLE_super_admin')")
    @PostMapping
    public ResponseEntity<BasicTenantDetails> createTenant(@Valid @RequestBody final CreateTenantRequest payload) {
        // Delegate to the service layer.
        final BasicTenantDetails newTenant = tenantService.create(payload);

        // Build a response entity object and return it.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(newTenant);
    }


    @Operation(method = "fetchAllTenants",
               summary = "Fetch All tenants in the system.",
               description = "This API is used to fetch the list of tenants in the system.",
               tags = {TenantApi.API_TAG},
               security = {
                       @SecurityRequirement(name =
                               OpenApiDocumentationSettings.ApiSecurityScheme.DEFAULT_SECURITY_SCHEME_NAME)
               })
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200",
                                 description = "Successfully retrieved the list of tenants.",
                                 content = @Content),
                    @ApiResponse(responseCode = "403",
                                 description = "You do not have permissions to perform this operation.",
                                 content = @Content)
            })
    // @formatter:on
    @PreAuthorize("hasRole('ROLE_super_admin')")
    @GetMapping
    public ResponseEntity<List<Tenant>> fetchAllTenants() {
        // Delegate to the service layer.
        final List<Tenant> tenants = tenantService.fetchAllTenants();
        // Build a response entity object and return it.
        return ResponseEntity.status(HttpStatus.OK)
                .body(tenants);
    }
}
