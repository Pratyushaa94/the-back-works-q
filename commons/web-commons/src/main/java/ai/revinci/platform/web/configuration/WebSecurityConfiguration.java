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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.security.access.UserPermissionPolicyEvaluator;
import ai.revinci.platform.security.token.PlatformJwtAuthenticationConverter;

@Slf4j
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
@Configuration
public class WebSecurityConfiguration {
    private final JwtDecoder jwtDecoder;
    private final PlatformJwtAuthenticationConverter platformJwtAuthenticationConverter;

    private final UserPermissionPolicyEvaluator userPermissionPolicyEvaluator;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        // @formatter:off
        return http
                // Disable CSRF if you're building a stateless REST API
                .csrf(AbstractHttpConfigurer::disable)
                // Session Management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Security Headers
                .headers(this::configureHeaders)
                .authorizeHttpRequests(authz -> authz.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health/**", "/api/v1/public/**").permitAll()
                        .requestMatchers("/actuator/metrics/**").hasAnyRole("super_admin")
                        .requestMatchers("/api/v1/**").authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.decoder(jwtDecoder)
                        .jwtAuthenticationConverter(platformJwtAuthenticationConverter)))
                .build();
        // @formatter:on
    }

    @Bean
    public MethodSecurityExpressionHandler createExpressionHandler() {
        final DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(userPermissionPolicyEvaluator);

        return expressionHandler;
    }

    private void configureHeaders(final HeadersConfigurer<?> headers) {
        // @formatter:off
        headers.cacheControl(HeadersConfigurer.CacheControlConfig::disable)
                .contentTypeOptions(contentTypeOptions -> {
                })
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; " +
                                                                           "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                                                           "style-src 'self' 'unsafe-inline'; " +
                                                                           "img-src 'self' data: https:; " +
                                                                           "font-src 'self'; " +
                                                                           "frame-ancestors 'none'; " +
                                                                           "form-action 'self'"))
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(31_536_000))
                .permissionsPolicyHeader(permissions -> permissions.policy(" geolocation=(*), payment=()"));
        // @formatter:on
    }
}
