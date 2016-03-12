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

package com.palantir.docker.compose.service;


import com.palantir.docker.compose.DockerCompositionBuilder;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import org.joda.time.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.joda.time.Duration.standardMinutes;

public class DockerService {

    public static final Duration DEFAULT_TIMEOUT = standardMinutes(2);

    private final ServiceDefinition serviceDefinition;
    private final Map<String, HealthCheck> healthChecks;
    private final Duration timeout;

    private DockerService(ServiceDefinition serviceDefinition, Map<String, HealthCheck> healthChecks, Duration timeout) {
        this.serviceDefinition = serviceDefinition;
        this.healthChecks = healthChecks;
        this.timeout = timeout;
    }

    public static DockerService fromDockerCompositionFile(String dockerComposeFile) {
        return new DockerService(ServiceDefinition.fromFile(dockerComposeFile), emptyMap(), DEFAULT_TIMEOUT);
    }

    public static DockerService fromDockerCompositionFile(File dockerComposeFile) {
        return new DockerService(ServiceDefinition.fromFile(dockerComposeFile), emptyMap(), DEFAULT_TIMEOUT);
    }

    public static DockerService externallyDefined() {
        return new DockerService(ServiceDefinition.external(), emptyMap(), DEFAULT_TIMEOUT);
    }

    public static InlineDockerServiceBuilder fromImage(String imageName, String serviceName) {
        return new InlineDockerServiceBuilder(imageName, serviceName);
    }

    public DockerService withTimeout(Duration newTimeout) {
        return new DockerService(serviceDefinition, healthChecks, newTimeout);
    }

    public DockerService withHealthCheck(String serviceName, HealthCheck healthCheck) {
        Map<String, HealthCheck> newHealthChecks = new HashMap<>(healthChecks);
        newHealthChecks.put(serviceName, healthCheck);
        return new DockerService(serviceDefinition, newHealthChecks, timeout);
    }

    public Optional<File> dockerComposeFileLocation() {
        return serviceDefinition.dockerComposeFileLocation();
    }

    public List<ServiceWait> waits(ContainerCache containerCache) {
        List<ServiceWait> serviceWaits = new ArrayList<>();
        healthChecks.forEach((serviceName, healthCheck) -> serviceWaits.add(new ServiceWait(containerCache.get(serviceName), healthCheck, timeout)));
        return serviceWaits;
    }

    public void addWaits(DockerCompositionBuilder builder) {
        healthChecks.forEach((serviceName, healthCheck) -> builder.waitingForService(serviceName, healthCheck, timeout));
    }

}
