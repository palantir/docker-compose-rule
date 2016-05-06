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
package com.palantir.docker.compose.connection.waiting;

import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface ClusterHealthCheck {
    static ClusterHealthCheck serviceHealthCheck(List<String> containerNames, MultiServiceHealthCheck delegate) {
        return cluster -> delegate.areServicesUp(cluster.containers(containerNames));
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

    SuccessOrFailure isClusterHealthy(Cluster cluster);
}
