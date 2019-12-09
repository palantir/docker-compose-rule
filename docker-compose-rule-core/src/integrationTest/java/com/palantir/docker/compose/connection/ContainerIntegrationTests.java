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

import static com.jayway.awaitility.Awaitility.await;
import static com.palantir.docker.compose.execution.DockerComposeExecArgument.arguments;
import static com.palantir.docker.compose.execution.DockerComposeExecOption.noOptions;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

import com.github.zafarkhaja.semver.Version;
import com.jayway.awaitility.core.ConditionFactory;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class ContainerIntegrationTests {

    private static final ConditionFactory wait = await().atMost(10, TimeUnit.SECONDS);

    private final DockerMachine dockerMachine = DockerMachine.localMachine().build();
    private final Docker docker = new Docker(DockerExecutable.builder()
            .dockerConfiguration(dockerMachine)
            .build());

    @Test
    public void testStateChanges_withoutHealthCheck() throws IOException, InterruptedException {
        DockerCompose dockerCompose = new DefaultDockerCompose(
                DockerComposeFiles.from("src/test/resources/no-healthcheck.yaml"),
                dockerMachine,
                ProjectName.random());

        // The noHealthcheck service has no healthcheck specified; it should be immediately healthy
        Container container = new Container("noHealthcheck", docker, dockerCompose);
        Assert.assertEquals(State.DOWN, container.state());
        container.up();
        assertEquals(State.HEALTHY, container.state());
        container.kill();
        assertEquals(State.DOWN, container.state());
    }

    /**
     * This test is not currently enabled in Circle as it does not provide a sufficiently recent version of docker-compose.
     *
     * @see <a href="https://github.com/palantir/docker-compose-rule/issues/156">Issue #156</a>
     */
    @Test
    public void testStateChanges_withHealthCheck() throws IOException, InterruptedException {
        DockerCompose dockerCompose = new DefaultDockerCompose(
                DockerComposeFiles.from("src/test/resources/native-healthcheck.yaml"),
                dockerMachine,
                ProjectName.random());

        assumeThat(docker.version(), greaterThanOrEqualTo(Version.valueOf("1.12.0")));
        assumeThat(dockerCompose.version(), greaterThanOrEqualTo(Version.valueOf("1.10.0")));

        // The withHealthcheck service's healthcheck checks every 100ms whether the file "healthy" exists
        Container container = new Container("withHealthcheck", docker, dockerCompose);
        assertEquals(State.DOWN, container.state());
        container.up();
        assertEquals(State.UNHEALTHY, container.state());
        dockerCompose.exec(noOptions(), "withHealthcheck", arguments("touch", "healthy"));
        wait.until(container::state, equalTo(State.HEALTHY));
        dockerCompose.exec(noOptions(), "withHealthcheck", arguments("rm", "healthy"));
        wait.until(container::state, equalTo(State.UNHEALTHY));
        container.kill();
        assertEquals(State.DOWN, container.state());
    }
}
