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
package ai.revinci.platform.common.exception;

import java.io.Serial;
import java.text.MessageFormat;
import java.util.Objects;

import ai.revinci.platform.common.error.IError;

/**
 * Default implementation of a service exception for this application.
 *
 * @author Subbu
 */
public class ServiceException extends RuntimeException {
    /** Serial Version Id. */
    @Serial
    private static final long serialVersionUID = -873876099009267527L;

    /** Display string for the service exception. {0} - error code; {1} - error message. */
    private static final String DISPLAY_STRING = "{0}: {1}";

    /** Reference to the error code wrapped by the exception. */
    private final String errorCode;

    /**
     * Constructor.
     *
     * @param error Instance of type {@link IError} that wraps the error code and error message. Uses the message via
     *              {@code message()} on {@link IError}.
     */
    public ServiceException(final IError error) {
        this(error.name(), error.message());
    }

    /**
     * Constructor.
     *
     * @param error     Instance of type {@link IError} that wraps the error code and error message. Uses the message
     *                  via {@code formattedMessage()} on {@link IError}.
     * @param arguments Arguments that will be used as placeholder values within the error message.
     */
    public ServiceException(final IError error, final Object... arguments) {
        this(error.name(), error.formattedMessage(arguments));
    }

    /**
     * Constructor.
     *
     * @param errorCode        Error code.
     * @param formattedMessage Formatted error message that will be set as the exception error message.
     */
    public ServiceException(final String errorCode, final String formattedMessage) {
        super(formattedMessage);
        this.errorCode = errorCode;
    }

    /**
     * Factory method to create an instance of {@link ServiceException} using the provided error of type
     * {@link IError}.
     *
     * @param error Instance of type {@link IError} that wraps the error code and error message. Uses the message via
     *              {@code message()} on {@link IError}.
     *
     * @return Service exception of type {@link ServiceException}.
     */
    public static ServiceException of(final IError error) {
        return new ServiceException(error);
    }

    /**
     * Factory method to create an instance of {@link ServiceException} using the provided error of type {@link IError}
     * and error message arguments.
     *
     * @param error     Instance of type {@link IError} that wraps the error code and error message. Uses the message
     *                  via {@code message()} on {@link IError}.
     * @param arguments Arguments that will be used as placeholder values within the error message.
     *
     * @return Service exception of type {@link ServiceException}.
     */
    public static ServiceException of(final IError error, final Object... arguments) {
        return new ServiceException(error, arguments);
    }

    /**
     * Factory method to create an instance of {@link ServiceException} using the provided error code and formatter
     * error message.
     *
     * @param errorCode        Error code.
     * @param formattedMessage Formatted error message that will be set as the exception error message.
     *
     * @return Service exception of type {@link ServiceException}.
     */
    public static ServiceException of(final String errorCode, final String formattedMessage) {
        return new ServiceException(errorCode, formattedMessage);
    }

    /**
     * This method returns the error code wrapped by this exception object.
     *
     * @return Error code.
     */
    public String errorCode() {
        return errorCode;
    }

    /**
     * This method returns a boolean indicating if the error code wrapped by the service exception matches the provided
     * error object of type {@link IError}.
     *
     * @param error Error object of type {@link IError}.
     *
     * @return True if the error code wrapped by this exception object matches the error code of the provided
     *         {@link IError} object.
     */
    public boolean is(final IError error) {
        if (Objects.isNull(error)) {
            return false;
        }
        return error.name()
                    .equalsIgnoreCase(errorCode);
    }

    @Override
    public String toString() {
        return MessageFormat.format(ServiceException.DISPLAY_STRING, errorCode, getMessage());
    }
}
