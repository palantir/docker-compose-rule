/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.docker.compose;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AggressiveShutdownStrategyTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final DockerCompose mockDockerCompose = mock(DockerCompose.class);
    private final Docker mockDocker = mock(Docker.class);

    private static final String btrfs_message = "'docker rm -f test-1.container.name test-2.container.name' "
            + "returned exit code 1\nThe output was:\nFailed to remove container (test-1.container.name): "
            + "Error response from daemon: Driver btrfs failed to remove root filesystem ";

    @Test
    public void first_btrfs_error_should_be_caught_silently_and_retried() throws Exception {
        doThrow(new DockerExecutionException(btrfs_message))
                .doNothing()
                .when(mockDocker)
                .rm(anyListOf(String.class));

        ShutdownStrategy.AGGRESSIVE.shutdown(mockDockerCompose, mockDocker);

        verify(mockDocker, times(2)).rm(anyListOf(String.class));
    }

    @Test
    public void after_two_btrfs_failures_we_should_just_log_and_continue() throws Exception {
        doThrow(new DockerExecutionException(btrfs_message))
                .doThrow(new DockerExecutionException(btrfs_message))
                .when(mockDocker)
                .rm(anyListOf(String.class));

        ShutdownStrategy.AGGRESSIVE.shutdown(mockDockerCompose, mockDocker);

        verify(mockDocker, times(2)).rm(anyListOf(String.class));
    }

}
