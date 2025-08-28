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

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.data.jpa.mapper.LookupMapper;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.lookup.TenantResourceStatusEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.lookup.TenantResourceTypeEntity;
import ai.revinci.platform.provisioning.status.handler.data.model.persistence.lookup.TenantStatusEntity;
import ai.revinci.platform.provisioning.status.handler.data.repository.lookup.TenantResourceStatusRepository;
import ai.revinci.platform.provisioning.status.handler.data.repository.lookup.TenantResourceTypeRepository;
import ai.revinci.platform.provisioning.status.handler.data.repository.lookup.TenantStatusRepository;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceStatus;
import ai.revinci.platform.provisioning.status.handler.enums.TenantResourceType;
import ai.revinci.platform.provisioning.status.handler.enums.TenantStatus;
import ai.revinci.platform.provisioning.status.handler.error.ProvisioningStatusHandlerErrors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LookupService {
    /** A mapper implementation of type {@link LookupMapper}. */
    private final LookupMapper lookupMapper;


    /**
     * This method transforms the provided {@code status} to an instance of type {@link TenantStatusEntity}.
     *
     * @param status Tenant status type to transform.
     *
     * @return An instance of type {@link TenantStatusEntity}.
     */
    public TenantStatusEntity transform(@NonNull final TenantStatus status) {
        final String statusName = status.name();
        return lookupMapper.transform(statusName, TenantStatusRepository.class,
                                      () -> ServiceException.of(ProvisioningStatusHandlerErrors.LOOKUP_TYPE_NOT_FOUND,
                                                                TenantStatusEntity.TABLE_NAME, statusName));
    }

    /**
     * This method transforms the provided {@code resourceType} to an instance of type
     * {@link TenantResourceTypeEntity}.
     *
     * @param resourceType Resource type to transform.
     *
     * @return An instance of type {@link TenantResourceTypeEntity}.
     */
    public TenantResourceTypeEntity transform(@NonNull final TenantResourceType resourceType) {
        final String resourceTypeName = resourceType.name();
        return lookupMapper.transform(resourceTypeName, TenantResourceTypeRepository.class,
                                      () -> ServiceException.of(ProvisioningStatusHandlerErrors.LOOKUP_TYPE_NOT_FOUND,
                                                                TenantResourceTypeEntity.TABLE_NAME, resourceTypeName));
    }

    /**
     * This method transforms the provided {@code resourceStatus} to an instance of type
     * {@link TenantResourceStatusEntity}.
     *
     * @param resourceStatus Resource status to transform.
     *
     * @return An instance of type {@link TenantResourceStatusEntity}.
     */
    public TenantResourceStatusEntity transform(@NonNull final TenantResourceStatus resourceStatus) {
        final String resourceStatusName = resourceStatus.name();
        return lookupMapper.transform(resourceStatusName, TenantResourceStatusRepository.class,
                                      () -> ServiceException.of(ProvisioningStatusHandlerErrors.LOOKUP_TYPE_NOT_FOUND,
                                                                TenantResourceStatusEntity.TABLE_NAME,
                                                                resourceStatusName));
    }
}
