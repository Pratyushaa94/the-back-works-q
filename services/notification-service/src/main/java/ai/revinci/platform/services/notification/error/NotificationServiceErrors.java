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

package ai.revinci.platform.services.notification.error;

import ai.revinci.platform.common.error.ErrorMessageProvider;
import ai.revinci.platform.common.error.IError;
import ai.revinci.platform.common.error.IErrorMessageProvider;

public enum NotificationServiceErrors implements IError {
    // IMPORTANT: The error messages must be defined in the properties file
    // src/main/resources/l10n/notification_service_error_messages.properties
    INVALID_NOTIFICATION_MESSAGE;

    /** Reference to {@link IErrorMessageProvider}, which holds the error messages. */
    private static final ErrorMessageProvider ERROR_MESSAGE_PROVIDER = ErrorMessageProvider.instance(
            "l10n/notification_service_error_messages", NotificationServiceErrors.class.getClassLoader());

    @Override
    public IErrorMessageProvider errorMessageProvider() {
        return NotificationServiceErrors.ERROR_MESSAGE_PROVIDER;
    }
}