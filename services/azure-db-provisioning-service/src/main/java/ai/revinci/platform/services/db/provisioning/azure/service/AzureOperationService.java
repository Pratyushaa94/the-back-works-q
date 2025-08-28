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

package ai.revinci.platform.services.db.provisioning.azure.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.services.db.provisioning.azure.enums.PostgreSqlOperationStatus;
import ai.revinci.platform.services.db.provisioning.azure.error.AzureDatabaseProvisioningServiceErrors;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Server;

/**
 * Service for managing Azure PostgreSQL long-running operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureOperationService {

    private final PostgreSqlManager postgreSqlManager;

    /** Default timeout for operations in minutes. */
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;

    /** Polling interval in seconds. */
    private static final int POLLING_INTERVAL_SECONDS = 30;

    /**
     * Waits for a server creation operation to complete.
     *
     * @param resourceGroupName The resource group name
     * @param serverName The server name
     * @param timeoutMinutes Timeout in minutes
     * @return The created server
     */
    public Server waitForServerCreation(final String resourceGroupName, final String serverName, final int timeoutMinutes) {
        try {
            AzureOperationService.LOGGER.info("Waiting for server creation to complete: {}", serverName);

            final long startTime = System.currentTimeMillis();
            final long timeoutMs = TimeUnit.MINUTES.toMillis(timeoutMinutes);

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    final Server server = postgreSqlManager.servers().getByResourceGroup(resourceGroupName, serverName);

                    if (server != null && server.userVisibleState() != null) {
                        final String state = server.userVisibleState().toString();
                        AzureOperationService.LOGGER.debug("Server {} state: {}", serverName, state);

                        if ("Ready".equalsIgnoreCase(state)) {
                            AzureOperationService.LOGGER.info("Server creation completed successfully: {}", serverName);
                            return server;
                        } else if ("Failed".equalsIgnoreCase(state) || "Disabled".equalsIgnoreCase(state)) {
                            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_OPERATION_FAILED,
                                "Server creation failed with state: " + state);
                        }
                    }

                    // Wait before next poll
                    Thread.sleep(TimeUnit.SECONDS.toMillis(POLLING_INTERVAL_SECONDS));

                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_OPERATION_FAILED,
                        "Operation interrupted");
                }
            }

            // Timeout reached
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_OPERATION_TIMEOUT,
                "Server creation timed out after " + timeoutMinutes + " minutes");

        } catch (final Exception ex) {
            AzureOperationService.LOGGER.error("Error waiting for server creation: {}: {}", serverName, ex.getMessage(), ex);

            if (ex instanceof ServiceException) {
                throw ex;
            }

            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_OPERATION_FAILED, ex.getMessage());
        }
    }

    /**
     * Waits for a server deletion operation to complete.
     *
     * @param resourceGroupName The resource group name
     * @param serverName The server name
     * @param timeoutMinutes Timeout in minutes
     */
    public void waitForServerDeletion(final String resourceGroupName, final String serverName, final int timeoutMinutes) {
        try {
            AzureOperationService.LOGGER.info("Waiting for server deletion to complete: {}", serverName);

            final long startTime = System.currentTimeMillis();
            final long timeoutMs = TimeUnit.MINUTES.toMillis(timeoutMinutes);

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    final Server server = postgreSqlManager.servers().getByResourceGroup(resourceGroupName, serverName);

                    if (server == null) {
                        AzureOperationService.LOGGER.info("Server deletion completed successfully: {}", serverName);
                        return;
                    }

                    // Wait before next poll
                    Thread.sleep(TimeUnit.SECONDS.toMillis(POLLING_INTERVAL_SECONDS));

                } catch (final com.azure.core.management.exception.ManagementException e) {
                    if (e.getResponse().getStatusCode() == 404) {
                        AzureOperationService.LOGGER.info("Server deletion completed successfully: {}", serverName);
                        return;
                    }
                    throw e;
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_OPERATION_FAILED,
                        "Operation interrupted");
                }
            }

            // Timeout reached
            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_OPERATION_TIMEOUT,
                "Server deletion timed out after " + timeoutMinutes + " minutes");

        } catch (final Exception ex) {
            AzureOperationService.LOGGER.error("Error waiting for server deletion: {}: {}", serverName, ex.getMessage(), ex);

            if (ex instanceof ServiceException) {
                throw ex;
            }

            throw ServiceException.of(AzureDatabaseProvisioningServiceErrors.AZURE_POSTGRESQL_OPERATION_FAILED, ex.getMessage());
        }
    }

    /**
     * Checks if an operation has completed successfully.
     *
     * @param status The operation status
     * @return true if completed successfully, false otherwise
     */
    public boolean isOperationSuccessful(final String status) {
        return PostgreSqlOperationStatus.SUCCEEDED.value().equalsIgnoreCase(status);
    }

    /**
     * Checks if an operation has failed.
     *
     * @param status The operation status
     * @return true if failed, false otherwise
     */
    public boolean isOperationFailed(final String status) {
        return PostgreSqlOperationStatus.FAILED.value().equalsIgnoreCase(status) ||
               PostgreSqlOperationStatus.CANCELED.value().equalsIgnoreCase(status);
    }

    /**
     * Checks if an operation is still in progress.
     *
     * @param status The operation status
     * @return true if in progress, false otherwise
     */
    public boolean isOperationInProgress(final String status) {
        return PostgreSqlOperationStatus.PENDING.value().equalsIgnoreCase(status) ||
               PostgreSqlOperationStatus.IN_PROGRESS.value().equalsIgnoreCase(status);
    }

    /**
     * Executes an operation with retry logic.
     *
     * @param operation The operation to execute
     * @param maxRetries Maximum number of retries
     * @param retryDelaySeconds Delay between retries in seconds
     * @return CompletableFuture with the operation result
     */
    public <T> CompletableFuture<T> executeWithRetry(final java.util.function.Supplier<T> operation,
                                                     final int maxRetries,
                                                     final int retryDelaySeconds) {
        return CompletableFuture.supplyAsync(() -> {
            Exception lastException = null;

            for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
                try {
                    return operation.get();
                } catch (final Exception ex) {
                    lastException = ex;
                    AzureOperationService.LOGGER.warn("Operation attempt {} failed: {}", attempt, ex.getMessage());

                    if (attempt <= maxRetries) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(retryDelaySeconds));
                        } catch (final InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Operation interrupted", ie);
                        }
                    }
                }
            }

            throw new RuntimeException("Operation failed after " + (maxRetries + 1) + " attempts", lastException);
        });
    }
}
