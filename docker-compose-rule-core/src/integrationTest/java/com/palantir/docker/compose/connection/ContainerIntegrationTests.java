/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.docker.compose.connection;

import static com.palantir.docker.compose.execution.DockerComposeExecArgument.arguments;
import static com.palantir.docker.compose.execution.DockerComposeExecOption.noOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.io.IOException;
import java.time.Duration;
import org.awaitility.core.ConditionFactory;
import org.junit.Test;

public class ContainerIntegrationTests {

    private static final ConditionFactory wait = await().atMost(Duration.ofSeconds(10));

    private final DockerMachine dockerMachine = DockerMachine.localMachine().build();
    private final Docker docker = new Docker(
            DockerExecutable.builder().dockerConfiguration(dockerMachine).build());

    @Test
    public void testStateChanges_withoutHealthCheck() throws IOException, InterruptedException {
        DockerCompose dockerCompose = new DefaultDockerCompose(
                DockerComposeFiles.from("src/test/resources/no-healthcheck.yaml"), dockerMachine, ProjectName.random());

        // The noHealthcheck service has no healthcheck specified; it should be immediately healthy
        Container container = new Container("noHealthcheck", docker, dockerCompose);
        assertThat(container.state()).isEqualTo(State.DOWN);
        container.up();
        assertThat(container.state()).isEqualTo(State.HEALTHY);
        container.kill();
        assertThat(container.state()).isEqualTo(State.DOWN);
    }

    @Test
    public void testStateChanges_withHealthCheck() throws IOException, InterruptedException {
        DockerCompose dockerCompose = new DefaultDockerCompose(
                DockerComposeFiles.from("src/test/resources/native-healthcheck.yaml"),
                dockerMachine,
                ProjectName.random());

        // The withHealthcheck service's healthcheck checks every 100ms whether the file "healthy" exists
        Container container = new Container("withHealthcheck", docker, dockerCompose);
        assertThat(container.state()).isEqualTo(State.DOWN);
        container.up();
        assertThat(container.state()).isEqualTo(State.UNHEALTHY);
        dockerCompose.exec(noOptions(), "withHealthcheck", arguments("touch", "healthy"));
        wait.until(container::state, equalTo(State.HEALTHY));
        dockerCompose.exec(noOptions(), "withHealthcheck", arguments("rm", "healthy"));
        wait.until(container::state, equalTo(State.UNHEALTHY));
        container.kill();
        assertThat(container.state()).isEqualTo(State.DOWN);
    }
}
