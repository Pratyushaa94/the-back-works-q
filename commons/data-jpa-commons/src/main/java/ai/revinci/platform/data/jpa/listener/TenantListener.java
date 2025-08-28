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

package ai.revinci.platform.data.jpa.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import java.util.Objects;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.data.jpa.persistence.ITenantAwareEntity;

@Slf4j
public class TenantListener {
    /**
     * This method is called before the entity is persisted to the database. It sets the tenant identifier on the entity
     * of type {@link ITenantAwareEntity}. This method reaches out to the {@link TenantContext} to get the tenant
     * identifier.
     *
     * @param entity Entity of type {@link ITenantAwareEntity} that is being persisted.
     */
    @PreUpdate
    @PreRemove
    @PrePersist
    public void setTenant(final ITenantAwareEntity entity) {
        final UUID tenantId = TenantContext.tenantId();
        if (Objects.isNull(tenantId)) {
            TenantListener.LOGGER.error("Tenant context does not have the tenant identifier set");
            throw new IllegalStateException("Tenant context does not have the tenant identifier set");
        }

        entity.setTenantId(tenantId);
    }
}
