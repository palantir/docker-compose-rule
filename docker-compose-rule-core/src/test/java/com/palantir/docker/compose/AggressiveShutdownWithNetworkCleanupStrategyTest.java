/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AggressiveShutdownWithNetworkCleanupStrategyTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final DockerCompose mockDockerCompose = mock(DockerCompose.class);

    private static final String error_msg = "Random DockerExecutionException message";

    @Test
    public void docker_compose_down_should_be_called_despite_docker_rm_throwing_exception() throws Exception {
        doThrow(new DockerExecutionException(error_msg))
                .when(mockDockerCompose)
                .rm();

        ShutdownStrategy.AGGRESSIVE_WITH_NETWORK_CLEANUP.shutdown(mockDockerCompose);

        verify(mockDockerCompose, times(1)).down();
    }

}
