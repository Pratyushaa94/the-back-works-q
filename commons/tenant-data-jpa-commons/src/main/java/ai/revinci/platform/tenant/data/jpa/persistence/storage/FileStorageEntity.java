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
package ai.revinci.platform.tenant.data.jpa.persistence.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.data.jpa.persistence.AbstractUUIDEntity;

/**
 * ORM entity for {@code storage_cloud} table.

 */
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = FileStorageEntity.TABLE_NAME)
@NoArgsConstructor
public class FileStorageEntity extends AbstractUUIDEntity {
    /** Name of the table in the database. */
    public static final String TABLE_NAME = "storage_cloud";

    /** The name of the storage cloud. This is a unique identifier for the cloud storage service. */
    @ToString.Include
    @Column(name = "cloud_name", length = 100, nullable = false, unique = true)
    private String cloudName;

    /** The display name for the cloud storage provider, used for user-friendly identification. */
    @ToString.Include
    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    /** The type of the storage provider, such as "AWS", "Azure", "Google Cloud", etc. */
    @Column(name = "provider_type", length = 50, nullable = false)
    private String providerType;

    /** The URL used for authentication with the cloud storage provider. */
    @Column(name = "auth_url", length = 500)
    private String authUrl;

    /** The URL used to obtain access tokens from the cloud storage provider. */
    @Column(name = "token_url", length = 500)
    private String tokenUrl;

    /** The URL used to refresh access tokens with the cloud storage provider. */
    @Column(name = "refresh_url", length = 500)
    private String refreshUrl;

    /** The OAuth scopes required for accessing the cloud storage provider's resources. */
    @Column(name = "scopes", columnDefinition = "TEXT")
    private String scopes;

    /** The response type expected from the OAuth provider, such as "code" or "token". */
    @Column(name = "response_type", length = 50)
    private String responseType;

    /** The response mode for the OAuth provider, such as "query" or "form_post". */
    @Column(name = "response_mode", length = 50)
    private String responseMode;

    /** The base API URL for the cloud storage provider, used for making API requests. */
    @Column(name = "base_api_url", length = 500)
    private String baseApiUrl;

    /** The version of the API used by the cloud storage provider. */
    @Column(name = "api_version", length = 20)
    private String apiVersion;

    /** The endpoint for listing files in the cloud storage. */
    @Column(name = "list_files_endpoint", length = 200)
    private String listFilesEndpoint;

    /** The endpoint for uploading files to the cloud storage. */
    @Column(name = "file_metadata_endpoint", length = 200)
    private String fileMetadataEndpoint;

    /** The endpoint for downloading files from the cloud storage. */
    @Column(name = "download_endpoint", length = 200)
    private String downloadEndpoint;

    /** The rate limit for API calls, specified in requests per hour. */
    @Column(name = "rate_limit_per_hour")
    private Integer rateLimitPerHour = 1000;

    /** The maximum number of results returned per page when listing files. */
    @Column(name = "max_results_per_page")
    private Integer maxResultsPerPage = 100;

    /** The ID of the root folder in the cloud storage, used for organizing files. */
    @Column(name = "root_folder_id", length = 255)
    private String rootFolderId;

    /** Indicates whether the cloud storage supports folder paths. */
    @Column(name = "supports_folder_path")
    private Boolean supportsFolderPath = true;

    /** The path separator used in the cloud storage, typically "/" or "\". */
    @Column(name = "path_separator", length = 5)
    private String pathSeparator = "/";
}

