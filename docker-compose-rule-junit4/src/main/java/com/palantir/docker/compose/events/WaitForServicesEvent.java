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

public interface WaitForServicesEvent extends DockerComposeRuleEvent {

    @Value.Immutable
    interface WaitForServicesStarted extends WaitForServicesEvent, Started { }

    @Value.Immutable
    interface WaitForServicesSucceeded extends WaitForServicesEvent, Succeeded { }

    @Value.Immutable
    interface WaitForServicesFailed extends WaitForServicesEvent, Failed { }

    LifeCycleEvent.Factory2 FACTORY = new LifeCycleEvent.Factory2() {
        @Override
        public Started started() {
            return ImmutableWaitForServicesStarted.builder().build();
        }

        @Override
        public Succeeded succeeded() {
            return ImmutableWaitForServicesSucceeded.builder().build();
        }

        @Override
        public Failed failed(Exception exception) {
            return ImmutableWaitForServicesFailed.of(exception);
        }
    };
}
