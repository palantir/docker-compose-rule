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

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.ServiceWait;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import com.palantir.docker.compose.logging.LogCollector;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class DockerComposition extends ExternalResource {

    private static final Logger log = LoggerFactory.getLogger(DockerComposition.class);

    private final DockerCompose dockerCompose;
    private final ContainerCache containers;
    private final List<ServiceWait> serviceWaits;
    private final LogCollector logCollector;

    public DockerComposition(
            DockerCompose dockerCompose,
            List<ServiceWait> serviceWaits,
            LogCollector logCollector,
            ContainerCache containers) {
        this.dockerCompose = dockerCompose;
        this.serviceWaits = ImmutableList.copyOf(serviceWaits);
        this.logCollector = logCollector;
        this.containers = containers;
    }

    @Override
    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");
        dockerCompose.build();
        dockerCompose.up();

        log.debug("Starting log collection");

        logCollector.startCollecting(dockerCompose);
        log.debug("Waiting for services");
        serviceWaits.forEach(ServiceWait::waitTillServiceIsUp);
        log.debug("docker-compose cluster started");
    }

    @Override
    public void after() {
        try {
            log.debug("Killing docker-compose cluster");
            dockerCompose.down();
            dockerCompose.kill();
            dockerCompose.rm();
            logCollector.stopCollecting();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error cleaning up docker compose cluster", e);
        }
    }

    public DockerPort portOnContainerWithExternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return containers.get(container)
                         .portMappedExternallyTo(portNumber);
    }

    public DockerPort portOnContainerWithInternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return containers.get(container)
                         .portMappedInternallyTo(portNumber);
    }

    public static DockerCompositionBuilder of(String dockerComposeFile) {
        return of(DockerComposeFiles.from(dockerComposeFile));
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles) {
        return of(dockerComposeFiles, DockerMachine.localMachine().build());
    }

    public static DockerCompositionBuilder of(String dockerComposeFile, DockerMachine dockerMachine) {
        return of(DockerComposeFiles.from(dockerComposeFile), dockerMachine);
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine) {
        return of(new DefaultDockerCompose(dockerComposeFiles, dockerMachine, ProjectName.random()));
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine, String projectName) {
        return of(new DefaultDockerCompose(dockerComposeFiles, dockerMachine, ProjectName.fromString(projectName)));
    }

    public static DockerCompositionBuilder of(DockerCompose executable) {
        return new DockerCompositionBuilder(executable);
    }

    public void exec(DockerComposeExecOption dockerComposeExecOption, String containerName, DockerComposeExecArgument dockerComposeExecArgument)
            throws IOException, InterruptedException {
        dockerCompose.exec(dockerComposeExecOption, containerName, dockerComposeExecArgument);
    }

}
