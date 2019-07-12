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

import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.events.ClusterWaitType;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RecordingClusterWait {
    private static final Logger log = LoggerFactory.getLogger(RecordingClusterWait.class);

    private final ClusterWait clusterWait;
    private final ClusterWaitType clusterWaitType;

    // This weird bit of complexity is because we can't tell what services a ClusterWait is using until it
    // actually runs. So we have to record the services it accesses then use this to generate the events. The
    // Optional exists solely as a check again logic errors - in the case when events are generated before the
    // cluster wait has begun.
    private Optional<Set<String>> recordedServiceNames = Optional.empty();

    RecordingClusterWait(ClusterWait clusterWait, ClusterWaitType clusterWaitType) {
        this.clusterWait = clusterWait;
        this.clusterWaitType = clusterWaitType;
    }

    public void waitForCluster(Cluster cluster) {
        RecordingCluster recordingCluster = new RecordingCluster(cluster);
        try {
            clusterWait.waitUntilReady(recordingCluster);
            log.info(
                    "Cluster wait for services {} (type: {}) successfully finished",
                    recordingCluster.recordedContainerNames(),
                    clusterWaitType.toString().toLowerCase());
        } catch (Exception e) {
            // Message is sometimes null eg in the case where an InterruptedException is raised
            if (e.getMessage() != null) {
                log.error(
                        "Cluster wait for services {} (type: {}) timed out with exception:\n\t{}",
                        recordingCluster.recordedContainerNames(),
                        clusterWaitType.toString().toLowerCase(),
                        e.getMessage());
            }
            throw e;
        } finally {
            recordedServiceNames = Optional.of(recordingCluster.recordedContainerNames());
        }
    }

    public Set<String> recordedServiceNames() {
        return recordedServiceNames.orElseThrow(
                () -> new IllegalStateException("Recorded service names have not yet been computed"));
    }
}
