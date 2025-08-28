/*
 * Copyright (c) 2025 Revinci AI.
 *
 * All rights reserved. This software is proprietary to and embodies the
 * confidential technology of Revinci AI. Possession,
 * use, duplication, or dissemination of the software and media is
 * authorized only pursuant to a valid written license from
 * Revinci AI Solutions Pvt. Ltd.
 *
 * Unauthorized use of this software is strictly prohibited.
 *
 * THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Revinci AI BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ai.revinci.platform.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import lombok.extern.slf4j.Slf4j;
import ai.revinci.platform.common.xss.ContentType;
import ai.revinci.platform.common.xss.XssProtect;

/**
 * A utility class that provides the functionality of sanitizing input data, which can be of type {@link String}.
 * <p>
 * Generally, this is used to address use-cases where the input data is not sanitized and can be used to perform
 * malicious activities like SQL injection, XSS attacks, etc.
 *
 * @author Subbu
 */
@Slf4j
public final class XssSanitizer {
    // For sample policy examples, refer to the below link:
    // https://github.com/OWASP/java-html-sanitizer/blob/master/src/main/java/org/owasp/html/examples/EbayPolicyExample.java

    /** Pattern for email. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /** Pattern for phone. */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$");

    /** The policy to be used for sanitizing the input data. */
    // @formatter:off
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowStandardUrlProtocols()
            .allowElements("a", "label", "noscript", "h1", "h2", "h3", "h4", "h5", "h6", "p", "i", "b",
                           "u", "strong", "em", "small", "big", "pre", "code", "cite", "samp", "sub", "sup", "strike",
                           "center", "blockquote", "hr", "br", "col", "font", "map", "span", "div", "img", "ul", "ol",
                           "li", "dd", "dt", "dl", "tbody", "thead", "tfoot", "table", "td", "th", "tr", "colgroup",
                           "fieldset", "legend")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src", "alt").onElements("img")
            .allowTextIn("a")
            .toFactory();
    // @formatter:on

    /**
     * Private constructor.
     */
    private XssSanitizer() {
        throw new IllegalStateException("Cannot create instances of this class");
    }

    /**
     * This method sanitizes the provided {@code input} using the default content type as
     * {@link ContentType#PLAIN_TEXT}.
     *
     * @param input Input to sanitize.
     * @param <T>   Type of the input.
     *
     * @return Sanitized input.
     */
    public static <T> T sanitize(final T input) {
        return XssSanitizer.sanitize(input, ContentType.PLAIN_TEXT);
    }

    /**
     * This method sanitizes the provided {@code input} based on the provided {@code defaultContentType}.
     * <p>
     * The {@code defaultContentType} is used when the content type is not provided for the input.
     *
     * @param input              Input to sanitize.
     * @param defaultContentType Default content type to use for sanitization.
     * @param <T>                Type of the input.
     *
     * @return Sanitized input.
     */
    public static <T> T sanitize(final T input, final ContentType defaultContentType) {
        try {
            return XssSanitizer.sanitizeOrThrow(input, defaultContentType);
        } catch (final IllegalAccessException e) {
            XssSanitizer.LOGGER.error("Failed to sanitize. Error: {}", e.getMessage(), e);
            return input;
        }
    }

    /**
     * This method sanitizes the provided {@code input} using the default content type as
     * {@link ContentType#PLAIN_TEXT}.
     *
     * @param input Input to sanitize.
     * @param <T>   Type of the input.
     *
     * @return Sanitized input.
     *
     * @throws IllegalAccessException If the field is not accessible.
     */
    public static <T> T sanitizeOrThrow(final T input) throws IllegalAccessException {
        return XssSanitizer.sanitizeOrThrow(input, ContentType.PLAIN_TEXT);
    }

    /**
     * This method sanitizes the provided {@code input} based on the provided {@code defaultContentType}.
     * <p>
     * The {@code defaultContentType} is used when the content type is not provided for the input.
     *
     * @param input              Input to sanitize.
     * @param defaultContentType Default content type to use for sanitization.
     * @param <T>                Type of the input.
     *
     * @return Sanitized input.
     *
     * @throws IllegalAccessException If the field is not accessible.
     */
    @SuppressWarnings("unchecked")
    public static <T> T sanitizeOrThrow(final T input, final ContentType defaultContentType)
            throws IllegalAccessException {
        if (Objects.isNull(input)) {
            return null;
        }

        final Class<?> clazz = input.getClass();
        // 1. Is this a primitive type or a wrapper type or a UUID type, return as-is.
        if (clazz.isPrimitive() || XssSanitizer.isWrapperType(clazz) || XssSanitizer.skipType(clazz)) {
            return input;
        }

        // 2. Is this a string type?
        if (String.class.equals(clazz)) {
            return (T) XssSanitizer.sanitizeString((String) input, defaultContentType);
        }

        // 3. Handle collections
        if (input instanceof Collection<?> collection) {
            return (T) XssSanitizer.sanitizeCollection(collection);
        }

        // 4. Handle arrays
        if (clazz.isArray()) {
            return (T) XssSanitizer.sanitizeArray((Object[]) input);
        }

        // 5. Handle complex objects / reference types
        return XssSanitizer.sanitizeObject(input);
    }

    /**
     * This method sanitizes the provided {@code value} based on the provided {@code contentType}.
     *
     * @param value       Value to sanitize.
     * @param contentType Content type of the value and the rules for sanitization vary based on the content type.
     *
     * @return Sanitized value.
     */
    public static String sanitizeString(final String value, final ContentType contentType) {
        if (StringUtils.isBlank(value)) {
            return value;
        }

        return switch (contentType) {
            case EMAIL -> XssSanitizer.sanitizeEmail(value);
            case PHONE -> XssSanitizer.sanitizePhone(value);
            case HTML -> XssSanitizer.POLICY.sanitize(value);
            case URL -> XssSanitizer.sanitizeUrl(value);
            case HTML_ATTRIBUTE -> Encode.forHtmlAttribute(value);
            case JAVASCRIPT -> Encode.forJavaScript(value);
            case PLAIN_TEXT -> Encode.forHtmlContent(value);
        };
    }

