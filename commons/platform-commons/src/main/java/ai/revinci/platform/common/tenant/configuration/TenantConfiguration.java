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
package ai.revinci.platform.common.tenant.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import ai.revinci.platform.common.tenant.policy.TenantPasswordPolicy;
import ai.revinci.platform.common.tenant.policy.UserRegistrationPolicy;

/**
 * An experience model that is used to capture the configuration details of a tenant.
 * <p>
 * The configuration details can cover the below aspects:
 *
 * <ul>
 *     <li>Password policies for a tenant.</li>
 *     <li>Subscription details.</li>
 *     <li>Thresholds / limits for a tenant.</li>
 * </ul>
 *
 * @author Subbu
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class TenantConfiguration {
    /** Password policy for the tenant. */
    private ai.revinci.platform.common.tenant.policy.TenantPasswordPolicy passwordPolicy;

    /** User registration policy for the tenant. */
    private UserRegistrationPolicy userRegistrationPolicy;

}
