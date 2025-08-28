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

package ai.revinci.platform.provisioning.db.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.exception.ServiceException;
import ai.revinci.platform.common.log.Instrumentation;
import ai.revinci.platform.provisioning.db.error.DBProvisioningErrors;
import liquibase.command.CommandResults;
import liquibase.command.CommandScope;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.exception.CommandExecutionException;

@Slf4j
@Service
public class LiquibaseService {
    private static final String CREATE_SCHEMA_SQL = "CREATE SCHEMA IF NOT EXISTS \"{0}\"";

    @Value("${spring.liquibase.change-log}")
    private String changeLogPath;

    @Instrumentation
    public void populateDatabase(final String jdbcUrl, final String schema, final String username,
                                 final byte[] password) {
        LiquibaseService.LOGGER.info("Executing liquibase update for the database.");
        try {
            // Create the schema if it is not the default schema.
            if (!PlatformDefaults.SCHEMA_PUBLIC.value()
                    .equals(schema)) {
                LiquibaseService.LOGGER.info("Attempting to create postgresql schema: {}", schema);
                createSchema(jdbcUrl, schema, username, password);
            }

            // Now, execute the liquibase script.
            final CommandScope cs = new CommandScope(PlatformDefaults.LIQUIBASE_COMMAND_UPDATE.value());
            final CommandResults results = cs.addArgumentValue(DbUrlConnectionArgumentsCommandStep.URL_ARG, jdbcUrl)
                    .addArgumentValue("schemaName", schema)
                    .addArgumentValue(DbUrlConnectionArgumentsCommandStep.USERNAME_ARG,
                                      username)
                    .addArgumentValue(DbUrlConnectionArgumentsCommandStep.PASSWORD_ARG,
                                      new String(password))
                    .addArgumentValue(PlatformDefaults.CHANGELOG_FILE.value(), changeLogPath)
                    .execute();

            LiquibaseService.LOGGER.info("Liquibase update executed successfully.");
            final Map<String, Object> resultMap = results.getResults();
            for (final Map.Entry<String, Object> entry : resultMap.entrySet()) {
                LiquibaseService.LOGGER.debug("Key: {}, Value: {}", entry.getKey(), entry.getValue());
            }
        } catch (final CommandExecutionException cee) {
            LiquibaseService.LOGGER.error("Failures encountered while executing liquibase commands", cee);
            throw ServiceException.of(DBProvisioningErrors.LIQUIBASE_EXECUTION_FAILED, cee.getMessage());
        }
    }

    private void createSchema(final String jdbcUrl, final String schema, final String username, final byte[] password) {
        Connection connection = null;
        Statement statement = null;

        try {
            // Establish a connection to the database
            connection = DriverManager.getConnection(jdbcUrl, username, new String(password));
            // Create a statement to execute the SQL command
            statement = connection.createStatement();
            // SQL command to create the schema
            final String createSchemaSQL = MessageFormat.format(LiquibaseService.CREATE_SCHEMA_SQL, schema);
            // Execute the SQL command
            statement.executeUpdate(createSchemaSQL);

            LiquibaseService.LOGGER.info("Successfully created the schema: {}", schema);
        } catch (final Exception e) {
            LiquibaseService.LOGGER.error(e.getMessage(), e);
            throw ServiceException.of(DBProvisioningErrors.LIQUIBASE_SCHEMA_CREATION_FAILED, e.getMessage());
        } finally {
            // Close the statement and connection
            try {
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (final SQLException e) {
                LiquibaseService.LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
