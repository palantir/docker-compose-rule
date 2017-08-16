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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.palantir.docker.compose.connection.Container;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
public abstract class JdbcContainerConfiguration {

    private static final String JDBC_FORMAT = "jdbc:$SUB_PROTOCOL://$HOST:$EXTERNAL_PORT/$DATABASE";

    private static boolean handledByDriver(Driver driver, String jdbcUrl) {
        try {
            // Delegate to acceptsURL to determine if the url is handled by the driver
            return driver.acceptsURL(jdbcUrl);
        } catch (SQLException e) {
            return false;
        }
    }

    private static String generateNoDriversExistErrorMessage(String jdbcUrl, List<Driver> registeredDrivers) {
        StringBuilder string = new StringBuilder();
        string.append(String.format("Unable to handle jdbc url of \'%s\'", jdbcUrl));
        if (registeredDrivers.isEmpty()) {
            string.append(", no drivers found on classpath.");
        } else {
            string.append(String.format(", registered drivers found on classpath: [%s]",
                    Joiner.on(",").join(registeredDrivers.stream()
                            .map(Object::getClass)
                            .map(Class::getName)
                            .collect(Collectors.toList()))));
        }
        return string.toString();
    }

    private static void validateDriverExistsForSubProtocol(String jdbcUrl) {
        List<Driver> registeredDrivers = Collections.list(DriverManager.getDrivers());
        checkArgument(registeredDrivers.stream().anyMatch(driver -> handledByDriver(driver, jdbcUrl)),
                generateNoDriversExistErrorMessage(jdbcUrl, registeredDrivers));
    }

    public static JdbcContainerConfiguration of(String subProtocol,
            int internalPort,
            String database,
            String username,
            String password) {
        return ImmutableJdbcContainerConfiguration.builder()
                .subProtocol(subProtocol)
                .internalPort(internalPort)
                .database(database)
                .username(username)
                .password(password)
                .build();
    }

    private static JdbcContainerConfiguration of(KnownSubProtocols knownSubProtocol,
            String database,
            String username,
            String password) {
        return of(knownSubProtocol.name(),
                knownSubProtocol.defaultPort(),
                database,
                username,
                password);
    }

    public static JdbcContainerConfiguration ofPostgresql(String database, String username, String password) {
        return of(KnownSubProtocols.POSTGRESQL, database, username, password);
    }

    public static JdbcContainerConfiguration ofMySql(String database, String username, String password) {
        return of(KnownSubProtocols.MYSQL, database, username, password);
    }

    private String jdbcUrlFormat() {
        return JDBC_FORMAT
                .replaceAll("\\$SUB_PROTOCOL", subProtocol())
                .replaceAll("\\$DATABASE", database());
    }

    private String jdbcUrl(Container target) {
        return target.port(internalPort()).inFormat(jdbcUrlFormat());
    }

    @VisibleForTesting
    Connection openConnection(Container target) throws SQLException {
        return DriverManager.getConnection(jdbcUrl(target), username(), password());
    }

    @Value.Check
    protected void check() {
        // Replace container specific information with placeholders, since we don't need a live connection
        String placeholderJdbcUrl = jdbcUrlFormat()
                .replaceAll("\\$HOST", "host")
                .replaceAll("\\$EXTERNAL_PORT", String.valueOf(123));
        // Fail fast if there isn't a driver for the subprotocol
        validateDriverExistsForSubProtocol(placeholderJdbcUrl);
    }

    @Value.Check
    protected JdbcContainerConfiguration normalize() {
        String normalizedSubProtocol = subProtocol().toLowerCase();
        if (normalizedSubProtocol.equals(subProtocol())) {
            return this;
        }
        return of(normalizedSubProtocol,
                internalPort(),
                database(),
                username(),
                password());
    }

    public abstract String subProtocol();

    public abstract int internalPort();

    public abstract String database();

    public abstract String username();

    @Value.Redacted
    public abstract String password();

}
