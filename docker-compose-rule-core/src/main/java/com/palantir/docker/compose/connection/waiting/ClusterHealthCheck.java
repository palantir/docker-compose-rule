/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.docker.compose.connection.waiting;

import static java.util.stream.Collectors.joining;

import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.State;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface ClusterHealthCheck {
    Logger log = LoggerFactory.getLogger(ClusterHealthCheck.class);

    static ClusterHealthCheck serviceHealthCheck(List<String> containerNames, HealthCheck<List<Container>> delegate) {
        return transformingHealthCheck(cluster -> cluster.containers(containerNames), delegate);
    }

    static ClusterHealthCheck serviceHealthCheck(String containerName, HealthCheck<Container> containerCheck) {
        return transformingHealthCheck(cluster -> cluster.container(containerName), containerCheck);
    }

    static <T> ClusterHealthCheck transformingHealthCheck(Function<Cluster, T> transform, HealthCheck<T> healthCheck) {
        return cluster -> {
            T target = transform.apply(cluster);
            return healthCheck.isHealthy(target);
        };
    }

    /**
     * Returns a check that the native "healthcheck" status of the docker containers is not unhealthy.
     *
     * <p>Does not wait for DOWN or PAUSED containers, or containers with no healthcheck defined.
     */
    static ClusterHealthCheck nativeHealthChecks() {
        return cluster -> {
            Set<String> unhealthyContainers = new LinkedHashSet<>();
            try {
                log.info("Checking health of containers {}",
                        cluster.allContainers().stream().map(Container::toString).collect(joining(", ")));
                for (Container container : cluster.allContainers()) {
                    log.info("Checking health of container {}", container);
                    State state = container.state();
                    if (state == State.UNHEALTHY) {
                        log.info("Container unhealthy {}", container);
                        unhealthyContainers.add(container.getContainerName());
                    }
                }
                if (!unhealthyContainers.isEmpty()) {
                    log.warn("Unhealthy containers {}", unhealthyContainers.stream().collect(joining(", ")));
                    return SuccessOrFailure.failure(
                            "The following containers are not healthy: " + unhealthyContainers.stream().collect(joining(", ")));
                }
                return SuccessOrFailure.success();
            } catch (IOException e) {
                return SuccessOrFailure.fromException(e);
            }
        };
    }

    SuccessOrFailure isClusterHealthy(Cluster cluster) throws InterruptedException;
}
