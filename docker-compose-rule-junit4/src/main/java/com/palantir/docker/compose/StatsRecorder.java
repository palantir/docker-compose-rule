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

import com.google.common.base.Stopwatch;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.stats.ServiceStats;
import com.palantir.docker.compose.stats.Stats;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import one.util.streamex.EntryStream;

class StatsRecorder {
    private final Stats.Builder statsBuilder = Stats.builder();
    private final Stopwatch serviceHealthyStopwatch = Stopwatch.createUnstarted();
    private final ConcurrentMap<String, Optional<Duration>> serviceTimesToBecomeHealthy = new ConcurrentHashMap<>();

    public void pullBuildAndStartContainers(StopwatchUtils.CheckedRunnable runnable) throws IOException,
            InterruptedException {
        statsBuilder.pullBuildAndStartContainers(StopwatchUtils.time(runnable));
    }

    public void forContainersToBecomeHealthy(StopwatchUtils.CheckedRunnable runnable) throws IOException,
            InterruptedException {
        serviceHealthyStopwatch.start();
        statsBuilder.becomeHealthyOrTimeout(StopwatchUtils.time(runnable));
    }

    public void shutdown(StopwatchUtils.CheckedRunnable runnable) throws IOException,
            InterruptedException {
        statsBuilder.shutdown(StopwatchUtils.time(runnable));
    }

    private void serviceIsHealthy(String serviceName) {
        serviceTimesToBecomeHealthy.put(serviceName,
                Optional.of(StopwatchUtils.toDuration(serviceHealthyStopwatch)));
    }

    private void serviceTimedOut(String serviceName) {
        serviceTimesToBecomeHealthy.put(serviceName, Optional.empty());
    }

    private List<ServiceStats> getResults() {
        return EntryStream.of(serviceTimesToBecomeHealthy)
                .mapKeyValue((serviceName, timeTakenToBeHealthy) -> {
                    return ServiceStats.builder()
                            .containerName(serviceName)
                            .timeTakenToBecomeHealthy(timeTakenToBeHealthy)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Stats stats() {
        statsBuilder.addAllContainersWithHealthchecksStats(getResults());
        return statsBuilder.build();
    }

    public ClusterWait.Listener clusterWaitListener(String serviceName) {
        return new ClusterWait.Listener() {
            @Override
            public void becameHealthy() {
                serviceIsHealthy(serviceName);
            }

            @Override
            public void timedOut() {
                serviceTimedOut(serviceName);
            }
        };
    }

    public ClusterWait.Listener clusterWaitListener(Iterable<String> serviceNames) {
        return new ClusterWait.Listener() {
            @Override
            public void becameHealthy() {
                serviceNames.forEach(StatsRecorder.this::serviceIsHealthy);
            }

            @Override
            public void timedOut() {
                serviceNames.forEach(StatsRecorder.this::serviceTimedOut);
            }
        };
    }
}
