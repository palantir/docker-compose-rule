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

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.waiting.MultiServiceHealthCheck;
import com.palantir.docker.compose.connection.waiting.ServiceWait;
import com.palantir.docker.compose.connection.waiting.SingleServiceHealthCheck;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.RetryingDockerCompose;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.joda.time.Duration.standardMinutes;

public class DockerCompositionBuilder {
    private static final Duration DEFAULT_TIMEOUT = standardMinutes(2);
    public static final int DEFAULT_RETRY_ATTEMPTS = 2;

    private final List<ServiceWait> serviceWaits = new ArrayList<>();
    private final DockerCompose dockerCompose;
    private final ContainerCache containers;
    private LogCollector logCollector = new DoNothingLogCollector();
    private int numRetryAttempts = DEFAULT_RETRY_ATTEMPTS;

    public DockerCompositionBuilder(DockerCompose dockerCompose) {
        this.dockerCompose = dockerCompose;
        this.containers = new ContainerCache(dockerCompose);
    }

    public DockerCompositionBuilder waitingForService(String serviceName, SingleServiceHealthCheck check) {
        return waitingForService(serviceName, check, DEFAULT_TIMEOUT);
    }

    public DockerCompositionBuilder waitingForServices(List<String> services, MultiServiceHealthCheck check) {
        return waitingForServices(services, check, DEFAULT_TIMEOUT);
    }

    public DockerCompositionBuilder waitingForServices(List<String> services, MultiServiceHealthCheck check, Duration timeout) {
        List<Container> containersToWaitFor = services.stream()
                .map(containers::get)
                .collect(toList());
        serviceWaits.add(new ServiceWait(containersToWaitFor, check, timeout));
        return this;
    }

    public DockerCompositionBuilder waitingForService(String serviceName, SingleServiceHealthCheck check, Duration timeout) {
        serviceWaits.add(new ServiceWait(containers.get(serviceName), check, timeout));
        return this;
    }

    public DockerCompositionBuilder saveLogsTo(String path) {
        this.logCollector = FileLogCollector.fromPath(path);
        return this;
    }

    public DockerCompositionBuilder retryAttempts(int retryAttempts) {
        this.numRetryAttempts = retryAttempts;
        return this;
    }

    public DockerComposition build() {
        DockerCompose retryingDockerCompose = new RetryingDockerCompose(numRetryAttempts, dockerCompose);
        return new DockerComposition(retryingDockerCompose, serviceWaits, logCollector, containers);
    }
}
