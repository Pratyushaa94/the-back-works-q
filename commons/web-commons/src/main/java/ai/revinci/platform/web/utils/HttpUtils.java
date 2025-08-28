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

package ai.revinci.platform.web.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;

public final class HttpUtils {
    /** Unknown ip address. */
    private static final String UNKNOWN = "unknown";

    /** Order of headers to be looked into for extracting the IP address. */
    // @formatter:off
    private static final String[] IP_ADDRESS_LOOKUP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };
    // @formatter:on

    /** Value template for Content-Disposition header. */
    private static final String CONTENT_DISPOSITION_HEADER_VALUE = "attachment; filename={0}";

    /**
     * Private constructor.
     */
    private HttpUtils() {
        throw new IllegalStateException("Cannot create instances of this class");
    }

    /**
     * This method attempts to retrieve the current Http request from the {@link RequestContextHolder}.
     *
     * @return An {@link Optional} instance wrapping the current Http request. If there is no request, an empty
     *         {@link Optional} is returned.
     */
    public static Optional<HttpServletRequest> getCurrentHttpRequest() {
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes sra) {
            return Optional.of(sra.getRequest());
        }
        return Optional.empty();
    }

    /**
     * This method attempts to return the ip address of the client by looking for specific headers in the provided
     * request.
     *
     * @param request Http request of type {@link HttpServletRequest}.
     *
     * @return IP address of the caller.
     */
    public static String getClientIpAddress(final HttpServletRequest request) {
        String ipAddress = StringUtils.EMPTY;

        if (Objects.isNull(request)) {
            return ipAddress;
        }

        for (final String header : HttpUtils.IP_ADDRESS_LOOKUP_HEADERS) {
            ipAddress = request.getHeader(header);
            if (StringUtils.isNotBlank(ipAddress) && !ipAddress.equalsIgnoreCase(HttpUtils.UNKNOWN)) {
                break;
            }
        }
        return StringUtils.isBlank(ipAddress) ?
                request.getRemoteAddr() :
                ipAddress;
    }

    /**
     * This method extracts the referer details from the provided {@code request} object.
     *
     * @param request Instance of type {@link HttpServletRequest}.
     *
     * @return Referer details.
     */
    public static String getReferer(final HttpServletRequest request) {
        final String referer = request.getHeader(HttpHeaders.REFERER);
        return StringUtils.isBlank(referer) ?
                PlatformDefaults.UNKNOWN_REFERER.value() :
                referer;
    }

    /**
     * This method invokes an API request using the provided {@code restTemplate} instance. The request is made to the
     * provided {@code url} using the provided {@code method} and {@code headers}.
     *
     * @param restTemplate Instance of type {@link RestTemplate}.
     * @param url          URL to which the request needs to be made.
     * @param method       HTTP method to be used for the request.
     * @param headers      Headers to be included in the request.
     *
     * @return Response from the API.
     */
    public static Map<String, Object> invokeApiRequest(@NonNull final RestTemplate restTemplate,
                                                       @NonNull final String url, @NonNull final HttpMethod method,
                                                       @NonNull final Map<String, String> headers) {
        // Set the headers.
        final HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        // Create the request entity.
        final HttpEntity<Object> requestEntity = new HttpEntity<>(httpHeaders);
        // Make the request.
        final ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, method, requestEntity,
                                                                                   new ParameterizedTypeReference<>() {
                                                                                   });
        if (!response.getStatusCode()
                .is2xxSuccessful()) {
            throw ServiceException.of(CommonErrors.API_REQUEST_FAILED, response.getStatusCode()
                    .value());
        }
        // Return the response body.
        return response.getBody();
    }

    /**
     * This method creates an instance of type {@link HttpHeaders} with basic initializations and returns it.
     *
     * @return Instance of type {@link HttpHeaders}.
     */
    public static HttpHeaders generateHeadersForFileDownload(final String fileName) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");
        if (StringUtils.isNotBlank(fileName)) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                        MessageFormat.format(CONTENT_DISPOSITION_HEADER_VALUE, fileName));
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        }

        return headers;
    }

    /**
     * From the provided request object, this method attempts to retrieve the "Authorization" header. If the value is a
     * Bearer token, the actual token (i.e. whatever comes after "Bearer ") is extracted and returned.
     *
     * @param request Request object of type {@link HttpServletRequest}.
     *
     * @return Bearer Token if present else returns empty string.
     */
    public static String extractBearerTokenFromAuthorizationHeader(final HttpServletRequest request) {
        String tokenValue = StringUtils.EMPTY;
        // Get the Authorization header from the request.
        final String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String bearerPattern = PlatformDefaults.BEARER.value()
                .concat(Token.COLON.value());
        if (StringUtils.isNotBlank(token) && token.startsWith(bearerPattern)) {
            tokenValue = token.replace(bearerPattern, StringUtils.EMPTY)
                    .trim();
        }
        return tokenValue;
    }
}
