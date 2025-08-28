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
package ai.revinci.platform.common.tenant.policy;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * An experience model (DTO) to capture the password policies for the tenant realm.
 *
 * @author Subbu
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class TenantPasswordPolicy {
    /** Minimum length of the password. */
    private int length;

    /** Maximum length of the password. */
    private int maxLength;

    /** Number of days before the password expires. */
    private int expirationDays;

    /** Number of previous passwords that cannot be used during password change process. */
    private int passwordHistory;

    /** Number of special characters that must be present in the password. */
    private int numberOfSpecialCharacters;

    /** Number of uppercase characters that must be present in the password. */
    private int numberOfUpperCaseCharacters;

    /** Number of lowercase characters that must be present in the password. */
    private int numberOfLowerCaseCharacters;

    /** Number of digits that must be present in the password. */
    private int numberOfDigits;
}
