/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import org.junit.Test;
import org.mockito.InOrder;

public class GracefulShutdownStrategyShould {

    @Test
    public void call_stop_then_kill_on_stop() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);

        ShutdownStrategy.GRACEFUL.stop(dockerCompose);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).stop();
        inOrder.verify(dockerCompose).kill();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void call_down_on_down() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);

        ShutdownStrategy.GRACEFUL.down(dockerCompose);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).down();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void do_nothing_on_shutdown() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);
        Docker docker = mock(Docker.class);

        ShutdownStrategy.GRACEFUL.shutdown(dockerCompose, docker);

        verifyZeroInteractions(dockerCompose, docker);
    }
}
