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
 *
 */

package ai.revinci.platform.common.enums;

import java.text.MessageFormat;

import lombok.AllArgsConstructor;

/**
 * An enumerated data type that provides a value pattern for various keys.
 * <p>
 * This enum is used to define specific value patterns that can be formatted with parameters.
 * </p>
 *
 * @author Subbu
 */
@AllArgsConstructor
public enum PatternTemplate implements IEnumValueProvider{

    AUTHORITY("ROLE_{0}"),
    CACHE_KEY_REVOKED_TOKEN("revokedToken:{0}"),
    CACHE_KEY_TENANT_DB_POST_PROVISIONING("tenantDbPostProvisioning:{0}"),
    DB_SEARCH_PATH("SET search_path TO \"{0}\""),
    JDBC_URL("jdbc:postgresql://{0}:{1,number,#}/{2}?currentSchema=\"{3}\""),
    // {0} is tenant realm and {1} is the actual key.
    TENANT_CACHE_KEY_TEMPLATE("{0}:{1}");

    /** Value for this enum constant. */
    private final String value;

    @Override
    public String value() {
        return value;
    }

    public String format(final Object... args) {
        return MessageFormat.format(value, args);
    }
}
