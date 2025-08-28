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

package ai.revinci.platform.data.jpa.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ResourceNotFoundException;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.data.jpa.persistence.IEntity;
import ai.revinci.platform.data.jpa.persistence.ISoftDeletable;


/**
 * Base Repository interface that provides common behaviors.
 *
 * @param <T>  Type of the entity.
 * @param <ID> Type of the primary key.
 */
@NoRepositoryBean
public interface ExtendedJpaRepository<T extends IEntity<ID>, ID extends Serializable> extends JpaRepository<T, ID> {
    /**
     * The method is overridden and JPQL is provided to enable the ability of including dynamic filters
     * ({@code tenantFilter} in the query.
     *
     * @param id must not be {@literal null}.
     *
     * @return An {@link Optional} wrapping the matching entity instance if found. If the entity is not found, an empty
     *         {@link Optional} is returned.
     */
    @Query(value = """
            SELECT e FROM #{#entityName} e WHERE e.id = :id
            """)
    @Override
    Optional<T> findById(ID id);

    /**
     * This method attempts to find an entity based on the provided {@code id} parameter. If the resource is not found,
     * an exception is thrown.
     *
     * @param id Unique identifier of the entity that needs to be retrieved.
     *
     * @return Matching entity instance if found else an exception is thrown.
     */
    default T findByIdOrThrow(final ID id) {
        return findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * This method attempts to find an entity based on the provided {@code example} parameter. If the resource is not
     * found, an exception is thrown.
     *
     * @param example Example instance based on which the resource needs to be found (leverages QBE - Query By
     *                Example).
     *
     * @return Matching entity instance if found else an exception is thrown.
     */
    default T findOneOrThrow(final Example<T> example) {
        return findOne(example).orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * This method attempts to find the entities for the provided identifiers. An exception is thrown in situations
     * where any of the provided identifiers do not exist in the system.
     *
     * @param ids Collection of unique identifiers of the entities that needs to be retrieved from the system.
     *
     * @return Map containing the matching entity instances for the provided identifiers. The key in the map is the
     *         unique identifier of the entity and the value is an instance of type T.
     */
    default Iterable<T> findAllOrThrow(final Collection<ID> ids) {
        // 1. Find all the entities with the provided identifiers.
        final Iterable<T> entities = findAllById(ids);

        // 2. If we were not able to find any ids, throw an exception.
        if (StreamSupport.stream(entities.spliterator(), false)
                .count() != ids.size()) {
            throw ServiceException.of(CommonErrors.RESOURCES_NOT_FOUND);
        }

        return entities;
    }

    /**
     * This method attempts to delete the object from the database.
     * <p>
     * The delete mechanism depends on the type of the entity i.e. if the type of the entity is {@link ISoftDeletable},
     * the entity is marked as deleted (by setting the "deleted" flag to true on the entity).
     * <p>
     * Likewise, if the type of the entity is not {@link ISoftDeletable}, then the entity is deleted from the system
     * i.e. hard deleted and there will be no reference to this entity subsequent to the successful deletion.
     *
     * @param id Unique identifier of the object that needs to be deleted (i.e. soft-deleted or hard-deleted based on
     *           the type of the entity).
     *
     * @return Unique identifier of the object that was deleted.
     */
    @SuppressWarnings("rawtypes")
    default ID deleteOne(final ID id) {
        return deleteOneOrThrow(id, entity -> Boolean.TRUE);
    }

    /**
     * This method attempts to delete the object from the database.
     * <p>
     * The delete mechanism depends on the type of the entity i.e. if the type of the entity is {@link ISoftDeletable},
     * the entity is marked as deleted (by setting the "deleted" flag to true on the entity).
     * <p>
     * Likewise, if the type of the entity is not {@link ISoftDeletable}, then the entity is deleted from the system
     * i.e. hard deleted and there will be no reference to this entity subsequent to the successful deletion.
     *
     * @param id                   Unique identifier of the object that needs to be deleted (i.e. soft-deleted or
     *                             hard-deleted based on the type of the entity).
     * @param executePreConditions Function that gets called before the action deletion happens. Any pre-processing work
     *                             can be done and a boolean can be returned to indicate if the deletion can proceed or
     *                             not.
     *
     * @return Unique identifier of the object that was deleted.
     */
    @SuppressWarnings("rawtypes")
    default ID deleteOneOrThrow(final ID id, final Function<T, Boolean> executePreConditions) {
        // 1. Validate the provided identifier.
        if (Objects.isNull(id)) {
            throw ServiceException.of(CommonErrors.ILLEGAL_ARGUMENT_DETAILED, Key.ID.value());
        }

        // 2. Check if the entity exists in the system.
        final T entity = findById(id).orElseThrow(
                () -> ServiceException.of(CommonErrors.RESOURCE_NOT_FOUND_DETAILED, Key.ID.value(), id));

        // 3. If pre-processing hook is specified, delegate to it before the deletion is done.
        boolean canProceed = true;
        if (Objects.nonNull(executePreConditions)) {
            // Delegate to the hook for any pre-processing actions.
            canProceed = executePreConditions.apply(entity);
        }

        if (canProceed) {
            // 4. Perform the delete based on whether the entity supports soft-delete or hard-delete.
            if (entity instanceof ISoftDeletable e) {
                e.setDeleted(true);
                e.setDeletedDate(System.currentTimeMillis());
                save(entity);
            } else {
                delete(entity);
            }
        }
        return id;
    }
}
