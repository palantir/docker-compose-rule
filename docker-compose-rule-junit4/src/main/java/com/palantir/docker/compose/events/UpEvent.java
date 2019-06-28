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
import org.immutables.value.Value;

public interface UpEvent extends DockerComposeRuleEvent {

    @Value.Immutable
    interface UpStarted extends UpEvent, Started { }

    @Value.Immutable
    interface UpSucceeded extends UpEvent, Succeeded { }

    @Value.Immutable
    interface UpFailed extends UpEvent, Failed { }

    LifeCycleEvent.Factory2 FACTORY = new LifeCycleEvent.Factory2() {
        @Override
        public Started started() {
            return ImmutableUpStarted.builder().build();
        }

        @Override
        public Succeeded succeeded() {
            return ImmutableUpSucceeded.builder().build();
        }

        @Override
        public Failed failed(Exception exception) {
            return ImmutableUpFailed.of(exception);
        }
    };
}