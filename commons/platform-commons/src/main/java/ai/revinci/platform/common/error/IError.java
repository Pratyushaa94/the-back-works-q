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

import java.text.MessageFormat;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Base interface to define the contracts for retrieving the messages based on their error codes.
 * <p>
 * This interface is typically implemented by the enumerated data types that provide messages based on unique codes. The
 * process is as below:
 *
 * <p>A message code needs to be added in the enumerated data type that implements {@link IError} interface.</p>
 * <p>Against the message code, a new entry is added to the resource bundle (e.g. error_messages.properties)</p>
 * <p>Every message in the resource bundle takes the form: [Message Code].[Message Code Suffix]=[Message]</p>
 * <p>Example:</p>
 * <p>USER_NOT_FOUND=Unable to find an user with username {0}.</p>
 * <pre>
 * public interface ApplicationErrors implements IError {
 *     GENERIC_ERROR,
 *     USER_NOT_FOUND;
 *     ....
 * }
 * </pre>
 * <p>Usage:</p>
 * <pre>
 * public class UsageExample {
 *     public void performSomething() {
 *          if(conditionFailed) {
 *              throw ServiceException.instance(ApplicationErrors.GENERIC_ERROR);
 *          }
 *     }
 *     public void performSomeOtherThing(userName) {
 *          if(conditionFailed) {
 *              throw ServiceException.instance(ApplicationErrors.USER_NOT_FOUND, userName);
 *          }
 *     }
 * }
 * </pre>
 *
 * @author Subbu
 */
public interface IError {
    /**
     * This method returns an instance of type {@link IErrorMessageProvider}, which holds all the error messages.
     *
     * @return Instance of type {@link IErrorMessageProvider} and cannot be null.
     */
    IErrorMessageProvider errorMessageProvider();

    /**
     * Returns the error code that are typically enum constants (for example: USER_NOT_FOUND).
     *
     * @return Error code.
     */
    String name();

    /**
     * Returns a localized (default locale) message for the error code associated with this instance.
     *
     * @return Localized message for the error code associated with this instance. Returns null if there is no message
     *         associated.
     */
    default String message() {
        return message(locale());
    }

    /**
     * Returns a localized message (for the provided locale) for the error code associated with this instance.
     *
     * @param locale Locale.
     *
     * @return Localized message (for the specified locale) for the error code associated with this instance. Returns
     *         null if there is no message associated.
     */
    default String message(final Locale locale) {
        return errorMessageProvider().message(this, locale);
    }

    /**
     * Returns a formatted message for the error code associated with this instance and for the default locale.
     *
     * @param messageArguments Arguments that will be used to replace the placeholders in the message string.
     *
     * @return Formatted message for the error code associated with this instance and for the default locale.
     */
    default String formattedMessage(final Object... messageArguments) {
        return formattedMessage(locale(), messageArguments);
    }

    /**
     * Returns a formatted message for the error code associated with this instance and for the provided locale.
     *
     * @param locale           Locale.
     * @param messageArguments Arguments that will be used to replace the placeholders in the error message string.
     *
     * @return Formatted message for the error code associated with this instance and for the provided locale.
     */
    default String formattedMessage(final Locale locale, final Object... messageArguments) {
        return errorMessageProvider().formattedMessage(this, locale, messageArguments);
    }

    /**
     * Returns the display message based on the code and the error message. The format of the display message is
     * determined by DISPLAY_STRING_FORMAT property on {@link IError}.
     *
     * @return Display message.
     */
    default String displayString() {
        return MessageFormat.format("{0} [{1}]", name(), message());
    }

    /**
     * Returns the default locale.
     *
     * @return Default locale of type {@link Locale}.
     */
    default Locale locale() {
        return LocaleContextHolder.getLocale();
    }
}
