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

package ai.revinci.platform.provisioning.status.handler.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.passay.PasswordGenerator;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.FieldType;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantResourceConfigurationEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantResourceEntity;
import ai.revinci.platform.provisioning.status.handler.data.repository.TenantRepository;
import ai.revinci.platform.provisioning.status.handler.enums.DeploymentEnvironment;
import ai.revinci.platform.provisioning.status.handler.enums.TenantCategory;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceStatus;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceType;
import ai.revinci.platform.provisioning.status.handler.enums.TenantStatus;
import ai.revinci.platform.provisioning.status.handler.error.ProvisioningStatusHandlerErrors;
import ai.revinci.platform.provisioning.status.handler.listener.ContextProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {
    /** Instance of type {@link Environment}. */
    private final Environment environment;

    /** A repository implementation of type {@link TenantRepository}. */
    private final TenantRepository tenantRepository;

    /** A service implementation of type {@link LookupService}. */
    private final LookupService lookupService;

    /** An instance of type {@link PasswordGenerator}. */
    private final PasswordGenerator passwordGenerator;

    /** An instance of type {@link ResourceConfigurationGenerator}. */
    private final ResourceConfigurationGenerator resourceConfigurationGenerator;

    @Instrumentation
    @Transactional(readOnly = true)
    public Optional<TenantEntity> findByIdOrRealm(final UUID tenantId, final String realm) {
        final Optional<TenantEntity> matchingTenant = tenantRepository.findById(tenantId);
        if (matchingTenant.isPresent()) {
            return matchingTenant;
        }

        return tenantRepository.findByRealmName(realm);
    }

    @Instrumentation
    @Transactional
    public TenantResourceEntity addTenantResource(@NonNull final TenantEntity tenant,
                                                  @NonNull final TenantResourceType resourceType) {
        // 1. Build an instance of type TenantResourceEntity for the provided resourceType.
        final TenantResourceEntity tre = transform(tenant, resourceType);

        // 2. Add this resource to the tenant.
        tenant.getResources()
                .add(tre);

        // 3. Add the resource configurations for this resource.
        if (TenantResourceType.PLATFORM_TENANT_DB.equals(resourceType)) {
            tre.getResourceConfigurations()
                    .addAll(getTenantDbResourceConfigurations(tre));
        } else if (TenantResourceType.PLATFORM_IAM_TENANT_REALM.equals(resourceType)) {
            tre.getResourceConfigurations()
                    .addAll(getTenantRealmResourceConfigurations(tre));
        }

        // 4. Save the tenant, which cascades the saves.
        final TenantEntity updatedTenant = tenantRepository.saveAndFlush(tenant);

        // 5. Find the created resource and return.
        return updatedTenant.findResourceType(resourceType)
                .orElseThrow(
                        () -> ServiceException.of(ProvisioningStatusHandlerErrors.TENANT_RESOURCE_NOT_FOUND,
                                                  resourceType));
    }

    @Instrumentation
    @Transactional
    public TenantEntity updateTenantStatus(@NonNull final TenantEntity tenant, @NonNull final TenantStatus newStatus) {
        // 1. Get the tenant status lookup entity and set on the tenant.
        tenant.setStatus(lookupService.transform(newStatus));

        // 2. Save the tenant and send back the updated entity.
        return tenantRepository.save(tenant);
    }

    public Optional<TenantEntity> findTenantWithResourceConfigurationKey(@NonNull final String key,
                                                                         @NonNull final String value) {
        TenantService.LOGGER.debug("Finding a tenant resource configuration with key: {}, value: {}", key, value);
        return tenantRepository.findTenantWithResourceConfigurationKey(key, value);
    }

    @Instrumentation
    @Transactional
    public void updateStatus(@NonNull final ContextProvider contextProvider) {
        // 1. Get the tenant and resource identifiers.
        final UUID tenantId = contextProvider.tenantId();
        final UUID resourceId = contextProvider.resourceId();
        final TenantResourceStatus status = contextProvider.resourceStatus();

        // 2. If any of the above values are null, then return.
        if (Objects.isNull(tenantId) || Objects.isNull(resourceId) || Objects.isNull(status)) {
            TenantService.LOGGER.warn("Tenant: {}. Resource Id: {}. Resource Status: {}. Invalid inputs", tenantId,
                                      resourceId, status);
            return;
        }

        TenantService.LOGGER.info("Tenant: {}. Resource Id: {}. Resource Status: {}. Received update status request",
                                  tenantId, resourceId, status);
        // 3. Find the matching tenant.
        final Optional<TenantEntity> matchingTenant = tenantRepository.findById(tenantId);
        if (matchingTenant.isEmpty()) {
            TenantService.LOGGER.warn("Tenant: {}. Unable to find the tenant", tenantId);
            return;
        }

        // 4. Find the matching resource.
        final TenantEntity tenant = matchingTenant.get();
        final Optional<TenantResourceEntity> matchingResource = tenant.findResourceById(resourceId);
        if (matchingResource.isEmpty()) {
            TenantService.LOGGER.warn("Tenant: {}. Unable to find the resource with id: {}", tenantId, resourceId);
            return;
        }
        final TenantResourceEntity resource = matchingResource.get();

        // 5. Build the configuration keys.
        final Map<String, String> configurationKeys = new HashMap<>();
        final String resourceType = resource.getResourceType()
                .getCode();
        if (TenantResourceType.PLATFORM_TENANT_DB.is(resourceType)) {
            final String accountId = contextProvider.accountId();
            final String dbInstanceName = contextProvider.dbInstanceName();
            final String dbName = contextProvider.dbName();
            final String jdbcUrl = contextProvider.jdbcUrl();

            putIfNotBlank(configurationKeys, Key.JDBC_URL.value(), jdbcUrl);
            putIfNotBlank(configurationKeys, Key.ACCOUNT_ID.value(), accountId);
            putIfNotBlank(configurationKeys, Key.DB_INSTANCE_NAME.value(), dbInstanceName);
            putIfNotBlank(configurationKeys, Key.DB_NAME.value(), dbName);

            final Map<String, String> operationIds = contextProvider.operationIds();
            if (!operationIds.isEmpty()) {
                operationIds.forEach((key, value) -> putIfNotBlank(configurationKeys, key, value));
            }
        } else if (TenantResourceType.PLATFORM_IAM_TENANT_REALM.is(resourceType)) {
            final String iamAccountConsoleUrl = contextProvider.iamAccountConsoleUrl();
            final String iamApplicationUrl = contextProvider.iamApplicationUrl();
            final String iamAdminConsoleUrl = contextProvider.iamAdminConsoleUrl();

            putIfNotBlank(configurationKeys, Key.IAM_ACCOUNT_CONSOLE_URL.value(), iamAccountConsoleUrl);
            putIfNotBlank(configurationKeys, Key.IAM_APPLICATION_URL.value(), iamApplicationUrl);
            putIfNotBlank(configurationKeys, Key.IAM_ADMIN_CONSOLE_URL.value(), iamAdminConsoleUrl);
        } else {
            TenantService.LOGGER.warn("Tenant: {}. Unsupported resource type: {}", tenantId, resourceType);
            return;
        }

        // 6. If the resource provisioning has failed, update the status on the resource and tenant as-well.
        if (TenantResourceStatus.PROVISIONING_FAILED.equals(status)) {
            final String failureMessage = contextProvider.errorMessage();
            handleProvisioningFailureUpdates(tenant, resource, configurationKeys, status, failureMessage);
        } else {
            handleProvisioningUpdates(tenant, resource, configurationKeys, status);
        }

        // 7. Save and flush the tenant.
        TenantService.LOGGER.debug("Tenant: {}. Resource Id: {}. Updating the tenant and resource status", tenantId,
                                   resourceId);
        tenantRepository.saveAndFlush(tenant);
    }

    private TenantResourceEntity transform(@NonNull final TenantEntity tenant,
                                           @NonNull final TenantResourceType resourceType) {
        // 1. For the provided resource type, create a TenantResourceEntity record.
        return TenantResourceEntity.builder()
                .tenant(tenant)
                .resourceType(lookupService.transform(resourceType))
                .resourceStatus(lookupService.transform(TenantResourceStatus.PROVISIONING_INITIATED))
                .build();
    }

    private Collection<TenantResourceConfigurationEntity> getTenantDbResourceConfigurations(
            @NonNull final TenantResourceEntity resource) {
        final TenantEntity tenant = resource.getTenant();
        final String realm = tenant.getRealmName();
        final String secret = tenant.getSecret();
        final TenantCategory category = TenantCategory.valueOf(tenant.getCategory()
                                                                       .getCode());

        // 1. Which environment are we dealing with?
        final String[] activeProfiles = environment.getActiveProfiles();
        final DeploymentEnvironment env = DeploymentEnvironment.findMatchingLowestEnvironment(activeProfiles);

        // 2. Configuration properties that we need to add - jdbcUrl, username, password, schema

        // Database username.
        final String username = resourceConfigurationGenerator.generateTenantDbUsername(category);
        // Generate the password, salt and encrypt using the salt.
        final String password = resourceConfigurationGenerator.generateTenantDbPassword(category, secret);
        // Database instance name.
        final String dbInstanceName = resourceConfigurationGenerator.generateTenantDbInstanceName(env, category, realm);
        // Database name.
        final String dbName = resourceConfigurationGenerator.generateTenantDbName(env, category, realm);
        // Database schema.
        final String schema = resourceConfigurationGenerator.generateTenantDbSchemaName(category, realm);
        // JDBC URL generation and update can happen later on. For enterprise tenants, the jdbc url will be
        // updated once the db provisioning process completes.
        final String jdbcUrl = resourceConfigurationGenerator.generateTenantDbJdbcUrl(category, schema);

        // Set to hold the configuration records.
        final List<TenantResourceConfigurationEntity> configurations = new ArrayList<>();
        configurations.add(transform(resource, Key.JDBC_URL.value(), FieldType.STRING, jdbcUrl));
        configurations.add(transform(resource, Key.USERNAME.value(), FieldType.STRING, username));
        configurations.add(transform(resource, Key.PASSWORD.value(), FieldType.ENCRYPTED_STRING, password));
        configurations.add(transform(resource, Key.SCHEMA.value(), FieldType.STRING, schema));
        configurations.add(transform(resource, Key.DB_INSTANCE_NAME.value(), FieldType.STRING, dbInstanceName));
        configurations.add(transform(resource, Key.DB_NAME.value(), FieldType.STRING, dbName));

        return configurations;
    }

    private Collection<TenantResourceConfigurationEntity> getTenantRealmResourceConfigurations(
            @NonNull final TenantResourceEntity resource) {
        // Set to hold the configuration records.
        final List<TenantResourceConfigurationEntity> configurations = new ArrayList<>();

        configurations.add(
                transform(resource, Key.IAM_ACCOUNT_CONSOLE_URL.value(), FieldType.STRING, StringUtils.EMPTY));
        configurations.add(transform(resource, Key.IAM_APPLICATION_URL.value(), FieldType.STRING, StringUtils.EMPTY));
        configurations.add(transform(resource, Key.IAM_ADMIN_CONSOLE_URL.value(), FieldType.STRING, StringUtils.EMPTY));

        return configurations;
    }

    private TenantResourceConfigurationEntity transform(@NonNull final TenantResourceEntity resource,
                                                        @NonNull final String key, @NonNull final FieldType fieldType,
                                                        @NonNull final String value) {
        return TenantResourceConfigurationEntity.builder()
                .resource(resource)
                .key(key)
                .valueType(fieldType.name())
                .value(value)
                .build();
    }

    private void handleProvisioningUpdates(@NonNull final TenantEntity tenant,
                                           @NonNull final TenantResourceEntity resource,
                                           final Map<String, String> configurationKeys,
                                           @NonNull final TenantResourceStatus resourceStatus) {
        TenantService.LOGGER.info("Tenant: {}. Resource: {}. Status: {}. Handling progress updates", tenant.getId(),
                                  resource.getId(), resourceStatus);
        // 1. Add the status on the resource. Add externalId if not present.
        resource.setResourceStatus(lookupService.transform(resourceStatus));
        addConfigurationsOnTenantResourceConfiguration(resource, configurationKeys);

        // 2. Update the status on the tenant.
        tenant.setStatus(lookupService.transform(TenantResourceStatus.map(resourceStatus)));
    }

    private void handleProvisioningFailureUpdates(@NonNull final TenantEntity tenant,
                                                  @NonNull final TenantResourceEntity resource,
                                                  final Map<String, String> configurationKeys,
                                                  @NonNull final TenantResourceStatus resourceStatus,
                                                  @NonNull final String failureMessage) {
        TenantService.LOGGER.info("Tenant: {}. Resource: {}. Status: {}. Failure message: {}. Handling failure updates",
                                  tenant.getId(), resource.getId(), resourceStatus, failureMessage);
        // 1. Add the status and failure message on the resource.
        resource.setResourceStatus(lookupService.transform(resourceStatus));
        resource.setFailureMessage(failureMessage);
        // Add any resource configuration records.
        addConfigurationsOnTenantResourceConfiguration(resource, configurationKeys);

        // 2. Add the status on the tenant.
        tenant.setStatus(lookupService.transform(TenantStatus.PROVISIONING_FAILED));
    }

    private void addConfigurationsOnTenantResourceConfiguration(final TenantResourceEntity resource,
                                                                final Map<String, String> configurations) {
        if (Objects.isNull(configurations) || configurations.isEmpty()) {
            TenantService.LOGGER.info("No configurations provided for Resource: {}", resource.getId());
            return;
        }

        final UUID tenantId = resource.getTenant()
                .getId();
        // Loop through the provided configuration keys.
        for (final Map.Entry<String, String> entry : configurations.entrySet()) {
            final String key = entry.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            // Value can still be blank / empty.
            final String newValue = Optional.ofNullable(entry.getValue())
                    .orElse(StringUtils.EMPTY)
                    .trim();

            // Is this key present?
            final Optional<String> ckValue = resource.findConfigurationKey(key);
            if (ckValue.isPresent()) {
                final String currentValue = ckValue.get();
                if (currentValue.equals(newValue)) {
                    TenantService.LOGGER.warn("Tenant: {}. Resource: {}. Key: {}. Value: {}. Key is already present",
                                              tenantId, resource.getId(), key, newValue);
                } else {
                    TenantService.LOGGER.warn("Tenant: {}. Resource: {}. Key: {}. Value mismatch: {} vs {}", tenantId,
                                              resource.getId(), key, newValue, currentValue);
                    resource.updateResourceConfiguration(key, newValue);
                }
            } else {
                // The configuration key is not present. Let us add it.
                resource.getResourceConfigurations()
                        .add(TenantResourceConfigurationEntity.builder()
                                     .resource(resource)
                                     .key(key)
                                     .valueType(FieldType.STRING.name())
                                     .value(newValue)
                                     .build());
            }
        }
    }

    private void putIfNotBlank(@NonNull final Map<String, String> map, @NonNull final String key, final String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

}
