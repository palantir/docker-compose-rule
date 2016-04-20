/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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
package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.ContainerNames;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RetryingDockerComposeShould {
    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final Retryer retryer = mock(Retryer.class);
    private final RetryingDockerCompose retryingDockerCompose = new RetryingDockerCompose(retryer, dockerCompose);
    private final ContainerNames someContainerNames = new ContainerNames("hey");
    @Before
    public void before() throws IOException, InterruptedException {
        retryerJustCallsOperation();
    }

    private void retryerJustCallsOperation() throws IOException, InterruptedException {
        when(retryer.runWithRetries(any(Retryer.RetryableDockerComposeOperation.class))).thenAnswer(invocation -> {
            Retryer.RetryableDockerComposeOperation operation = (Retryer.RetryableDockerComposeOperation) invocation.getArguments()[0];
            return operation.call();
        });
    }

    @Test
    public void calls_up_on_the_underlying_docker_compose() throws IOException, InterruptedException {
        retryingDockerCompose.up();

        verifyRetryerWasUsed();
        verify(dockerCompose).up();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void call_ps_on_the_underlying_docker_compose_and_returns_the_same_value() throws IOException, InterruptedException {
        when(dockerCompose.ps()).thenReturn(someContainerNames);

        assertThat(retryingDockerCompose.ps(), is(someContainerNames));

        verifyRetryerWasUsed();
        verify(dockerCompose).ps();
        verifyNoMoreInteractions(dockerCompose);
    }

    private void verifyRetryerWasUsed() throws IOException, InterruptedException {
        verify(retryer).runWithRetries(any(Retryer.RetryableDockerComposeOperation.class));
    }
}
