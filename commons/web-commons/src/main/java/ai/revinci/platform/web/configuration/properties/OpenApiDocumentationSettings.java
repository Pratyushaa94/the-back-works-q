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

package ai.revinci.platform.web.configuration.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "revinciai.platform.api.documentation")
public class OpenApiDocumentationSettings {
    /** Reference to the title. */
    private String title;

    /** Brief description about the API. */
    private String description;

    /** Security scheme for our APIs. */
    private ApiSecurityScheme securityScheme = new ApiSecurityScheme();

    /** Type of license for this software. */
    private String license;

    /** License url. */
    private String licenseUrl;

    /** Terms of service url. */
    private String termsOfServiceUrl;

    /** Version number. */
    private String version;

    /** Contact information. */
    private Contact contact = new Contact();

    /** Base package. */
    private String basePackage;

    /** List of servers. */
    private List<Server> servers = new ArrayList<>();

    /**
     * Contact details for the API.
     */
    @Data
    public static class Contact {
        /** Contact name. */
        private String name;

        /** Contact url. */
        private String url;

        /** Email address of the contact. */
        private String email;
    }

    /**
     * Security scheme for the exposed APIs.
     */
    @Data
    public static class ApiSecurityScheme {
        /** Default security scheme name. */
        public static final String DEFAULT_SECURITY_SCHEME_NAME = "bearerAuth";

        /** Security scheme name. */
        private String name = ApiSecurityScheme.DEFAULT_SECURITY_SCHEME_NAME;

        /** Security scheme. */
        private String scheme = "bearer";

        /** Security scheme type - HTTP, OAuth2, etc. */
        private String type = "HTTP";

        /** Bearer format. */
        private String bearerFormat = "JWT";
    }

    /**
     * Represents the server that exposes the APIs.
     */
    @Data
    public static class Server {
        /** Server url. */
        private String url;

        /** Server description. */
        private String description;
    }
}
