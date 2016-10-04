/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AggressiveShutdownStrategyTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final DockerComposeRule rule = mock(DockerComposeRule.class);
    private final Docker mockDocker = mock(Docker.class);

    private static final String btrfs_message = "'docker rm -f test-1.container.name test-2.container.name' "
            + "returned exit code 1\nThe output was:\nFailed to remove container (test-1.container.name): "
            + "Error response from daemon: Driver btrfs failed to remove root filesystem ";

    @Before
    public void before() {
        when(rule.dockerCompose()).thenReturn(mock(DockerCompose.class));
        when(rule.docker()).thenReturn(mockDocker);
    }

    @Test
    public void first_btrfs_error_should_be_caught_silently_and_retried() throws Exception {
        doThrow(new DockerExecutionException(btrfs_message))
                .doNothing()
                .when(mockDocker)
                .rm(anyListOf(String.class));

        ShutdownStrategy.AGGRESSIVE.shutdown(rule);

        verify(mockDocker, times(2)).rm(anyListOf(String.class));
    }

    @Test
    public void after_two_btrfs_failures_we_should_just_log_and_continue() throws Exception {
        doThrow(new DockerExecutionException(btrfs_message))
                .doThrow(new DockerExecutionException(btrfs_message))
                .when(mockDocker)
                .rm(anyListOf(String.class));

        ShutdownStrategy.AGGRESSIVE.shutdown(rule);

        verify(mockDocker, times(2)).rm(anyListOf(String.class));
    }

    @Test
    public void normal_rm_errors_should_be_rethrown() throws Exception {
        exception.expectMessage("Some other error");

        doThrow(new DockerExecutionException("Some other error")).when(mockDocker).rm(anyListOf(String.class));
        ShutdownStrategy.AGGRESSIVE.shutdown(rule);
    }

    @Test
    public void after_a_btrfs_failure_a_real_exception_should_be_rethrown() throws Exception {
        exception.expectMessage("real exception");

        doThrow(new DockerExecutionException(btrfs_message))
                .doThrow(new DockerExecutionException("real exception"))
                .when(mockDocker)
                .rm(anyListOf(String.class));

        ShutdownStrategy.AGGRESSIVE.shutdown(rule);
    }

}
