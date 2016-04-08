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

import com.palantir.docker.compose.utils.MockitoMultiAnswer;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryerShould {
    private final Retryer.RetryableDockerComposeOperation<String> operation = mock(Retryer.RetryableDockerComposeOperation.class);
    private final Retryer retryer = new Retryer(1);

    @Test
    public void not_retry_if_the_operation_was_successful_and_return_result() throws IOException, InterruptedException {
        when(operation.call()).thenReturn("hi");

        assertThat(retryer.runWithRetries(operation), is("hi"));
        verify(operation).call();
    }

    @Test
    public void retry_the_operation_if_it_failed_once_and_return_the_result_of_the_next_successful_call() throws IOException, InterruptedException {
        when(operation.call()).thenAnswer(new MockitoMultiAnswer<String>() {
            @Override
            protected String firstCall(InvocationOnMock invocation) throws Exception {
                throw new DockerComposeExecutionException();
            }

            @Override
            protected String secondCall(InvocationOnMock invocation) throws Exception {
                return "hola";
            }
        });

        assertThat(retryer.runWithRetries(operation), is("hola"));
        verify(operation, times(2)).call();
    }

    @Test
    public void throw_the_last_exception_when_the_operation_fails_more_times_than_the_number_of_specified_retry_attempts() throws IOException, InterruptedException {
        DockerComposeExecutionException finalException = new DockerComposeExecutionException();
        when(operation.call()).thenAnswer(new MockitoMultiAnswer() {
            @Override
            protected Object firstCall(InvocationOnMock invocation) throws Exception {
                throw new DockerComposeExecutionException();
            }

            @Override
            protected Object secondCall(InvocationOnMock invocation) throws Exception {
                throw finalException;
            }
        });

        try {
            retryer.runWithRetries(operation);
            fail("Should have caught exception");
        } catch (DockerComposeExecutionException actualException) {
            assertThat(actualException, is(finalException));
        }

        verify(operation, times(2)).call();
    }
}
