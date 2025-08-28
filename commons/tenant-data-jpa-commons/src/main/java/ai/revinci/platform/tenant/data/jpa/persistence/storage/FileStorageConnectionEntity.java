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
package ai.revinci.platform.tenant.data.jpa.persistence.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

import org.springframework.context.ApplicationListener;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.data.jpa.persistence.AbstractTenantAwareEntity;

/**
 * ORM entity for {@code tenant_cloud_storage_connection} table. Represents a connection to a file storage cloud for a
 * specific tenant.
 */
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(value = {AuditingEntityListener.class, ApplicationListener.class})
@Entity
@Table(name = FileStorageConnectionEntity.TABLE_NAME)
public class FileStorageConnectionEntity  extends AbstractTenantAwareEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant_cloud_storage_connection";

    /** The file storage cloud entity this connection belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_cloud_id", nullable = false)
    private FileStorageEntity storageCloud;

    /** The Access code for the storage connection. */
    @Column(name = "code")
    private String accessCode;

    /** The Tenant ID for which this storage connection is valid. */
    @Column(name = "storage_tenant_id", length = 36)
    private String storageTenantId;

    /** The Client ID for the storage connection. */
    @Column(name = "storage_client_id", length = 36)
    private String storageClientId;

    /** The Access token for the storage connection. */
    @Column(name = "access_token")
    private String accessToken;

    /** The Refresh token for the storage connection. */
    @Column(name = "refresh_token")
    private String refreshToken;

    /** The time when the access token expires. */
    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    /** The Token type for the storage connection. */
    @Column(name = "token_type", length = 50)
    private String tokenType = "Bearer";

    /** The time when the consent was granted for this connection. */
    @Column(name = "consent_granted_at")
    private OffsetDateTime consentGrantedAt;

    /** The scope of the consent granted for this connection. */
    @Column(name = "consent_scope")
    private String consentScope;

    /** The status of the connection, e.g., active, inactive, etc. */
    @Column(name = "connection_status", length = 50)
    private String connectionStatus;

    /** The time when the connection was last accessed. */
    @Column(name = "last_accessed_at")
    private OffsetDateTime lastAccessedAt;
}

