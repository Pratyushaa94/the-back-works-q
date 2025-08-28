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

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.services.tenant.data.mapper.FileStorageMapper;
import ai.revinci.platform.services.tenant.data.model.experience.storage.FileStorageResponse;
import ai.revinci.platform.tenant.data.jpa.persistence.storage.FileStorageEntity;
import ai.revinci.platform.tenant.data.jpa.repository.FileStorageRepository;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class FileStorageService {
    /** Repository for File Storage Cloud Providers. */
    private final FileStorageRepository fileStorageRepository;

    /** Mapper for transforming File Storage Cloud Providers to response model. */
    private final FileStorageMapper fileStorageMapper;

    /**
     * Get all file storage cloud providers.
     *
     * @return List of FileStorageResponse
     */
    @Instrumentation
    @Transactional(readOnly = true)
    public List<FileStorageResponse> getAllFileStorageProviders() {
        // Fetch all file storage entities from the repository
        final List<FileStorageEntity> fileStorageResponses = fileStorageRepository.findAll();

        // Transform the entities to response model using the mapper and return the list
        return fileStorageResponses.stream()
                .map(fileStorageMapper::transform)
                .toList();
    }
}

