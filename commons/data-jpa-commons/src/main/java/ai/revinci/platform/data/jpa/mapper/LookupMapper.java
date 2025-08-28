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

package ai.revinci.platform.data.jpa.mapper;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.data.jpa.model.experience.lookup.LookupValue;
import ai.revinci.platform.data.jpa.persistence.ILookupEntity;
import ai.revinci.platform.data.jpa.repository.LookupRepository;

@Slf4j
@Component
public class LookupMapper  implements ApplicationContextAware {
    /** Application context. */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public <R extends LookupRepository<T>, T extends ILookupEntity> T transform(final String name,
                                                                                final Class<R> repositoryType,
                                                                                final Supplier<ServiceException> exceptionSupplier) {
        return applicationContext.getBean(repositoryType)
                .findByCode(name)
                .orElseThrow(exceptionSupplier);
    }

    public <T extends ILookupEntity, R extends LookupRepository<T>> List<T> findAllValues(
            final Class<R> repositoryType) {
        return applicationContext.getBean(repositoryType)
                .findAll();
    }

    public <R extends LookupRepository<? extends ILookupEntity>> List<LookupValue> findAllValuesAndTransform(
            final Class<R> repositoryType) {
        return applicationContext.getBean(repositoryType)
                .findAll()
                .stream()
                .map(this::transform)
                .toList();
    }

    public LookupValue transform(@NonNull final ILookupEntity source) {
        return LookupValue.builder()
                .code(source.getCode())
                .name(source.getName())
                .description(source.getDescription())
                .build();
    }


    public List<LookupValue> transform(final List<ILookupEntity> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return List.of();
        }

        return sources.stream()
                .map(this::transform)
                .toList();
    }

}
