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

package ai.revinci.platform.web.api;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.web.api.error.ApiErrors;
import ai.revinci.platform.web.utils.HttpUtils;

@Slf4j
public abstract class AbstractApi {
    protected String getClientIpAddress(final HttpServletRequest request) {
        return HttpUtils.getClientIpAddress(request);
    }
    protected String getReferer(final HttpServletRequest request) {
        return HttpUtils.getReferer(request);
    }

    protected void transferFile(final MultipartFile file, final Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            file.transferTo(destination.toFile());
        } catch (final IOException e) {
            AbstractApi.LOGGER.error(e.getMessage(), e);
            throw ServiceException.of(ApiErrors.FAILED_TO_TRANSFER_FILE, file.getOriginalFilename(),
                                      destination.toString());
        }
    }

    protected ResponseEntity<InputStreamResource> responseForFileDownload(final File fileToDownload,
                                                                          final String fileName) {
        InputStreamResource resource = null;
        if (Objects.nonNull(fileToDownload)) {
            try {
                if (fileToDownload.isFile() && fileToDownload.exists()) {
                    resource = new InputStreamResource(new FileInputStream(fileToDownload));
                } else {
                    AbstractApi.LOGGER.warn(
                            "Unable to access file {}. Either it does not exist or does not have permissions",
                            fileName);
                }
            } catch (final Exception ex) {
                AbstractApi.LOGGER.error(ex.getMessage(), ex);
                throw ServiceException.of(ApiErrors.FAILED_TO_DOWNLOAD_FILE);
            }
        }
        if (Objects.isNull(resource)) {
            return ResponseEntity.notFound()
                    .headers(initializeHeadersForFileDownload(StringUtils.EMPTY))
                    .build();
        }

        return ResponseEntity.ok()
                .headers(initializeHeadersForFileDownload(fileName))
                .contentLength(fileToDownload.length())
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(resource);
    }

    protected HttpHeaders initializeHeadersForFileDownload(final String fileName) {
        return HttpUtils.generateHeadersForFileDownload(fileName);
    }

}
