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
    public void call_kill_on_stop() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);

        ShutdownStrategy.KILL_DOWN.stop(dockerCompose);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).kill();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void call_down_on_down() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);

        ShutdownStrategy.KILL_DOWN.down(dockerCompose);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).down();
        inOrder.verifyNoMoreInteractions();
    }
}
