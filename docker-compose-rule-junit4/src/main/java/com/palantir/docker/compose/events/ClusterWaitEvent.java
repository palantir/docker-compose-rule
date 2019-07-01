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

package com.palantir.docker.compose.events;

import com.palantir.docker.compose.events.LifeCycleEvent.Failed;
import com.palantir.docker.compose.events.LifeCycleEvent.Started;
import com.palantir.docker.compose.events.LifeCycleEvent.Succeeded;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.immutables.value.Value;

public interface ClusterWaitEvent extends DockerComposeRuleEvent {

    ClusterWaitEventId eventId();
    ClusterWaitType clusterWaitType();

    enum ClusterWaitType {
        NATIVE,
        USER
    }

    @Value.Immutable
    interface ClusterWaitEventId {
        String eventId();

        static ClusterWaitEventId of(String eventId) {
            return ImmutableClusterWaitEventId.builder()
                    .eventId(eventId)
                    .build();
        }

        static ClusterWaitEventId random() {
            return of(RandomStringUtils.randomAlphabetic(8));
        }
    }

    @Value.Immutable
    interface ClusterStarted extends ClusterWaitEvent, Started { }

    @Value.Immutable
    interface ClusterBecameHealthy extends ClusterWaitFinishedEvent, Succeeded { }

    @Value.Immutable
    interface ClusterTimedOut extends ClusterWaitFinishedEvent, Failed { }

    interface ClusterWaitFinishedEvent extends ClusterWaitEvent {
        Set<String> serviceNames();
    }

    static LifeCycleEvent.Factory2 factory(Supplier<Set<String>> serviceNames, ClusterWaitType clusterWaitType) {
        ClusterWaitEventId eventId = ClusterWaitEventId.random();

        return new LifeCycleEvent.Factory2() {
            @Override
            public Started started() {
                return ImmutableClusterStarted.builder()
                        .eventId(eventId)
                        .clusterWaitType(clusterWaitType)
                        .build();
            }

            @Override
            public Succeeded succeeded() {
                return ImmutableClusterBecameHealthy.builder()
                        .eventId(eventId)
                        .serviceNames(serviceNames.get())
                        .clusterWaitType(clusterWaitType)
                        .build();
            }

            @Override
            public Failed failed(Exception exception) {
                return ImmutableClusterTimedOut.builder()
                        .eventId(eventId)
                        .serviceNames(serviceNames.get())
                        .clusterWaitType(clusterWaitType)
                        .exception(exception)
                        .build();
            }
        };
    }
}
