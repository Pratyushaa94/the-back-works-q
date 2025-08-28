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

import jakarta.validation.Valid;
import java.util.UUID;

import org.springframework.data.domain.Example;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.services.tenant.data.mapper.FileStorageConnectionMapper;
import ai.revinci.platform.services.tenant.data.model.experience.storage.CreateFileStorageConnectionRequest;
import ai.revinci.platform.services.tenant.data.model.experience.storage.FileStorageConnectionResponse;
import ai.revinci.platform.services.tenant.data.model.experience.storage.UpdateFileStorageConnectionRequest;
import ai.revinci.platform.services.tenant.error.TenantServiceErrors;
import ai.revinci.platform.tenant.data.jpa.persistence.storage.FileStorageConnectionEntity;
import ai.revinci.platform.tenant.data.jpa.persistence.storage.FileStorageEntity;
import ai.revinci.platform.tenant.data.jpa.repository.FileStorageConnectionRepository;
import ai.revinci.platform.tenant.data.jpa.repository.FileStorageRepository;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class FileStorageConnectionService {
    /** Repository for managing file storage connections. */
    private final FileStorageConnectionRepository connectionRepository;

    /** Repository for managing file storage connections. */
    private final FileStorageRepository fileStorageRepository;

    /** Mapper for transforming file storage connection requests and responses. */
    private final FileStorageConnectionMapper connectionMapper;

    /**
     * Create a new tenant file storage connection.
     *
     * @param request the request containing connection details
     *
     * @return the created TenantFileStorageConnectionResponse
     */
    @Instrumentation
    @Transactional
    public FileStorageConnectionResponse createConnection(@NonNull final CreateFileStorageConnectionRequest request) {
        // Validate request
        final FileStorageConnectionEntity entity = connectionMapper.transform(request);
        connectionRepository.findOne(Example.of(entity))
                .ifPresent(existing -> {
                    throw ServiceException.of(
                            TenantServiceErrors.TENANT_FILE_STORAGE_CONNECTION_ALREADY_EXISTS,
                            existing.getId(), request.getStorageCloudId());
                });

        // Save the new connection
        final FileStorageConnectionEntity saved = connectionRepository.save(entity);

        //  Return the transformed response
        return connectionMapper.transform(saved);
    }

    /**
     * Update an existing tenant file storage connection.
     *
     * @param id      the unique identifier of the file storage connection to update
     * @param request the request containing the updated connection details
     *
     * @return the updated {@link FileStorageConnectionResponse}
     */
    @Instrumentation
    @Transactional
    public FileStorageConnectionResponse updateConnection(final @Valid @NonNull UUID id,
                                                          final @Valid @NonNull UpdateFileStorageConnectionRequest request) {
        // Find the existing connection by ID
        final FileStorageConnectionEntity existing = connectionRepository.findByIdOrThrow(id);
        // Update the existing connection with new details
        existing.setAccessCode(request.getAccessCode());

        // Save the updated connection
        final FileStorageConnectionEntity updated = connectionRepository.save(existing);

        // Return the transformed response
        return connectionMapper.transform(updated);
    }

    /**
     * Retrieve a file storage connection by its storage cloud ID.
     *
     * @param storageCloudId the unique identifier of the storage cloud
     *
     * @return the file storage connection response
     */
    @Instrumentation
    @Transactional(readOnly = true)
    public FileStorageConnectionResponse getConnectionByStorageCloudId(final UUID storageCloudId) {
        // Get the file storage entity by storage cloud ID
        final FileStorageEntity fileStorageEntity = fileStorageRepository.findByIdOrThrow(storageCloudId);

        // Find the connection by storage cloud ID
        final FileStorageConnectionEntity connection = connectionRepository.findByStorageCloud(fileStorageEntity)
                .orElseThrow(() -> ServiceException.of(
                        TenantServiceErrors.TENANT_FILE_STORAGE_CONNECTION_NOT_FOUND,
                        storageCloudId));

        // Return the transformed response
        return connectionMapper.transform(connection);
    }
}

