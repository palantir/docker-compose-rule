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

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.waiting.ClusterWait.Listener.DoNothingListener;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.joda.time.ReadableDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterWait {
    private static final Logger log = LoggerFactory.getLogger(ClusterWait.class);
    private final ClusterHealthCheck clusterHealthCheck;
    private final ReadableDuration timeout;
    private final Listener listener;

    public interface Listener {
        void becameHealthy();
        void timedOut();

        final class DoNothingListener implements Listener {
            static final DoNothingListener INSTANCE = new DoNothingListener();

            @Override
            public void becameHealthy() { }

            @Override
            public void timedOut() { }
        }
    }

    public ClusterWait(ClusterHealthCheck clusterHealthCheck, ReadableDuration timeout) {
        this(clusterHealthCheck, timeout, DoNothingListener.INSTANCE);
    }

    public ClusterWait(ClusterHealthCheck clusterHealthCheck, ReadableDuration timeout, Listener listener) {
        this.clusterHealthCheck = clusterHealthCheck;
        this.timeout = timeout;
        this.listener = listener;
    }

    public void waitUntilReady(Cluster cluster) {
        final AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure = new AtomicReference<>(
                Optional.empty());

        log.info("Waiting for cluster to be healthy");
        try {
            Awaitility.await()
                    .pollInterval(50, TimeUnit.MILLISECONDS)
                    .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                    .until(weHaveSuccess(cluster, lastSuccessOrFailure));
            listener.becameHealthy();
        } catch (ConditionTimeoutException e) {
            listener.timedOut();
            throw new IllegalStateException(serviceDidNotStartupExceptionMessage(lastSuccessOrFailure));
        }
    }

    private Callable<Boolean> weHaveSuccess(Cluster cluster,
            AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        return () -> {
            SuccessOrFailure successOrFailure = clusterHealthCheck.isClusterHealthy(cluster);
            lastSuccessOrFailure.set(Optional.of(successOrFailure));
            return successOrFailure.succeeded();
        };
    }

    private static String serviceDidNotStartupExceptionMessage(
            AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        String healthcheckFailureMessage = lastSuccessOrFailure.get()
                .flatMap(SuccessOrFailure::toOptionalFailureMessage)
                .orElse("The healthcheck did not finish before the timeout");

        return "The cluster failed to pass a startup check: " + healthcheckFailureMessage;
    }

}
