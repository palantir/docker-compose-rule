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

import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.ServiceWait;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import com.palantir.docker.compose.service.DockerService;
import org.joda.time.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DockerCompositionBuilder {

    private final List<DockerService> services = new ArrayList<>();
    private DockerCompose dockerComposeProcess;
    private LogCollector logCollector = new DoNothingLogCollector();

    public DockerCompositionBuilder(DockerCompose dockerComposeProcess) {
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public DockerCompositionBuilder waitingForService(String serviceName, HealthCheck check) {
        services.add(DockerService.externallyDefined().withHealthCheck(serviceName, check));
        return this;
    }

    public DockerCompositionBuilder waitingForService(String serviceName, HealthCheck check, Duration timeout) {
        services.add(DockerService.externallyDefined().withTimeout(timeout).withHealthCheck(serviceName, check));
        return this;
    }

    public DockerCompositionBuilder saveLogsTo(String path) {
        this.logCollector = FileLogCollector.fromPath(path);
        return this;
    }

    public DockerCompositionBuilder withService(DockerService service) {
        services.add(service);
        Optional<File> additionalFile = service.getDockerComposeFileLocation();
        if (additionalFile.isPresent()) {
            dockerComposeProcess = dockerComposeProcess.withAdditionalComposeFile(additionalFile.get());
        }
        return this;
    }

    public DockerComposition build() {
        ContainerCache containerCache = new ContainerCache(dockerComposeProcess);
        List<ServiceWait> serviceWaits = new ArrayList<>();
        services.forEach(service -> serviceWaits.addAll(service.waits(containerCache)));
        return new DockerComposition(dockerComposeProcess, serviceWaits, logCollector, containerCache);
    }

}
