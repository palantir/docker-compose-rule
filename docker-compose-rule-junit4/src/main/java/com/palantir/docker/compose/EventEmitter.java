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

import com.palantir.docker.compose.events.DockerComposeRuleEvent;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.events.pull.ExplicitPullImagesEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventEmitter {
    private static final Logger log = LoggerFactory.getLogger(EventEmitter.class);

    private final List<EventConsumer> statsConsumers;

    public EventEmitter(List<EventConsumer> statsConsumers) {
        this.statsConsumers = statsConsumers;
    }

    private void emitEvent(DockerComposeRuleEvent event) {
        statsConsumers.forEach(eventConsumer -> {
            try {
                eventConsumer.receiveEvent(event);
            } catch (Exception e) {
                log.error("Error sending event {}", event, e);
            }
        });
    }

    interface CheckedRunnable {
        void run() throws Exception;
    }

    public void pull(CheckedRunnable runnable) {
        try {
            emitEvent(ExplicitPullImagesEvent.Started.create());
            runnable.run();
            emitEvent(ExplicitPullImagesEvent.Succeeded.create());
        } catch (Exception e) {
            emitEvent(ExplicitPullImagesEvent.Failed.create(e));
        }
    }
}
