/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import org.junit.Test;
import org.mockito.InOrder;

public class GracefulShutdownStrategyShould {

    @Test
    public void call_down_then_kill_then_rm() throws Exception {
        DockerComposeRule rule = mock(DockerComposeRule.class);
        DockerCompose dockerCompose = mock(DockerCompose.class);
        when(rule.dockerCompose()).thenReturn(dockerCompose);
        ShutdownStrategy.GRACEFUL.shutdown(rule);

        InOrder inOrder = inOrder(dockerCompose);
        inOrder.verify(dockerCompose).down();
        inOrder.verify(dockerCompose).kill();
        inOrder.verify(dockerCompose).rm();
        inOrder.verifyNoMoreInteractions();
    }
}
