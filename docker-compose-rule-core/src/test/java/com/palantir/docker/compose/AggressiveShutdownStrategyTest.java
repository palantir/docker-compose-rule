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

public class AggressiveShutdownStrategyTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final DockerCompose mockDockerCompose = mock(DockerCompose.class);

    private static final String btrfs_message = "'docker rm -f test-1.container.name test-2.container.name' "
            + "returned exit code 1\nThe output was:\nFailed to remove container (test-1.container.name): "
            + "Error response from daemon: Driver btrfs failed to remove root filesystem ";

    @Test
    public void first_btrfs_error_should_be_caught_silently_and_retried() throws Exception {
        doThrow(new DockerExecutionException(btrfs_message))
                .doNothing()
                .when(mockDockerCompose)
                .rm();

        ShutdownStrategy.AGGRESSIVE.shutdown(mockDockerCompose);

        verify(mockDockerCompose, times(2)).rm();
    }

    @Test
    public void after_two_btrfs_failures_we_should_just_log_and_continue() throws Exception {
        doThrow(new DockerExecutionException(btrfs_message))
                .doThrow(new DockerExecutionException(btrfs_message))
                .when(mockDockerCompose)
                .rm();

        ShutdownStrategy.AGGRESSIVE.shutdown(mockDockerCompose);

        verify(mockDockerCompose, times(2)).rm();
    }

}
