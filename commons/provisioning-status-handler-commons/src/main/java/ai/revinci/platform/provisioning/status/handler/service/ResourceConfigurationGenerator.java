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

package ai.revinci.platform.provisioning.status.handler.service;

import java.text.MessageFormat;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.passay.PasswordGenerator;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.EnvironmentVariable;
import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.PatternTemplate;
import ai.revinci.platform.common.password.PasswordPolicy;
import ai.revinci.platform.common.util.Strings;
import ai.revinci.platform.provisioning.status.handler.enums.DeploymentEnvironment;
import ai.revinci.platform.provisioning.status.handler.enums.TenantCategory;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceConfigurationGenerator {
    private static final String MASTER_PLATFORM_DB_INSTANCE_TEMPLATE_NAME = "{0}-master-rvc-platform-db";
    private static final String TENANT_PLATFORM_DB_INSTANCE_TEMPLATE_NAME = "{0}-{1}-rvc-platform-db";
    private static final String MASTER_PLATFORM_DB_TEMPLATE_NAME = "{0}_master_rvc_platform_db";
    private static final String TENANT_PLATFORM_DB_TEMPLATE_NAME = "{0}_{1}_rvc_platform_db";
    private final Environment environment;

    // Environment variable keys
    private static final String MASTER_DB_USER = "MASTER_DB_USER";
    private static final String MASTER_DB_PASSWORD = "MASTER_DB_PASSWORD";
    private static final String MASTER_DB_HOST = "MASTER_DB_HOST";
    private static final String MASTER_DB_PORT = "MASTER_DB_PORT";
    private static final String MASTER_DB_NAME = "MASTER_DB_NAME";


    private final PasswordGenerator passwordGenerator;

    public String generateTenantDbUsername(final TenantCategory tenantCategory) {
        final String defaultUsername = PlatformDefaults.POSTGRESQL_USERNAME.value();
        return TenantCategory.ENTERPRISE.is(tenantCategory) ?
                defaultUsername :
                environment.getProperty(EnvironmentVariable.MASTER_DB_USER.value(), defaultUsername);
    }

    public String generateTenantDbPassword(final TenantCategory tenantCategory, final String secret) {
        final String password = passwordGenerator.generatePassword(PasswordPolicy.getRandomPasswordLength(),
                                                                   PasswordPolicy.usingDefaults());
        final String passwordToUse = TenantCategory.ENTERPRISE.is(tenantCategory) ?
                password :
                environment.getProperty(EnvironmentVariable.MASTER_DB_PASSWORD.value(), password);
        return Strings.encrypt(passwordToUse, secret.getBytes());
    }

    public String generateTenantDbSchemaName(final TenantCategory tenantCategory, final String realm) {
        final String defaultSchemaName = PlatformDefaults.SCHEMA_PUBLIC.value();
        return TenantCategory.ENTERPRISE.is(tenantCategory) ?
                defaultSchemaName :
                realm;
    }

    public String generateTenantDbInstanceName(final DeploymentEnvironment environment,
                                               @NonNull final TenantCategory tenantCategory,
                                               @NonNull final String realm) {
        final String env = Optional.ofNullable(environment)
                .orElse(DeploymentEnvironment.LOCAL)
                .value();

        // If the tenant is an enterprise, we will provision a separate database instance and not use the master
        // instance to have tenant-specific schemas.
        if (TenantCategory.ENTERPRISE.is(tenantCategory)) {
            return MessageFormat.format(ResourceConfigurationGenerator.TENANT_PLATFORM_DB_INSTANCE_TEMPLATE_NAME, env,
                                        realm);
        }

        // All other cases, we will use the master database and add tenant-specific schemas (using realm schema).
        return MessageFormat.format(ResourceConfigurationGenerator.MASTER_PLATFORM_DB_INSTANCE_TEMPLATE_NAME, env);
    }

    public String generateTenantDbName(final DeploymentEnvironment environment,
                                       @NonNull final TenantCategory tenantCategory, @NonNull final String realm) {
        final String env = Optional.ofNullable(environment)
                .orElse(DeploymentEnvironment.LOCAL)
                .value();

        // If the tenant is an enterprise, we will provision a separate database instance and not use the master
        // instance to have tenant-specific schemas.
        if (TenantCategory.ENTERPRISE.is(tenantCategory)) {
            return MessageFormat.format(ResourceConfigurationGenerator.TENANT_PLATFORM_DB_TEMPLATE_NAME, env, realm);
        }

        // All other cases, we will use the master database and add tenant-specific schemas (using realm schema).
        return MessageFormat.format(ResourceConfigurationGenerator.MASTER_PLATFORM_DB_TEMPLATE_NAME, env);
    }


    public String generateTenantDbJdbcUrl(final TenantCategory tenantCategory, final String schema) {
        if (TenantCategory.ENTERPRISE.is(tenantCategory)) {
            return StringUtils.EMPTY;
        }

        final String dbHost = environment.getProperty(EnvironmentVariable.MASTER_DB_HOST.value());
        final String port = environment.getProperty(EnvironmentVariable.MASTER_DB_PORT.value(), "5432");
        final Integer dbPort = Integer.valueOf(port);
        final String dbName = environment.getProperty(EnvironmentVariable.MASTER_DB_NAME.value());

        return PatternTemplate.JDBC_URL.format(dbHost, dbPort, dbName, schema);
    }


}
