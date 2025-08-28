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

package ai.revinci.platform.services.tenant.api;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.security.data.model.experience.UserProfile;
import ai.revinci.platform.security.service.TokenRevocationService;
import ai.revinci.platform.services.tenant.service.TenantUserService;
import ai.revinci.platform.web.api.AbstractApi;
import ai.revinci.platform.web.configuration.properties.OpenApiDocumentationSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(UserProfileApi.ROOT_ENDPOINT)
@RequiredArgsConstructor
public class UserProfileApi extends AbstractApi {
    /** Root endpoint for business dimensions API. */
    public static final String ROOT_ENDPOINT = "/api/v1/users";

    /** Tag under which the swagger APIs will be grouped under. */
    public static final String API_TAG = "Users";

    /** A service implementation of type {@link TenantUserService}. */
    private final TenantUserService tenantUserService;

    /** A service implementation of type {@link TokenRevocationService}. */
    private final TokenRevocationService tokenRevocationService;

    @Operation(method = "getUserProfile",
               summary = "Retrieve the user profile details.",
               description = "This API retrieves the user profile details of the current logged-in user.",
               tags = {
                       UserProfileApi.API_TAG
               },
               security = {
                       @SecurityRequirement(name = OpenApiDocumentationSettings.ApiSecurityScheme.DEFAULT_SECURITY_SCHEME_NAME)
               }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200",
                                 description = "Successfully retrieved the user profile details of the current logged-in user.",
                                 content = @Content),
                    @ApiResponse(responseCode = "401",
                                 description = "You need to authenticate to perform this operation.",
                                 content = @Content),
                    @ApiResponse(responseCode = "403",
                                 description = "You do not have permissions to perform this operation.",
                                 content = @Content)
            })
    // @formatter:on
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getUserProfile() {
        // Delegate to the service layer.
        final UserProfile userProfile = tenantUserService.getUserProfile();

        return ResponseEntity.ok(userProfile);
    }

    // @formatter:off
    @Operation(method = "logout",
               summary = "Logout the current logged-in user.",
               description = "This API performs a logout of the current logged-in user.",
               tags = {
                       UserProfileApi.API_TAG
               },
               security = {
                       @SecurityRequirement(name = OpenApiDocumentationSettings.ApiSecurityScheme.DEFAULT_SECURITY_SCHEME_NAME)
               }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200",
                                 description = "Current logged-in user has been logged out successfully.",
                                 content = @Content),
                    @ApiResponse(responseCode = "401",
                                 description = "You need to authenticate to perform this operation.",
                                 content = @Content),
                    @ApiResponse(responseCode = "403",
                                 description = "You do not have permissions to perform this operation.",
                                 content = @Content)
            })
    // @formatter:on
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") final String authorizationHeader) {
        // Extract the bearer token value.
        final String token = authorizationHeader.replace(PlatformDefaults.BEARER.value(), StringUtils.EMPTY)
                .trim();

        // Mark this token as invalid.
        tokenRevocationService.revoke(token);

        return ResponseEntity.ok()
                .build();
    }
}
