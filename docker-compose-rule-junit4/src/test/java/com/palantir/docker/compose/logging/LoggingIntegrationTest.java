/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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
package com.palantir.docker.compose.logging;

import static com.palantir.docker.compose.matchers.IOMatchers.fileWithConents;
import static com.palantir.docker.compose.matchers.IOMatchers.matchingPattern;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LoggingIntegrationTest {

    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private DockerComposeRule dockerComposeRule;

    @Before
    public void setUp() {
        dockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose.yaml")
                .waitingForService("db", foo())
                .waitingForService("db2", foo())
                .saveLogsTo(logFolder.getRoot().getAbsolutePath())
                .build();
    }

    private static HealthCheck<Container> foo() {
        return target -> {
            for (DockerPort port : target.ports().stream().collect(toList())) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(port.getIp(), port.getExternalPort()), 500);
                } catch (IOException e) {
                    return SuccessOrFailure.fromException(new RuntimeException(
                            "Port " + port.getInternalPort() + " failed to open", e));
                }
            }
            return SuccessOrFailure.success();
        };
    }

    @Test
    public void logs_can_be_saved_to_a_directory() throws IOException, InterruptedException {
        try {
            dockerComposeRule.before();
        } finally {
            dockerComposeRule.after();
        }
        assertThat(new File(logFolder.getRoot(), "db.log"), is(fileWithConents(matchingPattern(
                ".*Attaching to \\w+_db_1.*server started.*"))));
        assertThat(new File(logFolder.getRoot(), "db2.log"), is(fileWithConents(matchingPattern(
                ".*Attaching to \\w+_db2_1.*server started.*"))));
    }

}
