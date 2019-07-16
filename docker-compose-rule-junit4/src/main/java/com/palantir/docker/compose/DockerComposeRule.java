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

import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.serviceHealthCheck;
import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.transformingHealthCheck;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.logging.FileLogCollector;
import java.util.List;
import org.immutables.value.Value;
import org.joda.time.ReadableDuration;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@Value.Immutable
@CustomImmutablesStyle
public abstract class DockerComposeRule extends DockerComposeManager implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ImmutableDockerComposeRule.Builder implements BuilderExtensions {
        @Override
        public Builder file(String dockerComposeYmlFile) {
            return files(DockerComposeFiles.from(dockerComposeYmlFile));
        }

        @Override
        public Builder saveLogsTo(String path) {
            return logCollector(FileLogCollector.fromPath(path));
        }

        @Override
        public Builder skipShutdown(boolean skipShutdown) {
            if (skipShutdown) {
                return shutdownStrategy(ShutdownStrategy.SKIP);
            }

            return this;
        }

        @Override
        public Builder waitingForService(String serviceName, HealthCheck<Container> healthCheck) {
            return waitingForService(serviceName, healthCheck, DEFAULT_TIMEOUT);
        }

        @Override
        public Builder waitingForService(String serviceName, HealthCheck<Container> healthCheck, ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(serviceName, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        @Override
        public Builder waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck) {
            return waitingForServices(services, healthCheck, DEFAULT_TIMEOUT);
        }

        @Override
        public Builder waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck, ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(services, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        @Override
        public Builder waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck) {
            return waitingForHostNetworkedPort(port, healthCheck, DEFAULT_TIMEOUT);
        }

        @Override
        public Builder waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck, ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = transformingHealthCheck(cluster ->
                    new DockerPort(cluster.ip(), port, port), healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        @Override
        public Builder clusterWaits(Iterable<? extends ClusterWait> elements) {
            return addAllClusterWaits(elements);
        }

        @Override
        public DockerComposeRule build() {
            return super.build();
        }
    }
}
