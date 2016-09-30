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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Stopwatch;
import com.palantir.docker.compose.utils.MockitoMultiAnswer;
import java.util.concurrent.TimeUnit;
import org.joda.time.Duration;
import org.junit.Test;

public class RetryerShould {
    private final Retryer.RetryableDockerOperation<String> operation = mock(Retryer.RetryableDockerOperation.class);
    private final Retryer retryer = new Retryer(1, Duration.millis(0));

    @Test
    public void not_retry_if_the_operation_was_successful_and_return_result() throws Exception {
        when(operation.call()).thenReturn("hi");

        assertThat(retryer.runWithRetries(operation), is("hi"));
        verify(operation).call();
    }

    @Test
    public void retryer_should_wait_after_failure_before_trying_again() throws Exception {
        Retryer timeRetryer = new Retryer(1, Duration.millis(100));

        Stopwatch stopwatch = Stopwatch.createStarted();
        when(operation.call()).thenThrow(new DockerExecutionException()).thenAnswer(i -> {
            assertThat(stopwatch.elapsed(TimeUnit.MILLISECONDS), greaterThan(100L));
            return "success";
        });

        String result = timeRetryer.runWithRetries(operation);
        assertThat(result, is("success"));
    }

    @Test
    public void retry_the_operation_if_it_failed_once_and_return_the_result_of_the_next_successful_call() throws Exception {
        when(operation.call()).thenAnswer(MockitoMultiAnswer.<String>of(
                firstInvocation -> {
                    throw new DockerExecutionException();
                },
                secondInvocation -> "hola"
        ));

        assertThat(retryer.runWithRetries(operation), is("hola"));
        verify(operation, times(2)).call();
    }

    @Test
    public void throw_the_last_exception_when_the_operation_fails_more_times_than_the_number_of_specified_retry_attempts() throws Exception {
        DockerExecutionException finalException = new DockerExecutionException();

        when(operation.call()).thenAnswer(MockitoMultiAnswer.<String>of(
                firstInvocation -> {
                    throw new DockerExecutionException();
                },
                secondInvocation -> {
                    throw finalException;
                }
        ));

        try {
            retryer.runWithRetries(operation);
            fail("Should have caught exception");
        } catch (DockerExecutionException actualException) {
            assertThat(actualException, is(finalException));
        }

        verify(operation, times(2)).call();
    }
}
