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

package ai.revinci.platform.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;

@Slf4j
public final class FileUtils {
    /**
     * Private constructor.
     */
    private FileUtils() {
        throw new IllegalStateException("Cannot create instance of this class");
    }

    /**
     * This method attempts to read the {@code fileName} available on the classpath.
     *
     * @param fileName File name available on the classpath.
     *
     * @return The content of the file as a {@link String}.
     */
    public static String readFile(final String fileName) {
        return FileUtils.readFile(fileName, null);
    }

    /**
     * This method attempts to read the {@code fileName} available on the classpath using the provided
     * {@code classLoader}.
     *
     * @param fileName    File name available on the classpath.
     * @param classLoader Class loader to use for reading the file. If null, the default classloader is used.
     *
     * @return The content of the file as a {@link String}.
     */
    public static String readFile(final String fileName, final ClassLoader classLoader) {
        try {
            final ClassPathResource resource;
            if (Objects.isNull(classLoader)) {
                resource = new ClassPathResource(fileName);
            } else {
                resource = new ClassPathResource(fileName, classLoader);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            FileUtils.LOGGER.error("Error while reading the file. Error message : {}", e.getMessage(), e);
            throw ServiceException.of(CommonErrors.JSON_READ_FAILED, fileName);
        }
    }

    /**
     * This method reads the content of the provided {@code inputFile} and returns it as a {@link String}.
     *
     * @param inputFile The input file to read.
     *
     * @return The content of the file as a {@link String}.
     */
    public static String readFileContent(final String inputFile) {
        // 1. Does the path exist?
        final File file = Paths.get(inputFile)
                .toFile();
        if (!file.exists()) {
            FileUtils.LOGGER.error("The input file {} does not exist", inputFile);
            throw ServiceException.of(CommonErrors.FILE_DOES_NOT_EXIST, inputFile);
        }

        // 2. Read the file-content as a String and return.
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            FileUtils.LOGGER.error("Error while reading the input file. Error message : {}", e.getMessage(), e);
            throw ServiceException.of(CommonErrors.FAILED_TO_READ_FILE, inputFile);
        }
    }

    /**
     * This method writes the provided {@code dataBlob} to a file in the provided {@code directory}.
     *
     * @param dataBlob  Data blob that needs to be written to a file whose name is generated dynamically.
     * @param directory Directory where the file needs to be written.
     *
     * @return The path of the file where the data-blob is written.
     *
     * @throws IOException If an error occurs while writing the data-blob to the file.
     */
    public static Path writeDataBlob(final String dataBlob, final Path directory) throws IOException {
        return FileUtils.writeDataBlob(dataBlob, directory, UUID.randomUUID()
                .toString());
    }

    /**
     * This method writes the provided {@code dataBlob} to a file in the provided {@code directory} with the provided
     * {@code fileName}.
     *
     * @param dataBlob  Data blob that needs to be written to a file.
     * @param directory Directory where the file needs to be written.
     * @param fileName  Name of the file where the data-blob needs to be written.
     *
     * @return The path of the file where the data-blob is written.
     *
     * @throws IOException If an error occurs while writing the data-blob to the file.
     */
    public static Path writeDataBlob(final String dataBlob, final Path directory, final String fileName)
            throws IOException {
        // Decode the data-blob
        final byte[] decodedBytes = Base64.getDecoder()
                .decode(dataBlob.split(Token.COMMA.value())[1]);
        // Create the directories
        Files.createDirectories(directory);

        // Write the data-blob to the file
        final Path filePath = directory.resolve(fileName);
        Files.write(filePath, decodedBytes);

        return filePath;
    }

    /**
     * This method attempts to determine the file size of the provided {@code file}.
     *
     * @param file File whose size needs to be determined.
     *
     * @return Size of the file in bytes.
     */
    public static long getFileSize(final Path file) {
        long fileSize = 0L;
        try {
            fileSize = Files.size(file);
        } catch (final IOException ioe) {
            FileUtils.LOGGER.error("Failed to retrieve the file size. Error: {}", ioe.getMessage());
        }

        return fileSize;
    }

    /**
     * This method writes the provided {@code content} to a temporary file with the provided {@code fileName}.
     *
     * @param content  Content that needs to be written to the file.
     * @param fileName Name of the file where the content needs to be written.
     *
     * @return A {@link Optional} of {@link Path} object representing the path of the file where the content is written.
     *         If an error occurs while writing the content to the file, an empty {@link Optional} is returned.
     */
    public static Optional<Path> writeToTempFile(final String content, final String fileName) {
        try {
            return Optional.of(FileUtils.writeToTempFileOrThrow(content, fileName));
        } catch (final IOException ioe) {
            FileUtils.LOGGER.error("Failed to write the content to the temp file. Error: {}", ioe.getMessage());
            return Optional.empty();
        }
    }

    /**
     * This method writes the provided {@code content} to a temporary file with the provided {@code fileName}.
     *
     * @param content  Content that needs to be written to the file.
     * @param fileName Name of the file where the content needs to be written.
     *
     * @return A {@link Path} object representing the path of the file where the content is written.
     *
     * @throws IOException If an error occurs while writing the content to the file.
     */
    public static Path writeToTempFileOrThrow(final String content, final String fileName) throws IOException {
        // 1. Create a temporary directory.
        final Path tempDirectory = Files.createTempDirectory("temp");

        // 2. Create the directories
        Files.createDirectories(tempDirectory);

        // 3. Write the data-blob to the file
        final Path filePath = tempDirectory.resolve(fileName);
        org.apache.commons.io.FileUtils.writeStringToFile(filePath.toFile(), content, StandardCharsets.UTF_8);

        return filePath;
    }
}

