/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import org.junit.Test;
import org.mockito.InOrder;

public class GracefulShutdownStrategyShould {

    @Test
    public void call_down_then_kill_then_rm() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);
        Docker docker = mock(Docker.class);

        ShutdownStrategy.GRACEFUL.shutdown(dockerCompose, docker);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).down();
        inOrder.verify(dockerCompose).kill();
        inOrder.verify(dockerCompose).rm();
        inOrder.verifyNoMoreInteractions();
    }
}
