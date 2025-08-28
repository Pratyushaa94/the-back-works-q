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

package ai.revinci.platform.provisioning.iam.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.EventBinding;
import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.IamProvider;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.configuration.TenantConfiguration;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.messaging.service.MessagePublisher;
import ai.revinci.platform.provisioning.iam.data.mapper.TenantContactMapper;
import ai.revinci.platform.provisioning.iam.data.model.experience.OnboardRealmRequest;
import ai.revinci.platform.provisioning.iam.service.lifecycle.IRealmLifecycleService;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantContactEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.TenantResourceEntity;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceStatus;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceType;
import ai.revinci.platform.provisioning.status.handler.listener.ContextProvider;
import ai.revinci.platform.provisioning.status.handler.listener.IProgressListener;
import ai.revinci.platform.provisioning.status.handler.service.TenantService;

@Slf4j
@Service
@RequiredArgsConstructor
public class IamService implements ApplicationContextAware, IProgressListener {
    private final TenantContactMapper realmMapper;
    private final TenantService tenantService;
    private final MessagePublisher messagePublisher;
    private ApplicationContext applicationContext;

    @Override
    public void update(@NonNull final ContextProvider contextProvider) {
        tenantService.updateStatus(contextProvider);
        // If the resource status is active, then it means that provisioning is complete.
        if (contextProvider.resourceStatus()
                .equals(TenantResourceStatus.ACTIVE)) {
            // Publish the event.
            publishRealmProvisionedEvent(contextProvider.tenantId(), contextProvider.realm());
        }
    }

    @Override
    public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Instrumentation
    @Async
    @Transactional
    public void provisionRealm(@NonNull final IamProvider iamProvider, @NonNull final UUID tenantId,
                               @NonNull final String realm) {
        try {
            IamService.LOGGER.info("Initiated the realm provisioning for tenant: {}", tenantId);

            // 1. Get the IIamLifecycleService bean for the specified IAM provider.
            final IRealmLifecycleService realmLifecycleService = getIamLifecycleService(tenantId, iamProvider);

            // 2. Check if the realm exists.
            final boolean realmExists = realmLifecycleService.realmExists(realm);
            if (realmExists) {
                IamService.LOGGER.info("Realm with name {} already exists. Skipping the provisioning process", realm);
                return;
            }

            // 3. Check if the tenant identifier exists in the system. If so, do not initiate the provisioning process.
            final Optional<TenantEntity> matchingTenant = tenantService.findByIdOrRealm(tenantId, realm);
            if (matchingTenant.isEmpty()) {
                IamService.LOGGER.warn(
                        "Unable to find a tenant with id {} or realm {}. Skipping the provisioning process.", tenantId,
                        realm);
                return;
            }

            // 4. If the tenant exists and if a resource record already exists, then we skip the provisioning process.
            final TenantEntity tenant = matchingTenant.get();
            final TenantResourceType resourceType = TenantResourceType.PLATFORM_IAM_TENANT_REALM;
            final Optional<TenantResourceEntity> resource = tenant.findResourceType(resourceType);
            if (resource.isPresent()) {
                IamService.LOGGER.warn(
                        "Tenant: {}. Resource type {} exists for the tenant. Skipping the provisioning process.",
                        tenantId, resourceType);
                return;
            }

            // 5. Add a new tenant-resource record in the tenant_resource table.
            final TenantResourceEntity tre = tenantService.addTenantResource(tenant, resourceType);
            IamService.LOGGER.info("Tenant: {}. Added resource and resource configuration records", tenantId);

            // 6. Build the payload to provision a new realm.
            final OnboardRealmRequest payload = getPayloadForProvisioningTenantRealm(tenant, tre);

            // 7. Get an instance of IIamLifecycleService for the specified IAM provider and delegate.
            final String beanName = iamProvider.getBeanName();
            IamService.LOGGER.info("Tenant: {}. Delegating to {} bean for realm provisioning", tenantId, beanName);
            realmLifecycleService.provisionRealm(payload, this);
        } catch (final Exception ex) {
            IamService.LOGGER.error("Tenant: {}. Errors encountered while provisioning realm within IAM. Error: {}",
                                    tenantId, ex.getMessage(), ex);
        }
    }

    private IRealmLifecycleService getIamLifecycleService(final UUID tenantId, final IamProvider iamProvider) {
        // 1. Get the bean name from the provider type.
        final String beanName = iamProvider.getBeanName();
        IamService.LOGGER.debug("Tenant: {}. Retrieving {} bean from the context", tenantId, beanName);

        // 2. Get the bean from the application context.
        return applicationContext.getBean(beanName, IRealmLifecycleService.class);
    }

    private OnboardRealmRequest getPayloadForProvisioningTenantRealm(final TenantEntity tenant,
                                                                     final TenantResourceEntity resource) {
        final UUID tenantId = tenant.getId();
        final UUID resourceId = resource.getId();
        final String realm = tenant.getRealmName();
        final TenantConfiguration tenantConfiguration = tenant.getConfiguration();
        final List<TenantContactEntity> contacts = tenant.getContacts();
        IamService.LOGGER.info("Tenant: {}. Realm: {}. Number of contacts: {}", tenantId, realm, contacts.size());

        // 3. Build the payload.
        return OnboardRealmRequest.builder()
                .roles(defaultRolesForNewRealm(realm))
                .users(realmMapper.transform(contacts))
                .tenant(OnboardRealmRequest.Tenant.builder()
                                .id(tenantId)
                                .realmName(realm)
                                .resourceId(resourceId)
                                .build())
                .passwordPolicy(tenantConfiguration.getPasswordPolicy())
                .userRegistrationPolicy(tenantConfiguration.getUserRegistrationPolicy())
                .build();
    }

    private List<OnboardRealmRequest.RealmRole> defaultRolesForNewRealm(final String realm) {
        // Admin role.
        final OnboardRealmRequest.RealmRole admin = OnboardRealmRequest.RealmRole.builder()
                .name(PlatformDefaults.ROLE_ADMIN.value())
                .description(
                        PlatformDefaults.ROLE_ADMIN_DESCRIPTION.value())
                .build();
        if (PlatformDefaults.REALM_RVC.value()
                .equals(realm)) {
            // If revinci tenant, we will add "super_admin" role.
            return List.of(admin, OnboardRealmRequest.RealmRole.builder()
                    .name(PlatformDefaults.ROLE_SUPER_ADMIN.value())
                    .description(
                            PlatformDefaults.ROLE_SUPER_ADMIN_DESCRIPTION.value())
                    .build());
        }

        // Others will have only admin role.
        return List.of(admin);
    }

    private void publishRealmProvisionedEvent(final UUID tenantId, final String realm) {
        final String bindingName = EventBinding.OUT_PUBLISH_REALM_PROVISIONED_EVENT.value();
        final TenantRealm tenantRealm = TenantRealm.builder()
                .tenantId(tenantId)
                .realm(realm)
                .build();

        IamService.LOGGER.info(
                "Tenant: {}, Realm: {}, Binding: {}. Publishing realm provisioned message to the binding", tenantId,
                realm, bindingName);

        messagePublisher.publish(tenantRealm, bindingName, Map.of());

        IamService.LOGGER.debug(
                "Tenant: {}, Realm: {}, Binding: {}. Successfully published realm provisioned message to the binding",
                tenantId, realm, bindingName);
    }
}
