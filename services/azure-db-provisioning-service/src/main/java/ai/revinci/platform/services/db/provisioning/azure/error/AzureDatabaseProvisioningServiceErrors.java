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

package ai.revinci.platform.services.db.provisioning.azure.error;

import ai.revinci.platform.common.error.ErrorMessageProvider;
import ai.revinci.platform.common.error.IError;
import ai.revinci.platform.common.error.IErrorMessageProvider;

/**
 * Enum containing all the error codes for the Azure Database Provisioning Service.
 */
public enum AzureDatabaseProvisioningServiceErrors implements IError {
    // IMPORTANT: The error messages in this enum must match the keys in the properties file
    // src/main/resources/i10n/azure_database_provisioning_service_error_messages.properties
    AZURE_POSTGRESQL_SERVER_CREATION_FAILED,
    AZURE_POSTGRESQL_SERVER_DELETION_FAILED,
    AZURE_POSTGRESQL_DATABASE_CREATION_FAILED,
    AZURE_POSTGRESQL_FIREWALL_RULE_CREATION_FAILED,
    TENANT_DATABASE_SETTINGS_NOT_FOUND,
    AZURE_POSTGRESQL_OPERATION_FAILED,
    AZURE_POSTGRESQL_OPERATION_TIMEOUT;

    /** Reference to {@link IErrorMessageProvider}, which holds the error messages. */
    private static final ErrorMessageProvider ERROR_MESSAGE_PROVIDER = ErrorMessageProvider.instance(
            "i10n/azure_database_provisioning_service_error_messages",
            AzureDatabaseProvisioningServiceErrors.class.getClassLoader());

    @Override
    public IErrorMessageProvider errorMessageProvider() {
        return AzureDatabaseProvisioningServiceErrors.ERROR_MESSAGE_PROVIDER;
    }
}
