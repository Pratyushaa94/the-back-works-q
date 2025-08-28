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

package ai.revinci.platform.provisioning.status.handler.listener;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import ai.revinci.platform.common.enums.CloudHypervisor;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.metadata.AbstractDataProvider;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ContextProvider extends AbstractDataProvider<String, Object, ContextProvider> {

    /**
     * A factory method to create an instance of the {@link ContextProvider}.
     *
     * @return An instance of the {@link ContextProvider}.
     */
    public static ContextProvider instance() {
        return new ContextProvider();
    }

    /**
     * Adds the provided {@code cloudProvider} against the key {@code Key.CLOUD_PROVIDER}.
     *
     * @param cloudProvider Cloud provider.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider cloudProvider(final CloudHypervisor cloudProvider) {
        add(Key.CLOUD_HYPERVISOR.value(), cloudProvider);
        return this;
    }

    /**
     * Adds the provided {@code accountId} against the key {@code Key.ACCOUNT_ID}.
     *
     * @param accountId Unique identifier of the account / project in the cloud provider.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider accountId(final String accountId) {
        add(Key.ACCOUNT_ID.value(), accountId);
        return this;
    }

    /**
     * Adds the provided {@code tenantId} against the key {@code Key.TENANT_ID}.
     *
     * @param tenantId Unique identifier of the tenant.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider tenantId(final UUID tenantId) {
        add(Key.TENANT_ID.value(), tenantId);
        return this;
    }

    /**
     * Adds the provided {@code realm} against the key {@code Key.REALM}.
     *
     * @param realm Tenant realm.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider realm(final String realm) {
        add(Key.REALM.value(), realm);
        return this;
    }

    /**
     * Adds the provided {@code resourceId} against the key {@code Key.TENANT_RESOURCE_ID}.
     *
     * @param resourceId Unique identifier of the tenant resource.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider resourceId(final UUID resourceId) {
        add(Key.TENANT_RESOURCE_ID.value(), resourceId);
        return this;
    }

    /**
     * Adds the provided {@code resourceStatus} against the key {@code Key.TENANT_RESOURCE_STATUS}.
     *
     * @param resourceStatus Status of the tenant resource.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider resourceStatus(final TenantResourceStatus resourceStatus) {
        add(Key.TENANT_RESOURCE_STATUS.value(), resourceStatus);
        return this;
    }

    /**
     * Adds the provided {@code dbInstanceName} against the key {@code Key.DB_INSTANCE_NAME}.
     *
     * @param dbInstanceName Database instance name.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider dbInstanceName(final String dbInstanceName) {
        add(Key.DB_INSTANCE_NAME.value(), dbInstanceName);
        return this;
    }

    /**
     * Adds the provided {@code dbName} against the key {@code Key.DB_NAME}.
     *
     * @param dbName Database name.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider dbName(final String dbName) {
        add(Key.DB_NAME.value(), dbName);
        return this;
    }

    /**
     * Adds the provided {@code operationId} against the key {@code Key.OPERATION_ID_CREATE_DB_INSTANCE}.
     *
     * @param operationId Operation identifier.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider operationIdForCreateDbInstance(final String operationId) {
        add(Key.OPERATION_ID_CREATE_DB_INSTANCE.value(), operationId);
        return this;
    }

    /**
     * Adds the provided {@code operationId} against the key {@code Key.OPERATION_ID_CREATE_DB}.
     *
     * @param operationId Operation identifier.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider operationIdForCreateDb(final String operationId) {
        add(Key.OPERATION_ID_CREATE_DB.value(), operationId);
        return this;
    }

    /**
     * Adds the provided {@code operationId} against the key {@code Key.OPERATION_ID_SHUTDOWN_DB_INSTANCE}.
     *
     * @param operationId Operation identifier.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider operationIdForShutdownDbInstance(final String operationId) {
        add(Key.OPERATION_ID_SHUTDOWN_DB_INSTANCE.value(), operationId);
        return this;
    }

    /**
     * Adds the provided {@code jdbcUrl} against the key {@code Key.JDBC_URL}.
     *
     * @param jdbcUrl JDBC url.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider jdbcUrl(final String jdbcUrl) {
        add(Key.JDBC_URL.value(), jdbcUrl);
        return this;
    }

    /**
     * Adds the provided {@code iamAccountConsoleUrl} against the key {@code Key.IAM_ACCOUNT_CONSOLE_URL}.
     *
     * @param iamAccountConsoleUrl IAM Tenant Console url.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider iamAccountConsoleUrl(final String iamAccountConsoleUrl) {
        add(Key.IAM_ACCOUNT_CONSOLE_URL.value(), iamAccountConsoleUrl);
        return this;
    }

    /**
     * Adds the provided {@code iamApplicationUrl} against the key {@code Key.IAM_APPLICATION_URL}.
     *
     * @param iamApplicationUrl Tenant application url.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider iamApplicationUrl(final String iamApplicationUrl) {
        add(Key.IAM_APPLICATION_URL.value(), iamApplicationUrl);
        return this;
    }

    /**
     * Adds the provided {@code iamAdminConsoleUrl} against the key {@code Key.IAM_ADMIN_CONSOLE_URL}.
     *
     * @param iamAdminConsoleUrl IAM tenant admin console url.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider iamAdminConsoleUrl(final String iamAdminConsoleUrl) {
        add(Key.IAM_ADMIN_CONSOLE_URL.value(), iamAdminConsoleUrl);
        return this;
    }

    /**
     * Adds the provided {@code errorMessage} against the key {@code Key.ERROR_MESSAGE}.
     *
     * @param errorMessage Error message.
     *
     * @return The current instance of the {@link ContextProvider}.
     */
    public ContextProvider errorMessage(final String errorMessage) {
        add(Key.ERROR_MESSAGE.value(), errorMessage);
        return this;
    }

    /**
     * Retrieves the value for the key {@code Key.CLOUD_PROVIDER} in this instance.
     *
     * @return Value for the key {@code Key.CLOUD_PROVIDER} if present, else returns null.
     */
    public CloudHypervisor cloudProvider() {
        return get(Key.CLOUD_HYPERVISOR.value(), CloudHypervisor.class);
    }

    /**
     * Retrieves the value for the key {@code Key.ACCOUNT_ID} in this instance.
     *
     * @return Value for the key {@code Key.ACCOUNT_ID} if present, else returns empty / blank string.
     */
    public String accountId() {
        return get(Key.ACCOUNT_ID.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.TENANT_ID} in this instance.
     *
     * @return Value for the key {@code Key.TENANT_ID} if present, else returns null.
     */
    public UUID tenantId() {
        return get(Key.TENANT_ID.value(), UUID.class);
    }

    /**
     * Retrieves the value for the key {@code Key.REALM} in this instance.
     *
     * @return Value for the key {@code Key.REALM} if present, else returns empty / blank string.
     */
    public String realm() {
        return get(Key.REALM.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.TENANT_RESOURCE_ID} in this instance.
     *
     * @return Value for the key {@code Key.TENANT_RESOURCE_ID} if present, else returns null.
     */
    public UUID resourceId() {
        return get(Key.TENANT_RESOURCE_ID.value(), UUID.class);
    }

    /**
     * Retrieves the value for the key {@code Key.TENANT_RESOURCE_STATUS} in this instance.
     *
     * @return Value for the key {@code Key.TENANT_RESOURCE_STATUS} if present, else returns null.
     */
    public TenantResourceStatus resourceStatus() {
        return get(Key.TENANT_RESOURCE_STATUS.value(), TenantResourceStatus.class);
    }

    /**
     * Retrieves the value for the key {@code Key.DB_INSTANCE_NAME} in this instance.
     *
     * @return Value for the key {@code Key.DB_INSTANCE_NAME} if present, else returns empty / blank string.
     */
    public String dbInstanceName() {
        return get(Key.DB_INSTANCE_NAME.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.DB_NAME} in this instance.
     *
     * @return Value for the key {@code Key.DB_NAME} if present, else returns empty / blank string.
     */
    public String dbName() {
        return get(Key.DB_NAME.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.OPERATION_ID_CREATE_DB_INSTANCE} in this instance.
     *
     * @return Value for the key {@code Key.OPERATION_ID_CREATE_DB_INSTANCE} if present, else returns empty / blank
     *         string.
     */
    public String operationIdForCreateDbInstance() {
        return get(Key.OPERATION_ID_CREATE_DB_INSTANCE.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.OPERATION_ID_CREATE_DB} in this instance.
     *
     * @return Value for the key {@code Key.OPERATION_ID_CREATE_DB} if present, else returns empty / blank string.
     */
    public String operationIdForCreateDb() {
        return get(Key.OPERATION_ID_CREATE_DB.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.OPERATION_ID_SHUTDOWN_DB_INSTANCE} in this instance.
     *
     * @return Value for the key {@code Key.OPERATION_ID_SHUTDOWN_DB_INSTANCE} if present, else returns empty / blank
     *         string.
     */
    public String operationIdForShutdownDbInstance() {
        return get(Key.OPERATION_ID_SHUTDOWN_DB_INSTANCE.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.JDBC_URL} in this instance.
     *
     * @return Value for the key {@code Key.JDBC_URL} if present, else returns empty / blank string.
     */
    public String jdbcUrl() {
        return get(Key.JDBC_URL.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.IAM_ACCOUNT_CONSOLE_URL} in this instance.
     *
     * @return Value for the key {@code Key.IAM_ACCOUNT_CONSOLE_URL} if present, else returns empty / blank string.
     */
    public String iamAccountConsoleUrl() {
        return get(Key.IAM_ACCOUNT_CONSOLE_URL.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.IAM_APPLICATION_URL} in this instance.
     *
     * @return Value for the key {@code Key.IAM_APPLICATION_URL} if present, else returns empty / blank string.
     */
    public String iamApplicationUrl() {
        return get(Key.IAM_APPLICATION_URL.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.IAM_ADMIN_CONSOLE_URL} in this instance.
     *
     * @return Value for the key {@code Key.IAM_ADMIN_CONSOLE_URL} if present, else returns empty / blank string.
     */
    public String iamAdminConsoleUrl() {
        return get(Key.IAM_ADMIN_CONSOLE_URL.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * Retrieves the value for the key {@code Key.ERROR_MESSAGE} in this instance.
     *
     * @return Value for the key {@code Key.ERROR_MESSAGE} if present, else returns empty / blank string.
     */
    public String errorMessage() {
        return get(Key.ERROR_MESSAGE.value(), String.class, StringUtils.EMPTY);
    }

    /**
     * This method loops through the keys and returns all keys whose names contain the string "operationId".
     *
     * @return A collection of keys whose name contains the string "operationId".
     */
    public Collection<String> operationIdKeys() {
        return get().keySet()
                .stream()
                .filter(key -> key.contains(StringUtils.capitalize(Key.OPERATION_ID.value())))
                .collect(Collectors.toSet());
    }

    /**
     * This method returns a map of key / value pairs where the key represents the contextual operation-id key (e.g.,
     * createDBInstanceOperationId, createDBOperationId) and the value is the operation identifier for the contextual
     * key.
     *
     * @return A map of key-value pairs where the key represents the contextual operation-id key and teh value is the
     *         operation identifier for the respective key.
     */
    public Map<String, String> operationIds() {
        final Collection<String> opIdKeys = operationIdKeys();
        if (opIdKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        return opIdKeys.stream()
                .collect(Collectors.toMap(key -> key, key -> get(key, String.class, StringUtils.EMPTY)));
    }

    @Override
    protected ContextProvider self() {
        return this;
    }
}

