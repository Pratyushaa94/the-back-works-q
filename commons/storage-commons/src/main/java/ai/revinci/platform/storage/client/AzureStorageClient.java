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

package ai.revinci.platform.storage.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.storage.error.StorageErrors;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureStorageClient implements IStorageClient {

    private final BlobServiceClient blobServiceClient;

    @Instrumentation
    @Override
    public void upload(@NonNull final String bucketName, final String bucketFolderPath,
                       @NonNull final Path fileToUpload, final Map<String, String> metadata) {
        try {
            // 1. Validate the input arguments.
            if (StringUtils.isBlank(bucketName)) {
                AzureStorageClient.LOGGER.error("Container name cannot blank / empty");
                throw ServiceException.of(StorageErrors.INVALID_BUCKET_NAME);
            }

            // 2. Build the folder path.
            final File file = fileToUpload.toFile();
            final String blobName = StringUtils.isBlank(bucketFolderPath) ?
                    file.getName() :
                    bucketFolderPath.concat(Token.FORWARD_SLASH.value())
                            .concat(file.getName());

            // 3. Get the blob client for the specific blob
            final BlobClient blobClient = blobServiceClient.getBlobContainerClient(bucketName)
                    .getBlobClient(blobName);

            // 4. Upload the file to the blob including the metadata.
            AzureStorageClient.LOGGER.debug("Uploading file to container {}, blob {}", bucketName, blobName);

            // Set blob HTTP headers (optional - can be customized based on file type)
            final BlobHttpHeaders headers = new BlobHttpHeaders()
                    .setContentType("application/octet-stream");

            // Upload with metadata
            blobClient.uploadFromFile(fileToUpload.toString(), true);

            // Set metadata if provided
            final Map<String, String> finalMetadata = Optional.ofNullable(metadata)
                    .orElse(Collections.emptyMap());
            if (!finalMetadata.isEmpty()) {
                blobClient.setMetadata(finalMetadata);
            }

            // Set HTTP headers
            blobClient.setHttpHeaders(headers);

            AzureStorageClient.LOGGER.info("Successfully uploaded file to container {}, blob {}", bucketName, blobName);

        } catch (final Exception ex) {
            AzureStorageClient.LOGGER.error("Failed to upload file to container {}, path {}: {}", bucketName, bucketFolderPath, ex.getMessage(), ex);
            throw ServiceException.of(StorageErrors.FAILED_TO_UPLOAD_FILE_TO_BUCKET, bucketName, bucketFolderPath, ex.getMessage());
        }
    }

    @Override
    public String getSignedURL(@NonNull final String bucketName, @NonNull final String fileName,
                               @NonNull final Long expiration, @NonNull final TimeUnit timeUnit) {
        try {
            AzureStorageClient.LOGGER.info("Attempting to generate signed URL for blob {}/{}", bucketName, fileName);

            // 1. Get the blob client
            final BlobClient blobClient = blobServiceClient.getBlobContainerClient(bucketName)
                    .getBlobClient(fileName);

            // 2. Set permissions for the SAS token (read access)
            final BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);

            // 3. Calculate expiration time
            final OffsetDateTime expiryTime = OffsetDateTime.now().plus(expiration, convertTimeUnitToChronoUnit(timeUnit));

            // 4. Create SAS signature values
            final BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, sasPermission);

            // 5. Generate the signed URL
            final String signedUrl = blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasSignatureValues);

            AzureStorageClient.LOGGER.debug("Successfully generated signed URL for blob {}/{}", bucketName, fileName);

            return signedUrl;

        } catch (final Exception ex) {
            AzureStorageClient.LOGGER.error("Failed to generate signed URL for {}/{}: {}", bucketName, fileName, ex.getMessage(), ex);
            throw ServiceException.of(StorageErrors.FAILED_TO_GENERATE_SIGNED_URL, bucketName, fileName, ex.getMessage());
        }
    }

    /**
     * Converts TimeUnit to ChronoUnit for Azure SDK compatibility.
     */
    private java.time.temporal.ChronoUnit convertTimeUnitToChronoUnit(final TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS -> java.time.temporal.ChronoUnit.NANOS;
            case MICROSECONDS -> java.time.temporal.ChronoUnit.MICROS;
            case MILLISECONDS -> java.time.temporal.ChronoUnit.MILLIS;
            case SECONDS -> java.time.temporal.ChronoUnit.SECONDS;
            case MINUTES -> java.time.temporal.ChronoUnit.MINUTES;
            case HOURS -> java.time.temporal.ChronoUnit.HOURS;
            case DAYS -> java.time.temporal.ChronoUnit.DAYS;
        };
    }
}
