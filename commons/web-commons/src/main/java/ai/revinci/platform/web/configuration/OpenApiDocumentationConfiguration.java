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

package ai.revinci.platform.web.configuration;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.web.configuration.properties.OpenApiDocumentationSettings;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@EnableConfigurationProperties(value = {OpenApiDocumentationSettings.class})
@Configuration
public class OpenApiDocumentationConfiguration {

    /** API documentation properties. */
    private final OpenApiDocumentationSettings openApiDocumentationSettings;

    public OpenApiDocumentationConfiguration(final OpenApiDocumentationSettings openApiDocumentationSettings) {
        this.openApiDocumentationSettings = openApiDocumentationSettings;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final OpenApiDocumentationSettings.Contact contact = openApiDocumentationSettings.getContact();

        // API Information
        final Info info = new Info().title(openApiDocumentationSettings.getTitle())
                .version(openApiDocumentationSettings.getVersion())
                .contact(new Contact().name(contact.getName())
                                 .email(contact.getEmail())
                                 .url(contact.getUrl()))
                .license(new License().name(openApiDocumentationSettings.getLicense())
                                 .url(openApiDocumentationSettings.getLicenseUrl()));

        // Security scheme.
        final OpenApiDocumentationSettings.ApiSecurityScheme apiSecurityScheme =
                openApiDocumentationSettings.getSecurityScheme();
        final String securitySchemeName = apiSecurityScheme.getName();

        final SecurityScheme oasSecurityScheme = new SecurityScheme().name(securitySchemeName)
                .type(SecurityScheme.Type.valueOf(
                        apiSecurityScheme.getType()))
                .scheme(apiSecurityScheme.getScheme())
                .bearerFormat(apiSecurityScheme.getBearerFormat());
        // Return the configuration object.
        return new OpenAPI().addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName, oasSecurityScheme))
                .servers(openApiDocumentationSettings.getServers()
                                 .stream()
                                 .map(s -> new Server().url(s.getUrl())
                                         .description(s.getDescription()))
                                 .toList())
                .info(info);
    }

    @Bean
    public OpenApiCustomizer customGlobalHeaders() {
        final String header = PlatformDefaults.HEADER.value();
        final String tidHeader = PlatformDefaults.TENANT_HEADER.value();
        final String tidDesc = PlatformDefaults.TENANT_HEADER_DESCRIPTION.value();
        return openApi -> openApi.getPaths()
                .forEach((p, pi) -> pi.readOperations()
                        .forEach(op -> {
                            op.addParametersItem(new Parameter().in(header)
                                                         .schema(new StringSchema())
                                                         .name(tidHeader)
                                                         .description(tidDesc)
                                                         .required(true));
                        }));
    }
}
