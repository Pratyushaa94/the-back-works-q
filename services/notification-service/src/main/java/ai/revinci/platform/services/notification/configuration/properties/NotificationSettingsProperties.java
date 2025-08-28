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

package ai.revinci.platform.services.notification.configuration.properties;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ai.revinci.platform.services.notification.util.NotificationUtils;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "revinciai.platform.notification.settings")
public class NotificationSettingsProperties {
    /** Email settings. */
    private EmailSettings email = new EmailSettings();

    /** Notification settings for new tenant provisioned. */
    private NewTenantNotificationSettings tenantProvisioned = new NewTenantNotificationSettings();

    /** Notification settings for tenant deactivation. */
    private TenantDeactivatedNotificationSettings tenantDeactivated = new TenantDeactivatedNotificationSettings();


    /**
     * This method attempts to find the notification settings based on the context.
     *
     * @param context Notification context.
     *
     * @return An {@link Optional} wrapping an instance of type {@link AbstractNotificationSettings} if found, else
     *         empty {@link Optional}.
     */
    public Optional<AbstractNotificationSettings> findSettings(@NonNull final String context) {
        if (StringUtils.isBlank(context)) {
            return Optional.empty();
        }

        return switch (context) {
            case "NEW_TENANT_PROVISIONED" -> Optional.of(tenantProvisioned);
            case "TENANT_DEACTIVATED" -> Optional.of(tenantDeactivated);
            default -> Optional.empty();
        };
    }

    /**
     * This method attempts to return the blacklisted email domains as a collection.
     *
     * @return A {@link Collection} of blacklisted email domains.
     */
    public Collection<String> getBlacklistedEmailDomains() {
        if (Objects.isNull(email)) {
            return Set.of();
        }

        return email.getBlacklistedDomainsAsCollection();
    }

    /**
     * A class that holds the email settings for the platform.
     */
    @Data
    @NoArgsConstructor
    public static class EmailSettings {
        /** Comma-separated list of blacklisted email domains. */
        private String blacklistedDomains;

        /** Boolean indicating if the email domain should be replaced. */
        private boolean replaceEmailDomain;

        /** Replacement email domain (e.g., yopmail.com). */
        private String emailDomainReplacement;

        /**
         * This method returns the blacklisted email domains as a list.
         *
         * @return A {@link Collection} of blacklisted email domains.
         */
        public Collection<String> getBlacklistedDomainsAsCollection() {
            return NotificationUtils.getBlacklistedDomainsAsCollection(blacklistedDomains);
        }
    }

    /**
     * A class that holds the notification settings for new tenant provisioned.
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    public static class NewTenantNotificationSettings extends AbstractNotificationSettings {
    }

    /**
     * A class that holds the notification settings for tenant deactivation.
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    public static class TenantDeactivatedNotificationSettings extends AbstractNotificationSettings {
    }


    /**
     * An abstract class that holds the common notification settings.
     */
    @Data
    @NoArgsConstructor
    public abstract static class AbstractNotificationSettings {
        /** Context of the notification. */
        private String context;

        /** Notification type. */
        private String type;

        /** Subject for the notification. */
        private String subject;

        /** Template name for the notification. */
        private String templateName;
    }
}
