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

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.palantir.docker.compose.connection.Container;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ServiceWait {
    private static final Logger log = LoggerFactory.getLogger(ServiceWait.class);
    private final Container service;
    private final HealthCheck healthCheck;
    private final Duration timeout;

    public ServiceWait(Container service, HealthCheck healthCheck, Duration timeout) {
        this.service = service;
        this.healthCheck = healthCheck;
        this.timeout = timeout;
    }

    public void waitTillServiceIsUp() {
        log.debug("Waiting for service '{}'", service);
        try {
            Awaitility.await()
                    .pollInterval(50, TimeUnit.MILLISECONDS)
                    .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                    .until(() -> healthCheck.isServiceUp(service));
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException("Container '" + service.getContainerName() + "' failed to pass startup check");
        }
    }
}
