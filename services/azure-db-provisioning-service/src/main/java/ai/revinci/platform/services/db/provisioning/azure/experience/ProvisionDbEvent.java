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

package ai.revinci.platform.services.db.provisioning.azure.experience;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class ProvisionDbEvent {
    /** Insert identifier. */
    private String insertId;

    /** Timestamp of the event. */
    private String timestamp;

    /** Severity of the event. Example: NOTICE, INFO, WARNING, ALERT, etc. */
    private String severity;

    /** Payload. */
    private ProtoPayload protoPayload;

    /** Operation details. */
    private Operation operation;

    /** Timestamp indicating when the event was received. */
    private String receiveTimestamp;

    /**
     * Payload details.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class ProtoPayload {
        /** Method name. */
        private String methodName;

        /** Resource name that contains the project and database instance name. */
        private String resourceName;

        /** Request details. */
        private Request request;

        /** Response details. */
        private Response response;
    }

    /**
     * Request class.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class Request {
        /** Project id. */
        private String project;

        /** Request body. */
        private Body body;
    }

    /**
     * Request class.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class Body {
        /** Database instance name. */
        private String name;
    }

    /**
     * Response class.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class Response {
        /** Operation identifier. */
        private String name;

        /** Operation type. Example: UPDATE. */
        private String operationType;

        /** Operation status. Example: PENDING. */
        private String status;

        /** Database instance name. */
        private String targetId;

        /** Project identifier. */
        private String targetProject;
    }

    /**
     * Operation class.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class Operation {
        /** Operation identifier. */
        private String id;

        /** Producer of the event. Typically, this is: cloudsql.googleapis.com. */
        private String producer;

        /** Is this the first operation. */
        private boolean first;

        /** Is this the last operation. */
        private boolean last;
    }
}
