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

package ai.revinci.platform.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.web.configuration.properties.CORSProperties;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class PlatformDefaultCORSFilter implements Filter {
    private final CORSProperties corsProperties;
    public PlatformDefaultCORSFilter(final CORSProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        // Any initializations can be handled here.
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final CORSProperties.CorsHeaders corsHeaders = corsProperties.getCors()
                .getHeaders();

        // Does the origin header contain the allowed origins?
        final String originHeader = httpRequest.getHeader(HttpHeaders.ORIGIN);

        // TODO: Need to add the custom logic here.
        if (true) {
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, originHeader);
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                                   String.join(Token.COMMA.value(), corsHeaders.getAllowedMethods()));
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, String.valueOf(corsHeaders.getMaxAge()));
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                                   String.valueOf(corsHeaders.isAllowCredentials()));
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                                   String.join(Token.COMMA.value(), corsHeaders.getAllowedHeaders()));
        }

        if (httpRequest.getMethod()
                .equals(HttpMethod.OPTIONS.name())) {
            httpResponse.setStatus(HttpStatus.NO_CONTENT.value());
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // Any cleanup activities can be done here.
    }
}
