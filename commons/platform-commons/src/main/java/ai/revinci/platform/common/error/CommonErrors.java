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

/**
 * Enum constants that represent the common error codes and messages that can be used across the application.
 * <p>
 * For more details, see the documentation on {@link IError} contract.
 *
 * @author Subbu
 */
public enum CommonErrors implements IError {
    // NOTE:
    // Whenever a new constant is added here, ensure that the error message for the same constant is added in
    // src/main/resources/l10n/common_error_messages.properties
    ALGORITHM_NOT_FOUND,
    API_REQUEST_FAILED,
    DECRYPTION_FAILED,
    ENCRYPTION_FAILED,
    FAILED_TO_READ_FILE,
    FILE_DOES_NOT_EXIST,
    ILLEGAL_ARGUMENT,
    ILLEGAL_ARGUMENT_DETAILED,
    JSON_READ_FAILED,
    JSON_SERIALIZATION_FAILED,
    JSON_DESERIALIZATION_FAILED,
    MISSING_AUTHENTICATION,
    MISSING_REALM,
    MISSING_REALM_IN_AUTHENTICATION,
    MISSING_TENANT_ID,
    MISSING_TENANT_ID_IN_AUTHENTICATION,
    RESOURCE_NOT_FOUND,
    RESOURCE_NOT_FOUND_DETAILED,
    RESOURCES_NOT_FOUND,
    SALT_GENERATION_FAILED,
    TENANT_NOT_FOUND,
    TENANT_REALM_NOT_FOUND,
    VALIDATION_CONTEXT_MISSING_KEY,
    VALIDATION_CONTEXT_CANNOT_CAST_VALUE;

    /** Reference to {@link IErrorMessageProvider}, which holds the error messages. */
    private static final ErrorMessageProvider ERROR_MESSAGE_PROVIDER = ErrorMessageProvider.instance(
            "l10n/common_error_messages", CommonErrors.class.getClassLoader());

    @Override
    public IErrorMessageProvider errorMessageProvider() {
        return CommonErrors.ERROR_MESSAGE_PROVIDER;
    }
}