    /**
     * This method sanitizes the provided {@code collection} by sanitizing each element in the collection.
     *
     * @param collection Collection to sanitize.
     *
     * @return Sanitized collection.
     */
    public static Collection<?> sanitizeCollection(final Collection<?> collection) {
        return collection.stream()
                         .map(XssSanitizer::sanitize)
                         .collect(Collectors.toList());
    }

    /**
     * This method sanitizes the provided {@code array} by sanitizing each element in the array.
     *
     * @param array Array to sanitize.
     *
     * @return Sanitized array.
     */
    public static Object[] sanitizeArray(final Object[] array) {
        return Arrays.stream(array)
                     .map(XssSanitizer::sanitize)
                     .toArray();
    }

    /**
     * This method checks if the provided {@code clazz} is a wrapper type.
     * <p>
     * Wrapper types include:
     * <ul>
     *     <li>{@link Number} and all classes that extends {@link Number}</li>
     *     <li>{@link Boolean}</li>
     *     <li>{@link Character}</li>
     * </ul>
     *
     * @param clazz Class to check.
     *
     * @return {@code true} if the class is a wrapper type, {@code false} otherwise.
     */
    private static boolean isWrapperType(final Class<?> clazz) {
        return Number.class.isAssignableFrom(clazz) || Boolean.class.equals(clazz) || Character.class.equals(clazz);
    }

    /**
     * This method checks if the provided {@code clazz} can be skipped for sanitization.
     * <p>
     * The list of types excluded by this method include:
     * <ul>
     *     <li>{@link Date}</li>
     *     <li>{@link Temporal}</li>
     *     <li>{@link UUID}</li>
     *     <li>{@link URL}</li>
     * </ul>
     *
     * @param clazz Class to check.
     *
     * @return {@code true} if the class can be skipped, {@code false} otherwise.
     */
    private static boolean skipType(final Class<?> clazz) {
        // @formatter:off
        return Date.class.isAssignableFrom(clazz) || Temporal.class.isAssignableFrom(clazz) ||
                UUID.class.equals(clazz) || URL.class.equals(clazz) || clazz.isEnum();
        // @formatter:on
    }

    /**
     * This method attempts to sanitize the provided {@code object} by recursively sanitizing the fields of the object.
     * <p>
     * All {@link String} fields in the object are sanitized.
     *
     * @param object Object whose fields need to be sanitized.
     * @param <T>    Type of the object.
     *
     * @return Sanitized object.
     *
     * @throws IllegalAccessException If the field is not accessible.
     */
    private static <T> T sanitizeObject(final T object) throws IllegalAccessException {
        final Class<?> clazz = object.getClass();
        for (final Field field : XssSanitizer.getAllFields(clazz)) {
            field.setAccessible(true);
            final Object value = field.get(object);
            if (Objects.isNull(value)) {
                continue;
            }

            if (value instanceof String strValue) {
                final XssProtect annotation = field.getAnnotation(XssProtect.class);
                final ContentType contentType = Objects.nonNull(annotation) ?
                                                annotation.value() :
                                                ContentType.PLAIN_TEXT;
                field.set(object, XssSanitizer.sanitizeString(strValue, contentType));
                XssSanitizer.LOGGER.trace("Sanitized field {} in type {}", field.getName(),
                                          clazz.getName());
            } else if (!XssSanitizer.skipType(value.getClass())) {
                field.set(object, XssSanitizer.sanitizeOrThrow(value));
            }
        }
        return object;
    }

    /**
     * This method retrieves all the fields of the provided {@code clazz} including the fields from the super classes.
     * <p>
     * This method ignores all the static fields.
     *
     * @param clazz Class whose fields need to be retrieved.
     *
     * @return List of fields.
     */
    private static List<Field> getAllFields(final Class<?> clazz) {
        final List<Field> fields = new ArrayList<>();
        Class<?> clazz2 = clazz;

        while (Objects.nonNull(clazz2)) {
            for (final Field field : clazz2.getDeclaredFields()) {
                // Ignore static fields.
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            clazz2 = clazz2.getSuperclass();
        }
        return fields;
    }

    /**
     * This method attempts to sanitize the provided {@code email} value.
     *
     * @param email Email value that needs to be sanitized.
     *
     * @return Sanitized email value.
     */
    private static String sanitizeEmail(final String email) {
        // Preserve valid email addresses
        return XssSanitizer.EMAIL_PATTERN.matcher(email)
                                         .matches() ?
               email :
               Encode.forHtmlContent(email);
    }

    /**
     * This method attempts to sanitize the provided {@code phone} value.
     *
     * @param phone Phone value that needs to be sanitized.
     *
     * @return Sanitized phone value.
     */
    private static String sanitizePhone(final String phone) {
        // Preserve valid phone numbers
        return XssSanitizer.PHONE_PATTERN.matcher(phone)
                                         .matches() ?
               phone :
               Encode.forHtmlContent(phone);
    }

    /**
     * This method attempts to sanitize the provided {@code value}, which happens to be a URL value.
     *
     * @param value URL value that needs to be sanitized.
     *
     * @return Sanitized URL value.
     */
    private static String sanitizeUrl(final String value) {
        try {
            // Validate URL format
            new URI(value).toURL();
            return Encode.forUriComponent(value);
        } catch (final MalformedURLException | URISyntaxException e) {
            return Encode.forHtmlContent(value);
        }
    }
}
