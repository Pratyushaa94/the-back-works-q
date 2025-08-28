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

package ai.revinci.platform.services.tenant.data.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import ai.revinci.platform.services.tenant.data.model.experience.storage.CreateFileStorageConnectionRequest;
import ai.revinci.platform.services.tenant.data.model.experience.storage.FileStorageConnectionResponse;
import ai.revinci.platform.tenant.data.jpa.persistence.storage.FileStorageConnectionEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface FileStorageConnectionMapper {

    /**
     * Transforms a {@link CreateFileStorageConnectionRequest} to a {@link FileStorageConnectionEntity}.
     *
     * @param sorce the request to transform
     *
     * @return the transformed entity
     */
    @Mapping(source = "storageCloudId", target = "storageCloud.id")
    FileStorageConnectionEntity transform(CreateFileStorageConnectionRequest sorce);

    /**
     * Transforms a {@link FileStorageConnectionEntity} to a {@link FileStorageConnectionResponse}.
     *
     * @param source the request to transform
     *
     * @return the transformed entity
     */
    @Mapping(source = "storageCloud.id", target = "storageCloudId")
    FileStorageConnectionResponse transform(FileStorageConnectionEntity source);
}