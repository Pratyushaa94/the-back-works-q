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

package ai.revinci.platform.web.api.error;

import ai.revinci.platform.common.error.ErrorMessageProvider;
import ai.revinci.platform.common.error.IError;
import ai.revinci.platform.common.error.IErrorMessageProvider;

public enum ApiErrors implements IError {
    // IMPORTANT: When adding new error codes, ensure they are also added to the
    //src/main/resources/l10n/api_error_messages.properties
    ACCESS_DENIED,
    DATA_INTEGRITY_VIOLATION,
    FAILED_TO_DOWNLOAD_FILE,
    FAILED_TO_TRANSFER_FILE,
    GENERIC_ERROR,
    HTTP_MESSAGE_NOT_READABLE,
    INVALID_DATA_ACCESS;

    /** Reference to {@link IErrorMessageProvider}, which holds the error messages. */
    private static final ErrorMessageProvider ERROR_MESSAGES = ErrorMessageProvider.instance("l10n/api_error_messages",
                                                                                             ApiErrors.class.getClassLoader());

    @Override
    public IErrorMessageProvider errorMessageProvider() {
        return ApiErrors.ERROR_MESSAGES;
    }
}
