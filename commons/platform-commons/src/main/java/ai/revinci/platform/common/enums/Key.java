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

package ai.revinci.platform.common.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Key implements IEnumValueProvider {
    ACCOUNT_ID("accountId"),
    CLOUD_HYPERVISOR("cloudHypervisor"),
    CORRELATION_ID("correlationId"),
    DB_INSTANCE_NAME("dbInstanceName"),
    DB_NAME("dbName"),
    EMAIL("email"),
    ERROR_MESSAGE("errorMessage"),
    IAM_ACCOUNT_CONSOLE_URL("iamAccountConsoleUrl"),
    IAM_APPLICATION_URL("iamApplicationUrl"),
    IAM_ADMIN_CONSOLE_URL("iamAdminConsoleUrl"),
    ID("id"),
    JDBC_URL("jdbcUrl"),
    LIMIT("limit"),
    NAME("name"),
    OFFSET("offset"),
    OPERATION("operation"),
    OPERATION_ID("operationId"),
    OPERATION_ID_CREATE_DB_INSTANCE("createDBInstanceOperationId"),
    OPERATION_ID_CREATE_DB("createDBOperationId"),
    OPERATION_ID_SHUTDOWN_DB_INSTANCE("shutdownDBInstanceOperationId"),
    PASSWORD("password"),
    TENANT_CONFIGURATION("configuration"),
    TENANT_CONTACT("tenantContact"),
    TENANT_ID("tenantId"),
    TENANT_IDS("tenantIds"),
    TENANT_RESOURCE_ID("resourceId"),
    TENANT_RESOURCE_STATUS("resourceStatus"),
    REALM("realm"),
    REALM_APPLICATION_URL("realmApplicationUrl"),
    RESOURCE_LIFECYCLE("resourceLifecycle"),
    RESOURCE_TYPE("resourceType"),
    ROLES("roles"),
    SCHEMA("schema"),
    SECRET("secret"),
    SERVICE_NAME("serviceName"),
    USERNAME("username");

    /** Value for this enum constant. */
    private final String value;

    @Override
    public String value() {
        return value;
    }
}
