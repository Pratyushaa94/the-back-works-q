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

package ai.revinci.platform.services.tenant.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.FieldType;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.data.jpa.mapper.LookupMapper;
import ai.revinci.platform.data.jpa.model.experience.lookup.LookupValue;
import ai.revinci.platform.security.util.AuthenticationUtils;
import ai.revinci.platform.services.tenant.error.TenantServiceErrors;
import ai.revinci.platform.tenant.data.jpa.enums.LookupType;
import ai.revinci.platform.tenant.data.jpa.persistence.lookup.DataTypeEntity;
import ai.revinci.platform.tenant.data.jpa.repository.lookup.DataTypeRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LookupService {
    /** A mapper implementation of type {@link LookupMapper}. */
    private final LookupMapper lookupMapper;

    /**
     * This method retrieves the lookup values for the provided {@code lookupTypes}.
     *
     * @param lookupTypes List of lookup types.
     *
     * @return A map of lookup values where the key is the lookup type and the value is a list of lookup values.
     */
    public Map<String, List<LookupValue>> findLookupTypes(final List<LookupType> lookupTypes) {
        // 1. Make sure that the user is authenticated.
        final UUID tenantId = AuthenticationUtils.getTenantIdOrThrow();
        final String realm = AuthenticationUtils.getRealmOrThrow();
        AuthenticationUtils.getPrincipalOrThrow();

        final Map<String, List<LookupValue>> values = new HashMap<>();
        final Collection<LookupType> lookupTypesToRetrieve = CollectionUtils.isEmpty(lookupTypes) ?
                List.of(LookupType.values()) :
                lookupTypes;
        // Loop through the lookup types and retrieve the values.
        LookupService.LOGGER.info("Tenant: {}, Realm: {}. Retrieving lookup types: {}", tenantId, realm,
                                  lookupTypesToRetrieve);
        for (final LookupType lookupType : lookupTypesToRetrieve) {
            values.put(lookupType.name(), lookupMapper.findAllValuesAndTransform(lookupType.getRepositoryType()));
        }

        return values;
    }



    /**
     * This method attempts to retrieve the ORM entity representation for the provided {@code dataType}.
     *
     * @param fieldType Data type.
     *
     * @return An instance of type {@link DataTypeEntity}.
     */
    public DataTypeEntity transform(final FieldType fieldType) {
        final String valueType = fieldType.name();
        return lookupMapper.transform(valueType, DataTypeRepository.class,
                                      () -> ServiceException.of(TenantServiceErrors.LOOKUP_TYPE_NOT_FOUND,
                                                                DataTypeEntity.TABLE_NAME, valueType));
    }



}
