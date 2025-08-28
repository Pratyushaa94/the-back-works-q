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

package ai.revinci.platform.services.platform.data.model.projection;

import java.util.UUID;

public interface TenantSummary {
    /**
     * This method attempts to return the unique identifier of the tenant.
     *
     * @return unique identifier of the tenant
     */
    UUID getId();

    /**
     * This method attempts to return the realm name of the tenant.
     *
     * @return realm name of the tenant
     */
    String getRealmName();

    /**
     * This method attempts to return the name of the tenant.
     *
     * @return name of the tenant
     */
    String getName();

    /**
     * This method attempts to return the type of the tenant.
     *
     * @return type of the tenant
     */
    String getType();

    /**
     * This method attempts to return the category of the tenant.
     *
     * @return category of the tenant
     */
    String getCategory();

    /**
     * This method attempts to return the status of the tenant.
     *
     * @return status of the tenant
     */
    String getStatus();

    /**
     * This method attempts to return the industry vertical of the tenant.
     *
     * @return industry vertical of the tenant
     */
    boolean isMaster();
}