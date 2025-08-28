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

package ai.revinci.platform.services.iam.keycloak.sync.data.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.data.jpa.persistence.AbstractTenantAwareEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = NotificationSettingsEntity.TABLE_NAME,
       uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "type", "context"})})
public class NotificationSettingsEntity  extends AbstractTenantAwareEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "notification_settings";

    /** Type of the notification - SMS, Email, etc. */
    @Column(name = "type", nullable = false, length = 16)
    private String type;

    /** Notification context - Form creation, feedback acknowledgement, etc. */
    @Column(name = "context", nullable = false, length = 32)
    private String context;

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
}