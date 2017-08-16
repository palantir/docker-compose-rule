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

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import java.sql.Connection;
import java.sql.SQLException;

public class JdbcContainerHealthCheck implements HealthCheck<Container> {

    private final JdbcContainerConfiguration configuration;

    private JdbcContainerHealthCheck(JdbcContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public static JdbcContainerHealthCheck of(JdbcContainerConfiguration configuration) {
        return new JdbcContainerHealthCheck(configuration);
    }

    @Override
    public SuccessOrFailure isHealthy(Container target) {
        try (Connection connection = configuration.openConnection(target)) {
            return connection.isValid(0)
                    ? SuccessOrFailure.success()
                    : SuccessOrFailure.failure("Connection not valid, database may still be starting up?");
        } catch (SQLException e) {
            return SuccessOrFailure.fromException(e);
        }
    }

}
