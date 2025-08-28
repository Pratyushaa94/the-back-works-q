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

package ai.revinci.platform.services.notification.data.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.annotations.Type;
import org.springframework.context.ApplicationListener;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.data.jpa.persistence.AbstractTenantAwareEntity;
import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;
import ai.revinci.platform.data.jpa.persistence.ITenantAwareEntity;
import ai.revinci.platform.services.notification.data.model.persistence.lookup.NotificationContextEntity;
import ai.revinci.platform.services.notification.data.model.persistence.lookup.NotificationTypeEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(value = {ApplicationListener.class})
@Entity
@Table(name = NotificationSettingsEntity.TABLE_NAME,
       uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "type", "context"})})
public class NotificationSettingsEntity extends AbstractTenantAwareEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "notification_settings";

    /** Type of the notification - SMS, Email, etc. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", nullable = false)
    private NotificationTypeEntity type;

    /** Notification context - Form creation, feedback acknowledgement, etc. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context", nullable = false)
    private NotificationContextEntity context;

    /** Subject for the notification. */
    @ToString.Include
    @Column(name = "subject", nullable = false, length = 512)
    private String subject;

    /** "To" recipients. */
    @Type(StringArrayType.class)
    @Column(name = "\"to\"", columnDefinition = "varchar(512)[]")
    private String[] toRecipients;

    /** "cc" recipients. */
    @Type(StringArrayType.class)
    @Column(name = "cc", columnDefinition = "varchar(512)[]")
    private String[] ccRecipients;

    /** "bcc" recipients. */
    @Type(StringArrayType.class)
    @Column(name = "bcc", columnDefinition = "varchar(512)[]")
    private String[] bccRecipients;

    /** Template name to be used for sending the notification. */
    @ToString.Include
    @Column(name = "template_name", nullable = false, length = 1024)
    private String templateName;

    /** Boolean indicating if the tenant is a master tenant or not. */
    @Column(name = "enabled")
    private boolean enabled = true;

    /**
     * Checks if the notification has "to" recipients.
     *
     * @return {@code true} if the notification has "to" recipients, {@code false} otherwise.
     */
    public boolean hasToRecipients() {
        return Objects.nonNull(toRecipients) && toRecipients.length > 0;
    }

    /**
     * Checks if the notification has "cc" recipients.
     *
     * @return {@code true} if the notification has "cc" recipients, {@code false} otherwise.
     */
    public boolean hasCcRecipients() {
        return Objects.nonNull(ccRecipients) && ccRecipients.length > 0;
    }

    /**
     * Checks if the notification has "bcc" recipients.
     *
     * @return {@code true} if the notification has "bcc" recipients, {@code false} otherwise.
     */
    public boolean hasBccRecipients() {
        return Objects.nonNull(bccRecipients) && bccRecipients.length > 0;
    }

    /**
     * This method returns a boolean indicating if the provided {@code recipient} is present in the "to", "cc", or
     * "bcc"
     *
     * @param recipient Recipient to be checked for presence.
     *
     * @return {@code true} if the recipient is present, {@code false} otherwise.
     */
    public boolean isRecipientPresent(final String recipient) {
        // @formatter:off
        return (hasToRecipients() && Arrays.asList(toRecipients).contains(recipient)) ||
                (hasCcRecipients() && Arrays.asList(ccRecipients).contains(recipient)) ||
                (hasBccRecipients() && Arrays.asList(bccRecipients).contains(recipient));
        // @formatter:on
    }
}