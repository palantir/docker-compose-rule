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

package com.palantir.docker.compose;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.DockerComposeRunArgument;
import com.palantir.docker.compose.execution.DockerComposeRunOption;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.io.IOException;
import org.junit.rules.TestRule;

public interface DockerComposeRule extends TestRule {
    String run(
            DockerComposeRunOption options, String containerName,
            DockerComposeRunArgument arguments) throws IOException, InterruptedException;
    String exec(
            DockerComposeExecOption options, String containerName,
            DockerComposeExecArgument arguments) throws IOException, InterruptedException;
    Cluster containers();
    DockerCompose dockerCompose();
    ShutdownStrategy shutdownStrategy();
    Docker docker();
    DockerExecutable dockerExecutable();
    DockerComposeExecutable dockerComposeExecutable();
    ProjectName projectName();
    DockerMachine machine();
    DockerComposeFiles files();
    DockerPort hostNetworkedPort(int port);
    void before() throws IOException, InterruptedException;
    void after();

    static DefaultDockerComposeRule.Builder builder() {
        return new DefaultDockerComposeRule.Builder();
    }
}
