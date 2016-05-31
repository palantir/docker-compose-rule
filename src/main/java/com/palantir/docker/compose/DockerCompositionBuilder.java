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

import com.palantir.docker.compose.ImmutableDockerComposeRule.Builder;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.execution.DockerCompose;
import java.util.List;
import org.joda.time.Duration;

public class DockerCompositionBuilder {
    private final Builder builder;

    public DockerCompositionBuilder() {
        this.builder = DockerComposeRule.builder();
    }

    public DockerCompositionBuilder waitingForService(String serviceName, HealthCheck<Container> check) {
        builder.waitingForService(serviceName, check);
        return this;
    }

    public DockerCompositionBuilder waitingForServices(List<String> services, HealthCheck<List<Container>> check) {
        builder.waitingForServices(services, check);
        return this;
    }

    public DockerCompositionBuilder waitingForServices(List<String> services, HealthCheck<List<Container>> check, Duration timeout) {
        builder.waitingForServices(services, check, timeout);
        return this;
    }

    public DockerCompositionBuilder waitingForService(String serviceName, HealthCheck<Container> check, Duration timeout) {
        builder.waitingForService(serviceName, check, timeout);
        return this;
    }

    public DockerCompositionBuilder waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck, Duration timeout) {
        builder.waitingForHostNetworkedPort(port, healthCheck, timeout);
        return this;
    }

    public DockerCompositionBuilder waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck) {
        builder.waitingForHostNetworkedPort(port, healthCheck);
        return this;
    }

    public DockerCompositionBuilder files(DockerComposeFiles files) {
        builder.files(files);
        return this;
    }

    public DockerCompositionBuilder machine(DockerMachine machine) {
        builder.machine(machine);
        return this;
    }

    public DockerCompositionBuilder projectName(ProjectName name) {
        builder.projectName(name);
        return this;
    }

    public DockerCompositionBuilder dockerCompose(DockerCompose compose) {
        builder.dockerCompose(compose);
        return this;
    }

    public DockerCompositionBuilder saveLogsTo(String path) {
        builder.saveLogsTo(path);
        return this;
    }


    public DockerCompositionBuilder removeConflictingContainersOnStartup(boolean removeConflictingContainersOnStartup) {
        builder.removeConflictingContainersOnStartup(removeConflictingContainersOnStartup);
        return this;
    }

    public DockerCompositionBuilder retryAttempts(int retryAttempts) {
        builder.retryAttempts(retryAttempts);
        return this;
    }

    public DockerCompositionBuilder skipShutdown(boolean skipShutdown) {
        builder.skipShutdown(skipShutdown);
        return this;
    }

    public DockerComposition build() {
        DockerComposeRule rule = builder.build();
        return new DockerComposition(rule);
    }
}
