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
    private final Retryer retryer = new Retryer(2);

    @Test
    public void not_retry_if_the_ps_command_was_successful_and_return_the_correct_container_names() throws IOException, InterruptedException {
        when(operation.call()).thenReturn("hi");

        assertThat(retryer.runWithRetries(operation), is("hi"));
        verify(operation).call();
    }

    @Test
    public void retry_ps_if_the_command_failed_once_and_return_the_last_container_names() throws IOException, InterruptedException {
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
    public void throw_the_last_exception_when_ps_fails_more_times_than_the_specified_attempts() throws IOException, InterruptedException {
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
