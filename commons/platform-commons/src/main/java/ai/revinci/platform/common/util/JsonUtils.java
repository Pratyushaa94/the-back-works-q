/*
 * Copyright (c) 2025 Revinci AI.
 *
 * All rights reserved. This software is proprietary to and embodies the
 * confidential technology of Revinci AI. Possession,
 * use, duplication, or dissemination of the software and media is
 * authorized only pursuant to a valid written license from
 * Revinci AI Solutions Pvt. Ltd.
 *
 * Unauthorized use of this software is strictly prohibited.
 *
 * THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Revinci AI BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ai.revinci.platform.common.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;

/**
 * Utility class that contains methods for serialization and deserialization of Objects from / to JSON.
 */
@Slf4j
public final class JsonUtils {
    /** Object Mapper instance that will be used for serialization / deserialization. */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Private constructor.
     */
    private JsonUtils() {
        throw new IllegalStateException("Cannot create instance of this class");
    }

    /**
     * Serializes the provided object to JSON format and returns a JSON string.
     * <p>
     * If an exception is encountered during the serialization, an empty string is returned.
     *
     * @param object Object that needs to be serialized to JSON.
     *
     * @return JSON string representation of the provided object.
     */
    public static String serialize(final Object object) {
        String jsonString = StringUtils.EMPTY;
        if (Objects.isNull(object)) {
            return jsonString;
        }

        try {
            jsonString = JsonUtils.OBJECT_MAPPER.writeValueAsString(object);
        } catch (final JsonProcessingException e) {
            JsonUtils.LOGGER.error(e.getMessage(), e);
        }
        return jsonString;
    }

    /**
     * This method serializes the provided payload to a JSON string. If an exception occurs, it is thrown back to the
     * caller.
     *
     * @param payload Payload that needs to be serialized.
     *
     * @return Serialized JSON string.
     */
    public static String serializeOrThrow(final Object payload) {
        try {
            return JsonUtils.OBJECT_MAPPER.writeValueAsString(payload);
        } catch (final JsonProcessingException e) {
            JsonUtils.LOGGER.error("Error while serializing the input. Error message : {}", e.getMessage(), e);
            throw ServiceException.of(CommonErrors.JSON_SERIALIZATION_FAILED);
        }
    }

    /**
     * This method de-serializes the provided JSON string to an object of type identified by {@code targetType}
     * parameter.
     *
     * @param jsonString JSON string that needs to be de-serialized.
     * @param targetType Target type that the de-serialized object needs to be cast to.
     * @param <T>        Target type.
     *
     * @return De-serialized object cast to the requested {@code targetType}.
     */
    public static <T> T deserialize(final String jsonString, final Class<T> targetType) {
        try {
            return JsonUtils.OBJECT_MAPPER.readValue(jsonString, targetType);
        } catch (final IOException e) {
            JsonUtils.LOGGER.error("Error while deserializing the input. Error message : {}", e.getMessage(), e);
            throw ServiceException.of(CommonErrors.JSON_DESERIALIZATION_FAILED);
        }
    }

    /**
     * This method de-serializes the provided JSON string to an object of type identified by {@code targetType}
     * parameter.
     *
     * @param jsonString JSON string that needs to be de-serialized.
     * @param targetType Target type that the de-serialized object needs to be cast to.
     * @param <T>        Target type.
     *
     * @return De-serialized object cast to the requested {@code targetType}.
     */
    public static <T> T deserialize(final String jsonString, final TypeReference<T> targetType) {
        try {
            return JsonUtils.OBJECT_MAPPER.readValue(jsonString, targetType);
        } catch (final IOException e) {
            JsonUtils.LOGGER.error("Error while deserializing the input. Error message : {}", e.getMessage(), e);
            throw ServiceException.of(CommonErrors.JSON_DESERIALIZATION_FAILED);
        }
    }

    /**
     * For the provided input, this method attempts to check if it is a JSON (i.e. it can be deserialized to Java
     * object).
     *
     * @param input Input that needs to be verified if it represents a JSON and can be deserialized to a Java object.
     *
     * @return True if the provided input can be deserialized to a Java object, false otherwise.
     */
    public static boolean isJson(final String input) {
        boolean verdict = false;
        if (StringUtils.isNotBlank(input)) {
            try {
                JsonUtils.OBJECT_MAPPER.readTree(input);
                verdict = true;
            } catch (final IOException ex) {
                // Ignore the error
            }
        }

        return verdict;
    }

    public static <T> T parseFileAsJson(final String inputJsonFile, final Class<T> targetType) {
        // 1. Read file content.
        final String fileContent = FileUtils.readFileContent(inputJsonFile);

        // 2. Deserialize to InputRequest
        return JsonUtils.deserialize(fileContent, targetType);
    }
}