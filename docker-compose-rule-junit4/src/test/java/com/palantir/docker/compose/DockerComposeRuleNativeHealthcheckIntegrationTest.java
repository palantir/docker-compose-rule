/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static com.jayway.awaitility.Awaitility.await;
import static com.palantir.docker.compose.execution.DockerComposeExecArgument.arguments;
import static com.palantir.docker.compose.execution.DockerComposeExecOption.noOptions;
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

public class DockerComposeRuleNativeHealthcheckIntegrationTest {

    private final ExecutorService pool = Executors.newFixedThreadPool(1);
    private DockerComposeRule rule = null;

    @After
    public void shutdownPool() {
        pool.shutdown();
        if (rule != null) {
            rule.after();
        }
    }

    @Test
    public void dockerComposeRuleWaitsUntilHealthcheckPasses()
            throws ExecutionException, IOException, InterruptedException, TimeoutException {
        rule = DockerComposeRule.builder()
                .file("src/test/resources/native-healthcheck.yaml")
                .build();
        Future<?> beforeFuture = pool.submit(() -> {
            rule.before();
            return null;
        });

        Container container = rule.containers().container("dummy");
        await().until(container::state, Matchers.equalTo(State.UNHEALTHY));

        // The "dummy" container should not initially pass its healthcheck
        try {
            getUninterruptibly(beforeFuture, 1, TimeUnit.SECONDS);
            fail("Expected before() to wait");
        } catch (TimeoutException e) {
            // Expected
        }

        // Touching the "healthy" file in the "dummy" container should make its healthcheck pass
        rule.dockerCompose().exec(noOptions(), "dummy", arguments("touch", "healthy"));
        await().until(container::state, Matchers.equalTo(State.HEALTHY));
        getUninterruptibly(beforeFuture, 1, TimeUnit.SECONDS);
    }
}
