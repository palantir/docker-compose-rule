/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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

import com.palantir.docker.compose.connection.Cluster;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClusterWait {
    private static final Logger log = LoggerFactory.getLogger(ClusterWait.class);
    private final ClusterHealthCheck clusterHealthCheck;
    private final Duration timeout;

    public ClusterWait(ClusterHealthCheck clusterHealthCheck, ReadableDuration timeout) {
        this.clusterHealthCheck = clusterHealthCheck;
        this.timeout = Duration.millis(timeout.getMillis());
    }

    public void waitUntilReady(Cluster cluster) {
        final AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure =
                new AtomicReference<>(Optional.empty());

        // semi-intelligent poll interval. If we specify a fast timeout, it will poll more often, otherwise poll
        // at a slower rate
        Duration pollInterval = minDuration(Duration.millis(500), timeout.dividedBy(20));

        try {
            Awaitility.await()
                    .pollInterval(java.time.Duration.ofMillis(pollInterval.getMillis()))
                    .pollDelay(java.time.Duration.ofMillis(
                            ThreadLocalRandom.current().nextInt(1, 50)))
                    .atMost(java.time.Duration.ofMillis(timeout.getMillis()))
                    .until(weHaveSuccess(cluster, lastSuccessOrFailure));
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException(serviceDidNotStartupExceptionMessage(lastSuccessOrFailure));
        }
    }

    private Callable<Boolean> weHaveSuccess(
            Cluster cluster, AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        return () -> {
            SuccessOrFailure successOrFailure = clusterHealthCheck.isClusterHealthy(cluster);
            lastSuccessOrFailure.set(Optional.of(successOrFailure));
            return successOrFailure.succeeded();
        };
    }

    private static String serviceDidNotStartupExceptionMessage(
            AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        String healthcheckFailureMessage = lastSuccessOrFailure
                .get()
                .flatMap(SuccessOrFailure::toOptionalFailureMessage)
                .orElse("The healthcheck did not finish before the timeout");

        return "The cluster failed to pass a startup check: " + healthcheckFailureMessage;
    }

    private static Duration minDuration(Duration first, Duration second) {
        if (first.isShorterThan(second)) {
            return first;
        }

        return second;
    }
}
