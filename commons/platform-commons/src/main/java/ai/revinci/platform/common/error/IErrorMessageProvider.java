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
package ai.revinci.platform.common.error;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Base contract that defines the methods required to read the localized messages from the resource bundle.
 * <p>
 * See {@link ErrorMessageProvider} for the default implementation of this contract.
 *
 * @author Subbu
 */
public interface IErrorMessageProvider {
    /**
     * Returns the error message registered against the error-code using the provided {@code error} and {@code locale}.
     * If there is no message registered for the specific error code, the same error-code shall be returned.
     * <p>
     * The lookup key will be generated based on the provided error code.
     *
     * @param error  Error object against which the error message needs to be retrieved.
     * @param locale Locale.
     *
     * @return Error message for the error code wrapped within the specified error object and for the specified locale.
     */
    default String message(final IError error, final Locale locale) {
        return message(error.name(), locale);
    }

    /**
     * Returns the error message registered against the specified error-code and for the specified locale. If there is
     * no message registered for the specific error code, this method will return the error-code.
     * <p>
     * The lookup key will be generated based on the provided error code.
     *
     * @param errorCode Error code against which the message needs to be retrieved.
     * @param locale    Locale.
     *
     * @return Error message for the error code wrapped within the specified error object and for the specified locale.
     */
    String message(String errorCode, Locale locale);

    /**
     * Returns a formatted error message registered against the error code wrapped within the provided error object in
     * the resource bundle for the specified locale. If there is no message registered for the respective error code,
     * this method will return the error-code.
     * <p>
     * The lookup key will be generated based on the provided error code.
     *
     * @param error       Error object against which the error message needs to be retrieved.
     * @param locale      Locale.
     * @param messageArgs Arguments for the placeholders in the error message.
     *
     * @return Formatted error message for the provided error and locale combination.
     */
    default String formattedMessage(final IError error, final Locale locale, final Object... messageArgs) {
        return formattedMessage(error.name(), locale, messageArgs);
    }

    /**
     * Returns a formatted error message registered against the error code wrapped within the provided error object in
     * the resource bundle for the specified locale. If there is no message registered for the respective error code,
     * this method will return the error-code.
     * <p>
     * The lookup key will be generated based on the provided error code.
     *
     * @param errorCode   Error code for which the message needs to be retrieved (see {@link IError}).
     * @param locale      Locale.
     * @param messageArgs Arguments for the placeholders in the error message.
     *
     * @return Formatted error message for the provided error and locale combination.
     */
    String formattedMessage(String errorCode, Locale locale, Object... messageArgs);

    /**
     * Returns the default locale.
     *
     * @return Default locale of type {@link Locale}.
     */
    default Locale locale() {
        return LocaleContextHolder.getLocale();
    }
}