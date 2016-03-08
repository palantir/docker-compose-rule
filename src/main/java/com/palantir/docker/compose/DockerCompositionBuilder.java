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
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

import static org.joda.time.Duration.standardMinutes;

public class DockerCompositionBuilder {
    private static final Duration DEFAULT_TIMEOUT = standardMinutes(2);

    private final List<ServiceWait> serviceWaits = new ArrayList<>();
    private final DockerCompose dockerComposeProcess;
    private final ContainerCache containers;
    private LogCollector logCollector = new DoNothingLogCollector();

    public DockerCompositionBuilder(DockerCompose dockerComposeProcess) {
        this.dockerComposeProcess = dockerComposeProcess;
        this.containers = new ContainerCache(dockerComposeProcess);
    }

    public DockerCompositionBuilder waitingForService(String serviceName, HealthCheck check) {
        return waitingForService(serviceName, check, DEFAULT_TIMEOUT);
    }

    public DockerCompositionBuilder waitingForService(String serviceName, HealthCheck check, Duration timeout) {
        serviceWaits.add(new ServiceWait(containers.get(serviceName), check, timeout));
        return this;
    }

    public DockerCompositionBuilder saveLogsTo(String path) {
        this.logCollector = FileLogCollector.fromPath(path);
        return this;
    }

    public DockerComposition build() {
        return new DockerComposition(dockerComposeProcess, serviceWaits, logCollector, containers);
    }
}
