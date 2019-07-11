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
import com.palantir.docker.compose.connection.waiting.Exceptions;
import com.palantir.docker.compose.events.BuildEvent;
import com.palantir.docker.compose.events.ClusterWaitEvent;
import com.palantir.docker.compose.events.ClusterWaitType;
import com.palantir.docker.compose.events.Event;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.events.PullEvent;
import com.palantir.docker.compose.events.ShutdownEvent;
import com.palantir.docker.compose.events.Task;
import com.palantir.docker.compose.events.UpEvent;
import com.palantir.docker.compose.events.WaitForServicesEvent;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventEmitter {
    private static final Logger log = LoggerFactory.getLogger(EventEmitter.class);

    private final Clock clock;
    private final List<EventConsumer> eventConsumers;

    EventEmitter(List<EventConsumer> eventConsumers) {
        this(Clock.systemUTC(), eventConsumers);
    }

    EventEmitter(Clock clock, List<EventConsumer> eventConsumers) {
        this.clock = clock;
        this.eventConsumers = eventConsumers;
    }

    interface CheckedRunnable {
        void run() throws InterruptedException, IOException;
    }

    public void pull(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitTask(runnable, task -> Event.pull(PullEvent.builder()
                .task(task)
                .build()));
    }

    public void build(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitTask(runnable, task -> Event.build(BuildEvent.builder().task(task).build()));
    }

    public void up(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitTask(runnable, task -> Event.up(UpEvent.builder().task(task).build()));
    }

    public void waitingForServices(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitTask(runnable, task -> Event.waitForServices(WaitForServicesEvent.builder().task(task).build()));
    }

    public void shutdown(CheckedRunnable runnable) throws IOException, InterruptedException {
        emitTask(runnable, task -> Event.shutdown(ShutdownEvent.builder().task(task).build()));
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
                log.info(
                        "Cluster wait for services {} (type: {}) successfully finished",
                        recordingCluster.recordedContainerNames(),
                        clusterWaitType.toString().toLowerCase());
            } catch (Exception e) {
                log.error(
                        "Cluster wait for services {} (type: {}) timed out with exception:\n\t{}",
                        recordingCluster.recordedContainerNames(),
                        clusterWaitType.toString().toLowerCase(),
                        e.getMessage());
                throw e;
            } finally {
                recordedServiceNames.set(Optional.of(recordingCluster.recordedContainerNames()));
            }
        };

        return cluster -> emitNotThrowing(
                () -> recordingClusterWait.accept(cluster),
                task -> Event.clusterWait(ClusterWaitEvent.builder()
                        .task(task)
                        .serviceNames(recordedServiceNames.get().orElseThrow(
                                () -> new IllegalStateException("Recorded service names have not yet been computed")))
                        .type(clusterWaitType)
                        .build()));
    }

    private void emitNotThrowing(CheckedRunnable runnable, Function<Task, Event> eventFunction) {
        try {
            emitTask(runnable, eventFunction);
        } catch (InterruptedException | IOException e) {
            Throwables.propagate(e);
        }
    }

    private void emitTask(CheckedRunnable runnable, Function<Task, Event> eventFunction)
            throws IOException, InterruptedException {
        Optional<String> failure = Optional.empty();
        OffsetDateTime startTime = clock.instant().atOffset(ZoneOffset.UTC);
        try {
            runnable.run();
        } catch (RuntimeException | IOException | InterruptedException e) {
            failure = Optional.of(Exceptions.condensedStacktraceFor(e));
            throw e;
        } finally {
            OffsetDateTime endTime = clock.instant().atOffset(ZoneOffset.UTC);
            Task task = Task.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .failure(failure)
                    .build();

            Event event = eventFunction.apply(task);
            emitEvent(event);
        }
    }

    private void emitEvent(Event event) {
        eventConsumers.forEach(eventConsumer -> {
            try {
                eventConsumer.receiveEvent(event);
            } catch (Exception e) {
                log.error("Error sending event {}", event, e);
            }
        });
    }
}
