/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import org.junit.Test;
import org.mockito.InOrder;

public class KillDownShutdownStrategyShould {

    @Test
    public void call_kill_then_down() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);
        Docker docker = mock(Docker.class);

        ShutdownStrategy.KILL_DOWN.shutdown(dockerCompose, docker);

        InOrder inOrder = inOrder(dockerCompose, docker);
        inOrder.verify(dockerCompose).kill();
        inOrder.verify(dockerCompose).down();
        inOrder.verify(docker).pruneNetworks();
        inOrder.verifyNoMoreInteractions();
    }
}
