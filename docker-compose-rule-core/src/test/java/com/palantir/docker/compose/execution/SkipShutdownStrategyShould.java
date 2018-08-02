/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import org.junit.Test;

public class SkipShutdownStrategyShould {

    @Test
    public void stop_call_nothing() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);

        ShutdownStrategy.SKIP.stop(dockerCompose);

        verifyZeroInteractions(dockerCompose);
    }

    @Test
    public void shutdown_call_nothing() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);

        ShutdownStrategy.SKIP.shutdown(dockerCompose);

        verifyZeroInteractions(dockerCompose);
    }
}
