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

package ai.revinci.platform.services.platform.service;

import jakarta.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.EventBinding;
import ai.revinci.platform.common.enums.CloudHypervisor;
import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.enums.ResourceState;
import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.common.tenant.context.TenantRealm;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.messaging.service.MessagePublisher;
import ai.revinci.platform.notification.enums.NotificationType;
import ai.revinci.platform.notification.model.NotificationMessage;
import ai.revinci.platform.services.platform.data.mapper.TenantMapper;
import ai.revinci.platform.services.platform.data.model.experience.tenant.BasicTenantDetails;
import ai.revinci.platform.services.platform.data.model.experience.tenant.CreateTenantRequest;
import ai.revinci.platform.services.platform.data.model.experience.tenant.Tenant;
import ai.revinci.platform.services.platform.data.model.persistence.TenantContactEntity;
import ai.revinci.platform.services.platform.data.model.persistence.TenantEntity;
import ai.revinci.platform.services.platform.data.model.projection.TenantSummary;
import ai.revinci.platform.services.platform.data.repository.TenantRepository;
import ai.revinci.platform.services.platform.enums.TenantStatus;
import ai.revinci.platform.services.platform.error.PlatformServiceErrors;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class TenantService {
    /** A repository implementation of type {@link TenantRepository}. */
    private final TenantRepository tenantRepository;

    /** A mapper implementation of type {@link TenantMapper}. */
    private final TenantMapper tenantMapper;

    /** Scheduled thread-pool executor. */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /** A message publisher implementation of type {@link MessagePublisher}. */
    private final MessagePublisher messagePublisher;

    /** Application URL. */
    @Value("${revinciai.platform.application-url}")
    private String rvcPlatformApplicationUrl;

    /** Email to be notified whenever a tenant is provisioned. In the initial stages, this will be info@revinci.ai */
    @Value("${revinciai.platform.notification.tenant-provisioned.recipient:}")
    private String tenantProvisionedNotificationRecipient;

    /**
     * This method attempts to create a new tenant in the system using the provided payload.
     * <p>
     * Behind the scenes, this method triggers an asynchronous process to provision the resources for this tenant in the
     * system.
     *
     * @param payload Payload containing the details of the new tenant, which is to be created.
     *
     * @return A {@link BasicTenantDetails} object representing the tenant that was created. Note that the tenant may
     *         not be fully provisioned yet.
     */
    @Instrumentation
    @Transactional
    public BasicTenantDetails create(@Valid final CreateTenantRequest payload) {
        // 1. Check if the tenant with the provided realm-name already exists in the system.
        final String realm = payload.getRealmName();
        if (tenantRepository.findByRealmName(realm)
                .isPresent()) {
            TenantService.LOGGER.error("Tenant with realm-name {} already exists in the system.", realm);
            throw ServiceException.of(PlatformServiceErrors.TENANT_REALM_EXISTS, realm);
        }

        // 2. Transform the payload to the entity model and save to the database.
        final TenantEntity newTenant = tenantRepository.saveAndFlush(tenantMapper.transform(payload));
        TenantService.LOGGER.info("Successfully created a new tenant with realm {}, id {}", newTenant.getRealmName(),
                                  newTenant.getId());

        // 3. Fire an event that a new tenant has been created.
        publishTenantCreatedEvent(newTenant.getId(), realm);

        // 4. Transform and return the transformed data back to the caller.
        return tenantMapper.transform(newTenant);
    }

    /**
     * This method attempts to retrieve the details of a tenant in the system using the provided tenant-id.
     * <p>
     *
     * @param id The unique identifier of the tenant to be retrieved and cannot be null.
     *
     * @return A {@link Tenant} object representing the tenant that was retrieved.
     */
    @Instrumentation
    @Transactional(readOnly = true)
    public Tenant findOne(@NonNull final UUID id) {
        // 1. Check if the tenant with the provided id exists in the system.
        final Optional<TenantEntity> matchingTenant = tenantRepository.findById(id);
        if (matchingTenant.isEmpty()) {
            TenantService.LOGGER.error("Tenant with id {} not found in the system.", id);
            throw ServiceException.of(CommonErrors.TENANT_NOT_FOUND, id);
        }

        // 2. Transform the entity to the experience model and return.
        return tenantMapper.transform2(matchingTenant.get());
    }

    /**
     * This method is called whenever a tenant has been onboarded successfully.
     * <p>
     * This method extracts the tenant contacts (from the onboarded tenant) and sends an email to each of the contacts
     * with the details of the tenant that was onboarded.
     *
     * @param id    Unique identifier of the tenant that was onboarded.
     * @param realm Realm name of the tenant that was onboarded.
     *
     * @throws NoSuchAlgorithmException If there are issues while generating the key for the notification message.
     */
    @Instrumentation
    @Transactional(readOnly = true)
    public void tenantOnboarded(@NonNull final UUID id, @NonNull final String realm) throws NoSuchAlgorithmException {
        // 1. Check if the tenant with the provided id exists in the system.
        Optional<TenantEntity> matchingTenant = tenantRepository.findById(id);
        if (matchingTenant.isEmpty()) {
            TenantService.LOGGER.error("Tenant: {}. Tenant not found. Trying to find by realm {}", id, realm);
            matchingTenant = tenantRepository.findByRealmName(realm);
            if (matchingTenant.isEmpty()) {
                TenantService.LOGGER.error("Tenant: {}. Realm: {}. Unable to find tenant", id, realm);
                throw ServiceException.of(CommonErrors.TENANT_NOT_FOUND, id);
            }
        }

        final TenantEntity tenant = matchingTenant.get();

        // 2. Is the tenant status active?
        final TenantStatus expectedStatus = TenantStatus.ACTIVE;
        if (!tenant.hasStatus(expectedStatus)) {
            TenantService.LOGGER.warn(
                    "Tenant: {}. Realm: {}. Expecting tenant status as {} but got {}. Skipping notifications", id,
                    realm, expectedStatus, tenant.getStatus()
                            .getCode());
            return;
        }

        // 3. Tenant is in active state. If the tenant has contacts, let us notify them.
        if (!tenant.hasContacts()) {
            TenantService.LOGGER.error("Tenant: {}. Realm: {}. Tenant has no contacts. Skipping notifications", id,
                                       realm);
            return;
        }

        // TODO: This is a temporary solution to defer the message publishing by 1 minute.
        TenantService.LOGGER.info(
                "Tenant: {}. Realm: {}. Message to notify tenant contacts will be published in 2 minutes", id, realm);
        scheduler.schedule(() -> {
            TenantService.LOGGER.info("Tenant: {}. Realm: {}. Notifying {} contacts", id, realm, tenant.getContacts()
                    .size());
            try {
                publishNotifyTenantContactsEvent(tenant);
            } catch (final NoSuchAlgorithmException e) {
                TenantService.LOGGER.error(
                        "Tenant: {}. Realm: {}. Error while publishing notification message. Error: {}", id, realm,
                        e.getMessage(), e);
            }
        }, 2, TimeUnit.MINUTES);
    }

    /**
     * This method publishes a message to the output binding {@code rvcPlatformPublishNewTenantCreatedEvent-out-0} to
     * indicate that a new tenant has been created.
     *
     * @param tenantId The unique identifier of the tenant that was created.
     * @param realm    The realm name of the tenant that was created.
     */
    private void publishTenantCreatedEvent(final UUID tenantId, final String realm) {
        final TenantRealm tenantRealm = TenantRealm.builder()
                .tenantId(tenantId)
                .realm(realm)
                .build();
        final String bindingName = EventBinding.OUT_PUBLISH_NEW_TENANT_CREATED_EVENT.value();
        final Map<String, Object> data = Map.of(Key.CLOUD_HYPERVISOR.value(), CloudHypervisor.AZURE.name(),
                                                Key.TENANT_ID.value(), tenantId, Key.REALM.value(), realm,
                                                Key.RESOURCE_LIFECYCLE.value(), ResourceState.CREATE);

        TenantService.LOGGER.info("Tenant: {}, Realm: {}, Binding: {}. Publishing tenant created event", tenantId,
                                  realm, bindingName);

        // Publish now.
        messagePublisher.publish(tenantRealm, bindingName, data);

        TenantService.LOGGER.debug("Tenant: {}, Realm: {}, Binding: {}. Successfully published tenant created event",
                                   tenantId, realm, bindingName);
    }

    /**
     * This method publishes a notification event, which will be received by the {@code notification-service}.
     * <p>
     * The {@code notification-service} is responsible to send the email notifications.
     *
     * @param tenant Tenant containing the contacts to be notified.
     *
     * @throws NoSuchAlgorithmException If there are issues while generating the key for the notification message.
     */
    private void publishNotifyTenantContactsEvent(final TenantEntity tenant) throws NoSuchAlgorithmException {
        final UUID id = tenant.getId();
        final String realm = tenant.getRealmName();
        final TenantRealm tenantRealm = TenantRealm.builder()
                .tenantId(id)
                .realm(realm)
                .build();
        final String realmApplicationUrl = rvcPlatformApplicationUrl.concat(Token.FORWARD_SLASH.value())
                .concat(realm);
        final byte[] secret = tenant.getSecret()
                .getBytes();

        for (final TenantContactEntity contact : tenant.getContacts()) {
            final String recipientEmail = StringUtils.isBlank(tenantProvisionedNotificationRecipient) ?
                    contact.getEmail() :
                    tenantProvisionedNotificationRecipient;

            final Map<String, Object> placeholders = new HashMap<>();
            placeholders.put(Key.TENANT_CONTACT.value(), contact.getFullName());
            placeholders.put(Key.REALM_APPLICATION_URL.value(), realmApplicationUrl);
            placeholders.put(Key.EMAIL.value(), recipientEmail);
            placeholders.put(Key.PASSWORD.value(), Strings.decrypt(contact.getTempPassword(), secret));

            // Create a notification message.
            final NotificationMessage nm = NotificationMessage.builder()
                    .tenantId(id)
                    .realm(realm)
                    .type(NotificationType.EMAIL)
                    .context("NEW_TENANT_PROVISIONED")
                    .placeholders(placeholders)
                    .recipient(recipientEmail)
                    .build();

            final String bindingName = EventBinding.OUT_PUBLISH_NOTIFICATION_EVENT.value();
            TenantService.LOGGER.info("Tenant: {}, Realm: {}, Binding: {}. Publishing notify tenant contact event", id,
                                      realm, bindingName);

            messagePublisher.publish(tenantRealm, bindingName, nm);

            TenantService.LOGGER.info(
                    "Tenant: {}, Realm: {}, Binding: {}. Successfully published notify tenant contact event", id, realm,
                    bindingName);
        }
    }

    /**
     * This method attempts to fetch all the tenants in the system.
     * <p>
     * If there are no tenants in the system, then this method returns an empty list.
     *
     * @return A list of {@link Tenant} objects representing the tenants that were retrieved.
     */
    @Instrumentation
    @Transactional(readOnly = true)
    public List<Tenant> fetchAllTenants() {
        TenantService.LOGGER.info("Fetching all the tenants in the system");
        // 1. Fetch all the tenants from the database.
        final List<TenantSummary> tenantEntities = tenantRepository.findAllTenants();

        // 2. Transform the entity list to experience model list and return.
        return tenantMapper.transformSummary(tenantEntities);
    }
}
