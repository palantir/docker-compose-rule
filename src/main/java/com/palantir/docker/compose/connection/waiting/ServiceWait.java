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

import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceWait {
    private static final Logger log = LoggerFactory.getLogger(ServiceWait.class);
    private final List<Container> containers;
    private final List<String> containerNames;
    private final MultiServiceHealthCheck healthCheck;
    private final Duration timeout;

    public ServiceWait(Container container, SingleServiceHealthCheck healthCheck, Duration timeout) {
        this(ImmutableList.of(container), MultiServiceHealthCheck.fromSingleServiceHealthCheck(healthCheck), timeout);
    }

    public ServiceWait(List<Container> containers, MultiServiceHealthCheck healthCheck, Duration timeout) {
        this.containers = containers;
        this.healthCheck = healthCheck;
        this.timeout = timeout;
        containerNames = containers.stream()
                .map(Container::getContainerName)
                .collect(Collectors.toList());
    }

    public void waitTillServiceIsUp(Cluster cluster) {
        log.debug("Waiting for services [{}]", containerNames());
        final AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure = new AtomicReference<>(Optional.empty());
        try {
            Awaitility.await()
                    .pollInterval(50, TimeUnit.MILLISECONDS)
                    .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                    .until(weHaveSuccess(lastSuccessOrFailure));
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException(serviceDidNotStartupExceptionMessage(lastSuccessOrFailure));
        }
    }

    private Callable<Boolean> weHaveSuccess(AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        return () -> {
            SuccessOrFailure successOrFailure = healthCheck.areServicesUp(containers);
            lastSuccessOrFailure.set(Optional.of(successOrFailure));
            return successOrFailure.succeeded();
        };
    }

    private String serviceDidNotStartupExceptionMessage(AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        String healthcheckFailureMessage = lastSuccessOrFailure.get()
                .flatMap(SuccessOrFailure::toOptionalFailureMessage)
                .orElse("The healthcheck did not finish before the timeout");

        return String.format("%s '%s' failed to pass startup check:%n%s",
            containers.size() > 1 ? "Containers" : "Container",
            containerNames(),
            healthcheckFailureMessage);
    }

    private String containerNames() {
        return containers.stream()
                .map(Container::getContainerName)
                .collect(joining(", "));

    }
}
