/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection.waiting;

import static com.palantir.docker.compose.DockerComposeRule.DEFAULT_TIMEOUT;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import org.junit.Before;
import org.junit.Test;

public class SingleServiceWaitTest {

    private Cluster cluster = mock(Cluster.class);
    private SingleServiceHealthCheck healthCheck = mock(SingleServiceHealthCheck.class);
    private Container someContainer = mock(Container.class, "somecontainer");
    private SingleServiceWait wait = SingleServiceWait.of("somecontainer", healthCheck, DEFAULT_TIMEOUT);

    @Before
    public void before() {
        when(someContainer.getContainerName()).thenReturn("somecontainer");
        when(cluster.container("somecontainer")).thenReturn(someContainer);
        when(healthCheck.isServiceUp(any())).thenReturn(SuccessOrFailure.success());
    }

    @Test
    public void isReadyDelegatesToServiceWait() {
        wait.waitUntilReady(cluster);
        verify(healthCheck, times(1)).isServiceUp(someContainer);
    }
}
