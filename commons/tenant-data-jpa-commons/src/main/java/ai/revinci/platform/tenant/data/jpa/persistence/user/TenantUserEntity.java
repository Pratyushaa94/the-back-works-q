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

package ai.revinci.platform.tenant.data.jpa.persistence.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.data.jpa.listener.TenantListener;
import ai.revinci.platform.data.jpa.persistence.AbstractTenantAwareEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(value = {TenantListener.class, TenantUserEntity.TenantUserEntityListener.class})
@Entity
@Table(name = TenantUserEntity.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id",
        "username"}), @UniqueConstraint(columnNames = {"tenant_id", "email"})})
public class TenantUserEntity extends AbstractTenantAwareEntity {
    /** Name of the table. */
    public static final String TABLE_NAME = "tenant_user";

    /** Unique identifier of the user in the IAM provider (e.g., Keycloak). */
    @ToString.Include
    @Column(name = "iam_user_id")
    private UUID iamUserId;

    /** Username of the user. */
    @ToString.Include
    @Column(name = "username", nullable = false)
    private String username;

    /** Email address of the user. */
    @Column(name = "email", nullable = false)
    private String email;

    /** First name of the user. */
    @ToString.Include
    @Column(name = "firstname", length = 150)
    private String firstname;

    /** Last name of the user. */
    @ToString.Include
    @Column(name = "lastname", length = 150)
    private String lastname;

    /** Boolean indicating if the user is an active user. */
    @Column(name = "active")
    private Boolean active;

    /** Boolean indicating if the user account is locked. */
    @Column(name = "locked")
    private Boolean locked;

    /** Boolean indicating if the user account is deleted. */
    @Column(name = "deleted")
    private Boolean deleted;

    /** Epoch time indicating the last-login time of the user. */
    @Column(name = "last_login")
    private Long lastLogin;

    /** Metadata associated with this user. */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserMetadataEntity> metadata = new ArrayList<>();

    @JsonIgnore
    public void initializeParent() {
        // Loop through the metadata and set the parent to this user.
        if (hasMetadata()) {
            metadata.forEach(metadata -> metadata.initializeParent(this));
        }
    }

    @JsonIgnore
    public boolean hasMetadata() {
        return !CollectionUtils.isEmpty(metadata);
    }

    @JsonIgnore
    public Optional<UserMetadataEntity> findMetadataByKey(@NonNull final String key) {
        if (!hasMetadata()) {
            return Optional.empty();
        }

        return metadata.stream()
                .filter(m -> key.equals(m.getKey()))
                .findFirst();
    }

    @JsonIgnore
    public String getFullName() {
        return Strings.getFullName(firstname, lastname);
    }

    public void addMetadataIfAbsent(final Collection<UserMetadataEntity> values) {
        // 1. Return if empty.
        if (CollectionUtils.isEmpty(values)) {
            return;
        }

        // 2. Initialize if null
        if (Objects.isNull(metadata)) {
            metadata = new ArrayList<>();
        }

        // 3. Convert the metadata values present in this user instance to a map.
        final Map<String, UserMetadataEntity> umMap = metadata.stream()
                .collect(Collectors.toMap(UserMetadataEntity::getKey,
                                          ume -> ume));
        for (final UserMetadataEntity ume : values) {
            if (umMap.containsKey(ume.getKey())) {
                continue;
            }
            metadata.add(ume);
        }
    }

    public static class TenantUserEntityListener {
        /**
         * An entity listener that is associated on the {@link TenantUserEntity} and will be called before the insertion
         * or update of the User record.
         *
         * @param user
         *         User entity that is being inserted or updated.
         */
        @PrePersist
        @PreUpdate
        public void initializeParent(final TenantUserEntity user) {
            user.initializeParent();
        }
    }
}
