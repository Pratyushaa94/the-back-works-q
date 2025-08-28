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

package ai.revinci.platform.common.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PlatformDefaults implements IEnumValueProvider {
    AZP_ADMIN_CLI("admin-cli"),
    BEARER("Bearer"),
    CHANGELOG_FILE("changeLogFile"),
    HEADER("header"),
    KEYCLOAK_CLIENT_ACCOUNT_CONSOLE("account-console"),
    KEYCLOAK_CLIENT_ADMIN_CONSOLE("security-admin-console"),
    LIQUIBASE_COMMAND_UPDATE("update"),
    REVINCI_AI_EMAIL_DOMAIN("revinci.ai"),
    POSTGRESQL_DRIVER_CLASSNAME("org.postgresql.Driver"),
    POSTGRESQL_USERNAME("postgres"),
    REALM_MASTER("master"),
    REALM_RVC("revinci"),
    ROLE_ADMIN("admin"),
    ROLE_ADMIN_DESCRIPTION("${role_admin}"),
    ROLE_SUPER_ADMIN("super_admin"),
    ROLE_SUPER_ADMIN_DESCRIPTION("${role_super_admin}"),
    SCHEMA_PUBLIC("public"),
    TENANT_HEADER("Tenant-Id"),
    TENANT_HEADER_DESCRIPTION("Realm identifier"),
    TIME_ZONE_HEADER("X-User-TimeZone"),
    UNKNOWN_REFERER("Unknown");

    /** Value for this enum constant. */
    private final String value;

    @Override
    public String value() {
        return value;
    }
}
