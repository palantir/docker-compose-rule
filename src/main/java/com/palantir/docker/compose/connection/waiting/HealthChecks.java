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

import com.palantir.docker.compose.connection.DockerPort;
import org.joda.time.Duration;

import java.util.function.Function;

import static org.joda.time.Duration.standardMinutes;

public class HealthChecks {
    private static final Duration DEFAULT_TIMEOUT = standardMinutes(2);

    public static HealthCheck toRespondOverHttp(int internalPort, Function<DockerPort, String> urlFunction) {
        return (container) -> container.waitForHttpPort(internalPort, urlFunction, DEFAULT_TIMEOUT);
    }

    public static HealthCheck toHaveAllPortsOpen() {
        return container -> container.waitForPorts(DEFAULT_TIMEOUT);
    }
}
