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

package ai.revinci.platform.services.notification.service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.notification.client.IEmailClient;
import ai.revinci.platform.notification.enums.NotificationType;
import ai.revinci.platform.notification.model.EmailMessage;
import ai.revinci.platform.notification.model.NotificationMessage;
import ai.revinci.platform.services.notification.configuration.properties.NotificationSettingsProperties;
import ai.revinci.platform.services.notification.data.model.persistence.NotificationSettingsEntity;
import ai.revinci.platform.services.notification.data.model.persistence.TenantEntity;
import ai.revinci.platform.services.notification.data.repository.NotificationSettingsRepository;
import ai.revinci.platform.services.notification.data.repository.TenantRepository;
import ai.revinci.platform.services.notification.enums.NotificationContext;
import ai.revinci.platform.services.notification.util.NotificationUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    /** Format to build the overridden template file path. */
    private static final String OVERRIDDEN_TEMPLATE_RESOURCE_PATH = "classpath:templates/{0}/{1}";

    /** Format to build the overridden template file name. */
    private static final String OVERRIDDEN_TEMPLATE_FILE_NAME = "{0}/{1}";

    /** Named parameter template. */
    private static final String NAMED_PARAMETER_TEMPLATE = "$'{'{0}'}'";

    /** Instance of type IEmailClient, which is used to send email notifications. */
    private final IEmailClient emailClient;

    /** A repository implementation of type {@link NotificationSettingsRepository}. */
    private final NotificationSettingsRepository notificationSettingsRepository;

    /** A repository implementation of type {@link TenantRepository}. */
    private final TenantRepository tenantRepository;

    /** A configuration properties implementation of type {@link NotificationSettingsProperties}. */
    private final NotificationSettingsProperties notificationSettingsProperties;

    /** Instance of type {@link ResourceLoader}. */
    private final ResourceLoader resourceLoader;

    /** Sender email address. */
    @Value("${revinciai.platform.mail.sender.from}")
    private String sender;

    /**
     * Sends the given notification message.
     *
     * @param message The notification message to be sent.
     */
    @Instrumentation
    @Transactional(readOnly = true)
    public void send(@NonNull final NotificationMessage message) {
        NotificationService.LOGGER.debug("Tenant: {}. Attempting to send notification of type: {}",
                                         message.getTenantId(), message.getType());
        if (NotificationType.EMAIL.equals(message.getType())) {
            sendEmail(message);
        }
    }

    /**
     * Sends the given email notification message.
     *
     * @param message The email notification message to be sent.
     */
    protected void sendEmail(@NonNull final NotificationMessage message) {
        final Collection<String> blacklistedEmailDomains = notificationSettingsProperties.getBlacklistedEmailDomains();
        final NotificationSettingsProperties.EmailSettings emailSettings = notificationSettingsProperties.getEmail();
        final boolean replaceEmailDomain = emailSettings.isReplaceEmailDomain();
        final String emailDomainReplacement = replaceEmailDomain ?
                emailSettings.getEmailDomainReplacement() :
                StringUtils.EMPTY;

        final String context = message.getContext();
        final UUID tenantId = message.getTenantId();
        final Optional<NotificationSettingsProperties.AbstractNotificationSettings> contextSettings =
                notificationSettingsProperties.findSettings(context);

        // 1. Find the tenant.
        final Optional<TenantEntity> tenant = tenantRepository.findById(tenantId);
        if (tenant.isEmpty()) {
            NotificationService.LOGGER.error("Tenant: {}. Tenant not found. Skipping notification for context: {}",
                                             tenantId, context);
            return;
        }

        final List<String> to = new ArrayList<>();
        final List<String> cc = new ArrayList<>();
        final List<String> bcc = new ArrayList<>();
        String subject;
        String templateName;

        // 2. Are there emails present in the message. If so, use it as "to" email address.
        if (!CollectionUtils.isEmpty(message.getRecipients())) {
            final String[] emails = message.getRecipients()
                    .toArray(new String[0]);
            to.addAll(NotificationUtils.cleanseEmails(emails, blacklistedEmailDomains, emailDomainReplacement));
        }

        // 3. Next check the notification settings for the incoming context.
        NotificationService.LOGGER.info("Find settings for context: {}", context);
        final Optional<NotificationSettingsEntity> settings = notificationSettingsRepository.findByContextCode(context);
        if (settings.isEmpty()) {
            // Subject and template name on the message is given the priority. If not present, then we check the
            // yml files.
            subject = message.getSubject();
            templateName = message.getTemplateName();
        } else {
            final NotificationSettingsEntity nse = settings.get();
            subject = nse.getSubject();
            templateName = nse.getTemplateName();

            to.addAll(NotificationUtils.cleanseEmails(nse.getToRecipients(), blacklistedEmailDomains,
                                                      emailDomainReplacement));
            cc.addAll(NotificationUtils.cleanseEmails(nse.getCcRecipients(), blacklistedEmailDomains,
                                                      emailDomainReplacement));
            bcc.addAll(NotificationUtils.cleanseEmails(nse.getBccRecipients(), blacklistedEmailDomains,
                                                       emailDomainReplacement));
        }

        // If the subject and template are blank, check if there are any entries in the yml file.
        if (StringUtils.isBlank(subject) && contextSettings.isPresent()) {
            subject = contextSettings.get()
                    .getSubject();
        }
        if (StringUtils.isBlank(templateName) && contextSettings.isPresent()) {
            templateName = contextSettings.get()
                    .getTemplateName();
            templateName = getOverriddenTemplateName(message.getRealm(), templateName);
        }

        if (NotificationContext.NEW_TENANT_PROVISIONED.name()
                .equals(context)) {
            // Specialized handling for "NEW_TENANT_PROVISIONED" to retrieve the subject and template name.
            subject = notificationSettingsProperties.getTenantProvisioned()
                    .getSubject();
            templateName = notificationSettingsProperties.getTenantProvisioned()
                    .getTemplateName();
        }

        // Replace parameters in the subject (if any)
        subject = replaceParameters(subject, message.getPlaceholders());

        // Do not send the email message if the "to", "cc" and "bcc" are empty.
        if (to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) {
            NotificationService.LOGGER.warn("No recipients for context: {}. Notification not being sent", context);
            return;
        }

        final EmailMessage emailMessage = EmailMessage.builder()
                .from(sender)
                .subject(subject)
                .to(to)
                .cc(cc)
                .bcc(bcc)
                .build();

        // 4. Send the email message
        NotificationService.LOGGER.debug("Sending the email for context: {}, to: {}", context, to);
        emailClient.sendEmail(emailMessage, templateName, message.getPlaceholders());
    }

    /**
     * Replaces the parameters in the input string with the values from the placeholders.
     *
     * @param input        Input string.
     * @param placeholders A {@link Map} containing the parameters and their values.
     *
     * @return Input string with the parameters replaced.
     */
    private String replaceParameters(@NonNull final String input, final Map<String, Object> placeholders) {
        if (CollectionUtils.isEmpty(placeholders)) {
            return input;
        }

        String result = input;
        for (final Map.Entry<String, Object> entry : placeholders.entrySet()) {
            final String pattern = MessageFormat.format(NotificationService.NAMED_PARAMETER_TEMPLATE, entry.getKey());
            result = result.replace(pattern, entry.getValue()
                    .toString());
        }
        return result;
    }

    /**
     * This method checks if the provided {@code templateName} is overridden for the respective tenant. If yes, it
     * returns the tenant-specific overridden file name.
     *
     * @param realm        Tenant realm name.
     * @param templateName Template name.
     *
     * @return The overridden template name if it exists, else the original template name.
     */
    private String getOverriddenTemplateName(@NonNull final String realm, @NonNull final String templateName) {
        try {
            final String resourcePath = MessageFormat.format(NotificationService.OVERRIDDEN_TEMPLATE_RESOURCE_PATH,
                                                             realm, templateName);
            final Resource sefiResource = resourceLoader.getResource(resourcePath);
            return sefiResource.exists() ?
                    MessageFormat.format(NotificationService.OVERRIDDEN_TEMPLATE_FILE_NAME, realm, templateName) :
                    templateName;
        } catch (final Exception ex) {
            NotificationService.LOGGER.error("Realm: {}. Error while checking if the template is overridden: {}", realm,
                                             ex.getMessage());
        }
        return templateName;
    }
}
