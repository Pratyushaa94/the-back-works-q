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

package ai.revinci.platform.services.platform.data.mapper.decorator;

import java.util.List;
import java.util.Objects;

import org.passay.PasswordGenerator;
import org.passay.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.password.PasswordPolicy;
import ai.revinci.platform.common.tenant.configuration.TenantConfiguration;
import ai.revinci.platform.common.tenant.policy.TenantPasswordPolicy;
import ai.revinci.platform.common.tenant.policy.UserRegistrationPolicy;
import ai.revinci.platform.common.util.SaltGenerator;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.data.jpa.mapper.LookupMapper;
import ai.revinci.platform.services.platform.data.mapper.TenantMapper;
import ai.revinci.platform.services.platform.data.model.experience.tenant.CreateTenantRequest;
import ai.revinci.platform.services.platform.data.model.experience.tenant.Tenant;
import ai.revinci.platform.services.platform.data.model.persistence.TenantEntity;
import ai.revinci.platform.services.platform.data.model.persistence.lookup.TenantCategoryEntity;
import ai.revinci.platform.services.platform.data.model.persistence.lookup.TenantStatusEntity;
import ai.revinci.platform.services.platform.data.model.persistence.lookup.TenantTypeEntity;
import ai.revinci.platform.services.platform.data.model.projection.TenantSummary;
import ai.revinci.platform.services.platform.data.repository.lookup.TenantCategoryRepository;
import ai.revinci.platform.services.platform.data.repository.lookup.TenantStatusRepository;
import ai.revinci.platform.services.platform.data.repository.lookup.TenantTypeRepository;
import ai.revinci.platform.services.platform.enums.TenantCategory;
import ai.revinci.platform.services.platform.enums.TenantStatus;
import ai.revinci.platform.services.platform.enums.TenantType;
import ai.revinci.platform.services.platform.error.PlatformServiceErrors;

@Slf4j
public abstract class TenantMapperDecorator implements TenantMapper {
    /** Default delegate. */
    @Autowired
    @Qualifier("delegate")
    private TenantMapper delegate;

    /** A mapper instance of type {@link LookupMapper}. */
    @Autowired
    private LookupMapper lookupMapper;


    /** A password generator instance of type {@link PasswordGenerator}. */
    @Autowired
    private PasswordGenerator passwordGenerator;

    @Override
    public TenantEntity transform(@NonNull final CreateTenantRequest source) {
        final TenantEntity tenant = delegate.transform(source);
        // Add a secret.
        tenant.setSecret(SaltGenerator.generateSalt());
        // If the realm is revinci, then set the tenant as a master tenant.
        tenant.setMaster(source.getRealmName()
                                 .equals(PlatformDefaults.REALM_RVC.value()));
        // Set the configuration details.
        final TenantPasswordPolicy passwordPolicy = Objects.isNull(source.getPasswordPolicy()) ?
                getDefaultTenantPasswordPolicy() :
                source.getPasswordPolicy();
        // User registration policy where we default the email address as the username in the IAM system.
        UserRegistrationPolicy userRegistrationPolicy = source.getUserRegistrationPolicy();
        if (Objects.isNull(userRegistrationPolicy)) {
            userRegistrationPolicy = UserRegistrationPolicy.builder()
                    .registerEmailAsUsername(true)
                    .build();
        }


        tenant.setConfiguration(TenantConfiguration.builder()
                                        .passwordPolicy(passwordPolicy)
                                        .userRegistrationPolicy(userRegistrationPolicy)
                                        .build());

        // Transform the lookup types.
        final String typeName = source.getType()
                .name();
        tenant.setType(lookupMapper.transform(typeName, TenantTypeRepository.class,
                                              () -> ServiceException.of(PlatformServiceErrors.LOOKUP_TYPE_NOT_FOUND,
                                                                        TenantTypeEntity.TABLE_NAME, typeName)));
        final String categoryName = source.getCategory()
                .name();
        tenant.setCategory(lookupMapper.transform(categoryName, TenantCategoryRepository.class,
                                                  () -> ServiceException.of(PlatformServiceErrors.LOOKUP_TYPE_NOT_FOUND,
                                                                            TenantCategoryEntity.TABLE_NAME,
                                                                            categoryName)));
        final String status = TenantStatus.NEW.name();
        tenant.setStatus(lookupMapper.transform(status, TenantStatusRepository.class,
                                                () -> ServiceException.of(PlatformServiceErrors.LOOKUP_TYPE_NOT_FOUND,
                                                                          TenantStatusEntity.TABLE_NAME, status)));

        // If there are any tenant-contacts, make sure that we set the tenant object on them.
        if (tenant.hasContacts()) {
            final int passwordLength = PasswordPolicy.getRandomPasswordLength();
            final List<Rule> defaultRules = PasswordPolicy.usingDefaults();
            final byte[] secret = tenant.getSecret()
                    .getBytes();
            tenant.getContacts()
                    .forEach(contact -> {
                        contact.setTenant(tenant);
                        // Generate the temporary password, encrypt and set.
                        final String password = passwordGenerator.generatePassword(passwordLength, defaultRules);
                        contact.setTempPassword(Strings.encrypt(password, secret));
                    });
        }


        return tenant;
    }


    /**
     * This method returns the default tenant password policy.
     *
     * @return Default tenant password policy.
     */
    private TenantPasswordPolicy getDefaultTenantPasswordPolicy() {
        return TenantPasswordPolicy.builder()
                .length(PasswordPolicy.getMinimumPasswordLength())
                .maxLength(PasswordPolicy.getMaximumPasswordLength())
                .expirationDays(PasswordPolicy.getPasswordExpirationDays())
                .passwordHistory(PasswordPolicy.getPasswordHistory())
                .numberOfUpperCaseCharacters(PasswordPolicy.getMinimumUpperCaseCharactersCount())
                .numberOfLowerCaseCharacters(PasswordPolicy.getMinimumLowerCaseCharactersCount())
                .numberOfDigits(PasswordPolicy.getMinimumDigitsCount())
                .numberOfSpecialCharacters(PasswordPolicy.getMinimumSpecialCharactersCount())
                .build();
    }

    public Tenant transformSummary(TenantSummary tenantSummary){
        // Use the delegate to map direct attributes
        Tenant tenant = delegate.transformSummary(tenantSummary);

        // Add custom logic for specific fields if needed
        tenant.setStatus(TenantStatus.valueOf(tenantSummary.getStatus()));
        tenant.setCategory(TenantCategory.valueOf(tenantSummary.getCategory()));
        tenant.setType(TenantType.valueOf(tenantSummary.getType()));
        return tenant;
    }
}
