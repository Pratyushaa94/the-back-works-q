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

package ai.revinci.platform.web.api.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ResourceNotFoundException;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.web.api.error.ApiErrors;
import ai.revinci.platform.web.api.response.WebExceptionResponse;
import ai.revinci.platform.web.api.response.ConstraintValidationErrorsResponse;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@Component
public class GloabalApiExceptionHandler extends ResponseEntityExceptionHandler {
    /** Environment instance. */
    private final Environment env;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex,
                                                                  @NonNull final HttpHeaders headers,
                                                                  @NonNull final HttpStatusCode status,
                                                                  @NonNull final WebRequest request) {
        // Get the errors from the binding result.
        final List<ObjectError> errors = ex.getBindingResult()
                .getAllErrors();

        final Collection<ConstraintValidationErrorsResponse.FieldError> fieldErrors = new LinkedHashSet<>();
        // Get the exception messages
        if (errors.isEmpty()) {
            fieldErrors.add(ConstraintValidationErrorsResponse.FieldError.builder()
                                    .fieldName(
                                            CommonErrors.ILLEGAL_ARGUMENT.name())
                                    .errorMessage(ExceptionUtils.getMessage(ex))
                                    .build());
        } else {
            for (final ObjectError fieldError : errors) {
                final String propertyName = fieldError.getDefaultMessage();
                final String fieldName = fieldError instanceof FieldError fe ?
                        fe.getField() :
                        propertyName;
                if (Objects.nonNull(propertyName)) {
                    fieldErrors.add(ConstraintValidationErrorsResponse.FieldError.builder()
                                            .fieldName(fieldName)
                                            .errorMessage(env.getProperty(
                                                    propertyName))
                                            .build());
                }
            }
        }
        if (GloabalApiExceptionHandler.LOGGER.isErrorEnabled()) {
            GloabalApiExceptionHandler.LOGGER.error(fieldErrors.toString(), ex);
        }

        final ConstraintValidationErrorsResponse apiResponse = ConstraintValidationErrorsResponse.builder()
                .errorCode(
                        CommonErrors.ILLEGAL_ARGUMENT.name())
                .fieldErrors(
                        fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(apiResponse);
    }

    /**
     * Handles exceptions of type {@link ConstraintViolation}.
     *
     * @param ex      Constraint violation exception.
     * @param request Web request.
     *
     * @return Response entity containing the details of the exception.
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolation(final ConstraintViolationException ex,
                                                            final WebRequest request) {
        // Get the errors from the exception.
        final Collection<ConstraintViolation<?>> errors = ex.getConstraintViolations();
        final Collection<ConstraintValidationErrorsResponse.FieldError> fieldErrors = new LinkedHashSet<>();
        // Get the exception messages
        if (errors.isEmpty()) {
            fieldErrors.add(ConstraintValidationErrorsResponse.FieldError.builder()
                                    .fieldName(
                                            CommonErrors.ILLEGAL_ARGUMENT.name())
                                    .errorMessage(ExceptionUtils.getMessage(ex))
                                    .build());
        } else {
            for (final ConstraintViolation<?> fieldError : errors) {
                fieldErrors.add(ConstraintValidationErrorsResponse.FieldError.builder()
                                        .fieldName(fieldError.getPropertyPath()
                                                           .toString())
                                        .errorMessage(fieldError.getMessage())
                                        .build());
            }

        }
        if (GloabalApiExceptionHandler.LOGGER.isErrorEnabled()) {
            GloabalApiExceptionHandler.LOGGER.error(fieldErrors.toString(), ex);
        }

        final ConstraintValidationErrorsResponse apiResponse = ConstraintValidationErrorsResponse.builder()
                .errorCode(
                        CommonErrors.ILLEGAL_ARGUMENT.name())
                .fieldErrors(
                        fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(new HttpHeaders())
                .body(apiResponse);
    }

    /**
     * Handles exception of types {@link AccessDeniedException}.
     *
     * @param ex      An exception object of type {@link AccessDeniedException} if the exception is to be handled by
     *                this method.
     * @param request Web request of type {@link WebRequest}.
     *
     * @return Response entity containing the details of the exception.
     */
    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<Object> handleAccessDeniedException(final Exception ex, final WebRequest request) {
        // Get the exception message
        final String message = ExceptionUtils.getMessage(ex);

        GloabalApiExceptionHandler.LOGGER.error(message, ex);

        final WebExceptionResponse apiResponse = WebExceptionResponse.builder()
                .errorCode(ApiErrors.ACCESS_DENIED.name())
                .errorMessage(ApiErrors.ACCESS_DENIED.message())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .headers(new HttpHeaders())
                .body(apiResponse);
    }

    /**
     * Handles runtime exceptions of type {@link NullPointerException}, {@link IllegalArgumentException} or
     * {@link IllegalStateException}.
     *
     * @param ex      Runtime exception object and should be one of {@link NullPointerException} or
     *                {@link IllegalArgumentException} or {@link IllegalStateException} if they are to be handled by
     *                this method.
     * @param request Web request of type {@link WebRequest}.
     *
     * @return Response entity containing the details of the exception.
     */
    @ExceptionHandler({NullPointerException.class, IllegalArgumentException.class, IllegalStateException.class,
            IOException.class})
    public ResponseEntity<Object> handleInternal(final RuntimeException ex, final WebRequest request) {
        // Get the exception message
        final String message = ExceptionUtils.getMessage(ex);

        GloabalApiExceptionHandler.LOGGER.error(message, ex);

        final WebExceptionResponse apiResponse = WebExceptionResponse.builder()
                .errorCode(ApiErrors.GENERIC_ERROR.name())
                .errorMessage(message)
                .build();

        return handleExceptionInternal(ex, apiResponse, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles exceptions of type {@link ServiceException}.
     *
     * @param ex      Exception instance of type {@link ServiceException}.
     * @param request Web request.
     *
     * @return Response entity containing the details of the exception.
     */
    @ExceptionHandler({ServiceException.class})
    protected ResponseEntity<Object> handleServiceException(final ServiceException ex, final WebRequest request) {
        GloabalApiExceptionHandler.LOGGER.error("Service Exception: error code {}, error message {}", ex.errorCode(),
                                                ex.getMessage());
        GloabalApiExceptionHandler.LOGGER.debug(ex.getMessage(), ex);

        final WebExceptionResponse apiResponse = WebExceptionResponse.builder()
                .errorCode(ex.errorCode())
                .errorMessage(ex.getMessage())
                .build();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(status)
                .headers(new HttpHeaders())
                .body(apiResponse);
    }
}