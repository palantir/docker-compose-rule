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

public class AggressiveShutdownWithNetworkCleanupStrategyTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final DockerCompose mockDockerCompose = mock(DockerCompose.class);
    private final Docker mockDocker = mock(Docker.class);

    private static final String error_msg = "Random DockerExecutionException message";

    @Test
    public void docker_compose_down_should_be_called_despite_docker_rm_throwing_exception() throws Exception {
        doThrow(new DockerExecutionException(error_msg)).when(mockDocker).rm(anyListOf(String.class));

        ShutdownStrategy.AGGRESSIVE_WITH_NETWORK_CLEANUP.shutdown(mockDockerCompose, mockDocker);

        verify(mockDockerCompose, times(1)).down();
    }
}
