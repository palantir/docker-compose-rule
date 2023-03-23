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

import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static com.palantir.docker.compose.execution.DockerComposeExecArgument.arguments;
import static com.palantir.docker.compose.execution.DockerComposeExecOption.noOptions;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.fail;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.State;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

public class DockerComposeManagerNativeHealthcheckIntegrationTest {

    private final ExecutorService pool = Executors.newFixedThreadPool(1);
    private DockerComposeManager docker = null;

    @After
    public void shutdownPool() {
        pool.shutdown();
        if (docker != null) {
            docker.after();
        }
    }

    @Test
    public void dockerComposeManagerWaitsUntilHealthcheckPasses()
            throws ExecutionException, IOException, InterruptedException, TimeoutException {
        docker = new DockerComposeManager.Builder()
                .file("src/test/resources/native-healthcheck.yaml")
                .build();
        Future<?> beforeFuture = pool.submit(() -> {
            docker.before();
            return null;
        });

        Container container = docker.containers().container("withHealthcheck");
        await().until(container::state, Matchers.equalTo(State.UNHEALTHY));

        // The "withHealthCheck" container should not initially pass its healthcheck
        try {
            getUninterruptibly(beforeFuture, 1, TimeUnit.SECONDS);
            fail("Expected before() to wait");
        } catch (TimeoutException e) {
            // Expected
        }

        // Touching the "healthy" file in the "withHealthCheck" container should make its healthcheck pass
        docker.dockerCompose().exec(noOptions(), "withHealthcheck", arguments("touch", "healthy"));
        await().until(container::state, Matchers.equalTo(State.HEALTHY));
        getUninterruptibly(beforeFuture, 1, TimeUnit.SECONDS);
    }
}
