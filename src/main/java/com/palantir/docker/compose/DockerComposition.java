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
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.logging.LogCollector;
import com.palantir.docker.compose.service.DockerService;
import com.palantir.docker.compose.service.ServiceCluster;
import com.palantir.docker.compose.service.ServiceWait;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

public class DockerComposition extends ExternalResource {

    private static final Logger log = LoggerFactory.getLogger(DockerComposition.class);

    private final DockerCompose dockerComposeProcess;
    private final ContainerCache containers;
    private final List<ServiceWait> serviceWaits;
    private final LogCollector logCollector;

    public DockerComposition(
            DockerCompose dockerComposeProcess,
            List<ServiceWait> serviceWaits,
            LogCollector logCollector,
            ContainerCache containers) {
        this.dockerComposeProcess = dockerComposeProcess;
        this.serviceWaits = copyOf(serviceWaits);
        this.logCollector = logCollector;
        this.containers = containers;
    }

    @Override
    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");
        dockerComposeProcess.build();
        dockerComposeProcess.up();

        log.debug("Starting log collection");

        logCollector.startCollecting(dockerComposeProcess);
        log.debug("Waiting for services");
        serviceWaits.forEach(ServiceWait::waitTillServiceIsUp);
        log.debug("docker-compose cluster started");
    }

    @Override
    public void after() {
        try {
            log.debug("Killing docker-compose cluster");
            dockerComposeProcess.down();
            dockerComposeProcess.kill();
            dockerComposeProcess.rm();
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
        return of(new DockerCompose(dockerComposeFiles, dockerMachine));
    }

    public static DockerCompositionBuilder of(DockerCompose executable) {
        return new DockerCompositionBuilder(executable);
    }

    public static DockerCompositionBuilder fromService(DockerService service) {
        return fromServiceCluster(ServiceCluster.of(service));
    }

    public static DockerCompositionBuilder fromService(DockerService service, DockerMachine dockerMachine) {
        return fromServiceCluster(ServiceCluster.of(service), dockerMachine);
    }

    public static DockerCompositionBuilder fromServiceCluster(ServiceCluster services) {
        return fromServiceCluster(services, DockerMachine.localMachine().build());
    }

    public static DockerCompositionBuilder fromServiceCluster(ServiceCluster services, DockerMachine dockerMachine) {
        return fromServiceCluster(new DockerCompose(services.dockerComposeFiles(), dockerMachine), services);
    }

    public static DockerCompositionBuilder fromServiceCluster(DockerCompose executable, ServiceCluster services) {
        DockerCompositionBuilder builder = new DockerCompositionBuilder(executable);
        services.addToComposition(builder);
        return builder;
    }
}
