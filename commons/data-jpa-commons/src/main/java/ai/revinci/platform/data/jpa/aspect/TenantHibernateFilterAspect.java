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

package ai.revinci.platform.data.jpa.aspect;

import jakarta.persistence.EntityManagerFactory;

import java.util.Objects;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.UnknownFilterException;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.tenant.context.TenantContext;
import ai.revinci.platform.data.jpa.persistence.AbstractTenantAwareEntity;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TenantHibernateFilterAspect {

    private final EntityManagerFactory entityManagerFactory;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object enableFilter(final ProceedingJoinPoint joinPoint) throws Throwable {
        enableTenantFilters();
        return joinPoint.proceed();
    }

    private void enableTenantFilters() {
        final Object entityManagerHolder = TransactionSynchronizationManager.getResource(entityManagerFactory);
        if (entityManagerHolder instanceof EntityManagerHolder entityMgrHolder) {
            // Get the tenant  identifiers.
            final UUID tenantId = TenantContext.tenantId();

            if (Objects.isNull(tenantId) ) {
                return;
            }

            try {
                final Session session = entityMgrHolder.getEntityManager()
                        .unwrap(Session.class);
                enableFilter(session, AbstractTenantAwareEntity.TENANT_FILTER_NAME,
                             AbstractTenantAwareEntity.TENANT_ID_PARAMETER_NAME, tenantId);

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        disableTenantFilter(session);
                    }
                });

                // session.flush();
            } catch (final UnknownFilterException ufe) {
                TenantHibernateFilterAspect.LOGGER.trace("Failures while enabling filter {} ",
                                                         AbstractTenantAwareEntity.TENANT_FILTER_NAME);
            }
        }

    }

    /**
     * This method enables the filter with the specified name and sets the parameter value.
     *
     * @param session        Entity manager session.
     * @param filterName     Filter name to enable.
     * @param parameterName  Parameter name to set.
     * @param parameterValue Parameter value to set.
     */
    private void enableFilter(final Session session, final String filterName, final String parameterName,
                              final UUID parameterValue) {
        if (Objects.nonNull(parameterValue)) {
            // Enable the filter and set the parameter value.
            session.enableFilter(filterName)
                    .setParameter(parameterName, parameterValue);
            // Is it enabled?
            final Filter filter = session.getEnabledFilter(filterName);
            TenantHibernateFilterAspect.LOGGER.trace("Hibernate filter ({}) enabled status: {}", filterName,
                                                     filter != null);
        }
    }

    private void disableTenantFilter(final Session session) {
        if (Objects.nonNull(session)) {
            try {
                session.disableFilter(AbstractTenantAwareEntity.TENANT_FILTER_NAME);
            } catch (final UnknownFilterException ufe) {
                TenantHibernateFilterAspect.LOGGER.trace("Filter {} is not available and cannot be disabled",
                                                         AbstractTenantAwareEntity.TENANT_FILTER_NAME);
            }
        }
    }

}
