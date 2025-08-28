/*
 *
 *  * Copyright (c) 2025 Revinci AI.
 *  *
 *  * All rights reserved. This software is proprietary to and embodies the
 *  * confidential technology of Revinci AI. Possession,
 *  * use, duplication, or dissemination of the software and media is
 *  * authorized only pursuant to a valid written license from Revinci AI.
 *  *
 *  * Unauthorized use of this software is strictly prohibited.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 *  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL REVINCI AI BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */
package ai.revinci.platform.error;

import ai.revinci.platform.common.error.ErrorMessageProvider;
import ai.revinci.platform.common.error.IError;
import ai.revinci.platform.common.error.IErrorMessageProvider;

/**
 * An enumerated data type that represents the errors related to cache operations.
 * <p>
 * This enum is used to define specific error constants that can be used throughout the application
 * to handle cache-related errors in a consistent manner.
 * </p>
 *
 * @author Subbu
 *
 * */

public enum CacheErrors implements IError {
    // IMPORTANT: When adding new error constants, ensure that the error messages are added to the
    // src/main/resources/l10n/cache_error_messages.properties

    CACHE_OPERATION_FAILED,
    FAILED_TO_DESERIALIZE_CACHE_VALUE;

    /** Reference to {@link IErrorMessageProvider}, which holds the error messages. */
    private static final ErrorMessageProvider ERROR_MESSAGE_PROVIDER = ErrorMessageProvider.instance(
            "l10n/cache_error_messages", CacheErrors.class.getClassLoader());

    @Override
    public IErrorMessageProvider errorMessageProvider() {
        return CacheErrors.ERROR_MESSAGE_PROVIDER;
    }
}
