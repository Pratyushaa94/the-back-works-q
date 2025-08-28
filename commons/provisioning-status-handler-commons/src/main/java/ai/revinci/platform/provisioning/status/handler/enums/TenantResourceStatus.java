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

package ai.revinci.platform.provisioning.status.handler.enums;

public enum TenantResourceStatus {
    PROVISIONING_INITIATED,
    PROVISIONING_IN_PROGRESS,
    PROVISIONING_FAILED,
    PROVISIONING_COMPLETED,
    PROVISIONING_POST_ACTIONS_INITIATED,
    PROVISIONING_POST_ACTIONS_IN_PROGRESS,
    PROVISIONING_POST_ACTIONS_FAILED,
    PROVISIONING_POST_ACTIONS_COMPLETED,
    ACTIVE,
    SHUTDOWN_INITIATED,
    SHUTDOWN_IN_PROGRESS,
    SHUTDOWN_FAILED,
    SHUTDOWN_COMPLETED,
    DEACTIVATED;

    /**
     * Maps the resource status to the tenant status.
     *
     * @param resourceStatus Tenant resource status.
     *
     * @return Tenant status.
     */
    public static TenantStatus map(final TenantResourceStatus resourceStatus) {
        return switch (resourceStatus) {
            case PROVISIONING_INITIATED, PROVISIONING_IN_PROGRESS, PROVISIONING_COMPLETED,
                 PROVISIONING_POST_ACTIONS_INITIATED, PROVISIONING_POST_ACTIONS_IN_PROGRESS ->
                    TenantStatus.PROVISIONING_IN_PROGRESS;
            case PROVISIONING_POST_ACTIONS_COMPLETED -> TenantStatus.PROVISIONING_COMPLETED;
            case PROVISIONING_FAILED, PROVISIONING_POST_ACTIONS_FAILED -> TenantStatus.PROVISIONING_FAILED;
            case ACTIVE -> TenantStatus.ACTIVE;
            case SHUTDOWN_INITIATED, SHUTDOWN_IN_PROGRESS, SHUTDOWN_COMPLETED -> TenantStatus.DEACTIVATION_IN_PROGRESS;
            case SHUTDOWN_FAILED -> TenantStatus.DEACTIVATION_FAILED;
            case DEACTIVATED -> TenantStatus.DEACTIVATED;
            default -> TenantStatus.NEW;
        };
    }
}
