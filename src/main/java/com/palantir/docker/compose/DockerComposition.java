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
package com.palantir.docker.compose;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import java.io.IOException;
import org.junit.rules.ExternalResource;

/**
 * @deprecated Please use the DockerComposeRule class directly instead.
 *
 * Note, if you want to make this transition incrementally, you can use `DockerComposition#rule()` to access the underlying
 * DockerComposeRule instance.
 */
@Deprecated
public class DockerComposition extends ExternalResource {

    private DockerComposeRule rule;

    public DockerComposition(DockerComposeRule rule) {
        this.rule = rule;
    }

    @Override
    public void before() throws IOException, InterruptedException {
        rule.before();
    }

    @Override
    public void after() {
        rule.after();
    }

    public DockerComposeRule rule() {
        return rule;
    }

    public DockerPort portOnContainerWithExternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return rule.containers().container(container).portMappedExternallyTo(portNumber);
    }

    public DockerPort portOnContainerWithInternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return rule.containers().container(container).portMappedInternallyTo(portNumber);
    }

    public static DockerCompositionBuilder of(String dockerComposeFile) {
        return new DockerCompositionBuilder()
                .files(DockerComposeFiles.from(dockerComposeFile));
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles) {
        return new DockerCompositionBuilder()
                .files(dockerComposeFiles);
    }

    public static DockerCompositionBuilder of(String dockerComposeFile, DockerMachine dockerMachine) {
        return new DockerCompositionBuilder()
                .files(DockerComposeFiles.from(dockerComposeFile))
                .machine(dockerMachine);
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine) {
        return new DockerCompositionBuilder()
                .files(dockerComposeFiles)
                .machine(dockerMachine);
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine, String projectName) {
        return new DockerCompositionBuilder()
                .files(dockerComposeFiles)
                .machine(dockerMachine)
                .projectName(ProjectName.fromString(projectName));
    }

    public static DockerCompositionBuilder of(DockerCompose compose) {
        return new DockerCompositionBuilder().dockerCompose(compose);
    }

    public String exec(DockerComposeExecOption options, String containerName, DockerComposeExecArgument arguments)
            throws IOException, InterruptedException {
        return rule.exec(options, containerName, arguments);
    }

    public DockerPort hostNetworkedPort(int port) {
        return rule.hostNetworkedPort(port);
    }
}
