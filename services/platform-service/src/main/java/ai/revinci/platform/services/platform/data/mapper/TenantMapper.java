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

package ai.revinci.platform.services.platform.data.mapper;

import java.util.List;

import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import ai.revinci.platform.services.platform.data.mapper.decorator.TenantMapperDecorator;
import ai.revinci.platform.services.platform.data.model.experience.tenant.AddTenantContact;
import ai.revinci.platform.services.platform.data.model.experience.tenant.BasicTenantDetails;
import ai.revinci.platform.services.platform.data.model.experience.tenant.CreateTenantRequest;
import ai.revinci.platform.services.platform.data.model.experience.tenant.Tenant;
import ai.revinci.platform.services.platform.data.model.persistence.TenantContactEntity;
import ai.revinci.platform.services.platform.data.model.persistence.TenantEntity;
import ai.revinci.platform.services.platform.data.model.projection.TenantSummary;

@DecoratedWith(TenantMapperDecorator.class)
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface TenantMapper {
    /**
     * This method attempts to transform an instance of type {@link CreateTenantRequest} to an instance of type
     * {@link TenantEntity}.
     *
     * @param source An instance of type {@link CreateTenantRequest}.
     *
     * @return An instance of type {@link TenantEntity}.
     */
    TenantEntity transform(CreateTenantRequest source);

    /**
     * This method attempts to transform an instance of type {@link TenantEntity} to an instance of type
     * {@link BasicTenantDetails}.
     *
     * @param source An instance of type {@link TenantEntity}.
     *
     * @return An instance of type {@link BasicTenantDetails}.
     */
    BasicTenantDetails transform(TenantEntity source);

    /**
     * This method attempts to transform an instance of type {@link TenantEntity} to an instance of type
     * {@link Tenant}.
     *
     * @param source An instance of type {@link TenantEntity}.
     *
     * @return An instance of type {@link Tenant}.
     */
    @Mapping(target = "type", source = "type.code")
    @Mapping(target = "category", source = "category.code")
    @Mapping(target = "status", source = "status.code")
    Tenant transform2(TenantEntity source);

    /**
     * This method attempts to transform an instance of type {@link AddTenantContact} to an instance of type
     * {@link TenantContactEntity}.
     *
     * @param source An instance of type {@link AddTenantContact}.
     *
     * @return An instance of type {@link TenantContactEntity}.
     */
    TenantContactEntity transform(AddTenantContact source);



    Tenant transformSummary(TenantSummary tenantSummarry);

    List<Tenant> transformSummary(List<TenantSummary> tenantSummaries);}
