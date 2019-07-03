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

import com.google.common.base.Throwables;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.events.BuildEvent;
import com.palantir.docker.compose.events.ClusterWaitEvent;
import com.palantir.docker.compose.events.ClusterWaitEvent.ClusterWaitType;
import com.palantir.docker.compose.events.DockerComposeRuleEvent;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.events.PullImagesEvent;
import com.palantir.docker.compose.events.ShutdownEvent;
import com.palantir.docker.compose.events.TaskEvent;
import com.palantir.docker.compose.events.UpEvent;
import com.palantir.docker.compose.events.WaitForServicesEvent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventEmitter {
    private static final Logger log = LoggerFactory.getLogger(EventEmitter.class);

    private List<EventConsumer> eventConsumers;

    EventEmitter(List<EventConsumer> eventConsumers) {
        this.eventConsumers = eventConsumers;
    }

    interface CheckedRunnable {
        void run() throws InterruptedException, IOException;
    }

    public void pull(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitThrowing(runnable, PullImagesEvent.FACTORY);
    }

    public void build(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitThrowing(runnable, BuildEvent.FACTORY);
    }

    public void up(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitThrowing(runnable, UpEvent.FACTORY);
    }

    public void waitingForServices(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitThrowing(runnable, WaitForServicesEvent.FACTORY);
    }

    public void shutdown(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitThrowing(runnable, ShutdownEvent.FACTORY);
    }

    public Consumer<Cluster> userClusterWait(ClusterWait clusterWait)  {
        return clusterWait(ClusterWaitType.USER, clusterWait);
    }

    public Consumer<Cluster> nativeClusterWait(ClusterWait clusterWait) {
        return clusterWait(ClusterWaitType.NATIVE, clusterWait);
    }

    private Consumer<Cluster> clusterWait(
            ClusterWaitType clusterWaitType,
            ClusterWait clusterWait) {

        // This weird bit of complexity is because we can't tell what services a ClusterWait is using until it
        // actually runs. So we have to record the services it accesses then use this to generate the events. The
        // Optional exists solely as a check again logic errors - in the case when events are generated before the
        // cluster wait has begun.
        AtomicReference<Optional<Set<String>>> recordedServiceNames = new AtomicReference<>(Optional.empty());

        Consumer<Cluster> recordingClusterWait = cluster -> {
            RecordingCluster recordingCluster = new RecordingCluster(cluster);
            try {
                clusterWait.waitUntilReady(recordingCluster);
            } finally {
                recordedServiceNames.set(Optional.of(recordingCluster.recordedContainerNames()));
            }
        };

        return cluster -> emitNotThrowing(
                () -> recordingClusterWait.accept(cluster),
                ClusterWaitEvent.factory(
                        () -> recordedServiceNames.get().orElseThrow(
                                () -> new IllegalStateException("Recorded service names have not yet been computed")),
                        clusterWaitType));
    }

    private void emitNotThrowing(CheckedRunnable runnable, TaskEvent.Factory factory) {
        try {
            emitThrowing(runnable, factory);
        } catch (InterruptedException | IOException e) {
            Throwables.propagate(e);
        }
    }

    private void emitThrowing(CheckedRunnable runnable, TaskEvent.Factory factory)
            throws IOException, InterruptedException {
        try {
            emitEvent(factory.started());
            runnable.run();
            emitEvent(factory.succeeded());
        } catch (RuntimeException | IOException | InterruptedException e) {
            emitEvent(factory.failed(e));
            throw e;
        }
    }

    private void emitEvent(DockerComposeRuleEvent event) {
        eventConsumers.forEach(eventConsumer -> {
            try {
                eventConsumer.receiveEvent(event);
            } catch (Exception e) {
                log.error("Error sending event {}", event, e);
            }
        });
    }
}
