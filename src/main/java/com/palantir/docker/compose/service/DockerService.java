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

import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.ServiceWait;
import org.joda.time.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.joda.time.Duration.standardMinutes;

public class DockerService {

    private static final Duration DEFAULT_TIMEOUT = standardMinutes(2);

    private final File dockerComposeFile;
    private final Map<String, HealthCheck> healthChecks;

    private DockerService(File dockerComposeFile, Map<String, HealthCheck> healthChecks) {
        this.dockerComposeFile = dockerComposeFile;
        this.healthChecks = healthChecks;
    }

    public static DockerService fromDockerCompositionFile(String dockerComposeFile) {
        return new DockerService(new File(dockerComposeFile), emptyMap());
    }

    public DockerService withHealthCheck(String serviceName, HealthCheck healthCheck) {
        Map<String, HealthCheck> newHealthChecks = new HashMap<>(healthChecks);
        newHealthChecks.put(serviceName, healthCheck);
        return new DockerService(dockerComposeFile, newHealthChecks);
    }

    public File getDockerComposeFileLocation() {
        return dockerComposeFile;
    }

    public List<ServiceWait> waits(ContainerCache containerCache) {
        List<ServiceWait> serviceWaits = new ArrayList<>();
        healthChecks.forEach((serviceName, healthCheck) -> serviceWaits.add(new ServiceWait(containerCache.get(serviceName), healthCheck, DEFAULT_TIMEOUT)));
        return serviceWaits;
    }
}
