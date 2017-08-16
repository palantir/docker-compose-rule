/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose.connection.waiting.jdbc;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.logging.LogDirectory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JdbcContainerHealthCheckIntegrationTest {

    private static final String DATABASE_NAME = "foobar";
    private static final String USERNAME = "person";
    private static final String PASSWORD = "abc123";

    private static final String MYSQL_CONTAINER_NAME = "mysql";
    private static final String MYSQL_COMPOSE_FILE = "src/test/resources/jdbc/mysql-docker-compose.yaml";

    private static final String POSTGRES_CONTAINER_NAME = "postgresql";
    private static final String POSTGRES_COMPOSE_FILE = "src/test/resources/jdbc/postgresql-docker-compose.yaml";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static Driver deregisterDriver(Driver driver) {
        try {
            DriverManager.deregisterDriver(driver);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to deregister driver", e);
        }
        return driver;
    }

    private static List<Driver> deregisterDrivers() {
        return Collections.list(DriverManager.getDrivers())
                .stream()
                .peek(JdbcContainerHealthCheckIntegrationTest::deregisterDriver)
                .collect(Collectors.toList());
    }

    private static void registerDriver(Driver driver) {
        try {
            DriverManager.registerDriver(driver);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to register driver", e);
        }
    }

    private static DockerComposeRule generateRule(String containerName, String composeFile,
            JdbcContainerConfiguration configuration) {
        return DockerComposeRule.builder()
                .file(composeFile)
                .saveLogsTo(LogDirectory
                        .circleAwareLogDirectory(JdbcContainerHealthCheckIntegrationTest.class))
                .waitingForService(containerName, JdbcContainerHealthCheck.of(configuration))
                .build();
    }

    private static void assertCanConnectJdbc(JdbcContainerConfiguration configuration, Container target) {
        try (Connection connection = configuration.openConnection(target)) {
            assertThat(connection.isValid(0), is(true));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void should_run_and_connect_mysql() throws IOException, InterruptedException {
        JdbcContainerConfiguration jdbcContainer = JdbcContainerConfiguration
                .ofMySql(DATABASE_NAME, USERNAME, PASSWORD);
        DockerComposeRule rule = generateRule(MYSQL_CONTAINER_NAME, MYSQL_COMPOSE_FILE, jdbcContainer);
        rule.before();
        assertThat(rule.containers().container(MYSQL_CONTAINER_NAME)
                .port(KnownSubProtocols.MYSQL.defaultPort())
                .isListeningNow(), is(true));
        assertCanConnectJdbc(jdbcContainer, rule.containers().container(MYSQL_CONTAINER_NAME));
        rule.after();
    }

    @Test
    public void should_run_and_connect_postgres() throws IOException, InterruptedException {
        JdbcContainerConfiguration jdbcContainer = JdbcContainerConfiguration
                .ofPostgresql(DATABASE_NAME, USERNAME, PASSWORD);
        DockerComposeRule rule = generateRule(POSTGRES_CONTAINER_NAME, POSTGRES_COMPOSE_FILE, jdbcContainer);
        rule.before();
        assertThat(rule.containers().container(POSTGRES_CONTAINER_NAME)
                .port(KnownSubProtocols.POSTGRESQL.defaultPort())
                .isListeningNow(), is(true));
        assertCanConnectJdbc(jdbcContainer, rule.containers().container(POSTGRES_CONTAINER_NAME));
        rule.after();
    }

    @Test
    public void should_fail_fast_if_no_driver_can_handle_config() {
        List<Driver> jdbcDrivers = deregisterDrivers();
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Unable to handle jdbc url"));
        try {
            // Any config will do
            JdbcContainerConfiguration.ofPostgresql(DATABASE_NAME, USERNAME, PASSWORD);
        } finally {
            jdbcDrivers.forEach(JdbcContainerHealthCheckIntegrationTest::registerDriver);
        }
    }

}
