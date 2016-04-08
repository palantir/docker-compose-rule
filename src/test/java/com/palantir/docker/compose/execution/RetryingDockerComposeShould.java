package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.utils.MockitoMultiAnswer;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RetryingDockerComposeShould {
    private static final Object SUCCESS = null;

    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final RetryingDockerCompose retryingDockerCompose = new RetryingDockerCompose(2, dockerCompose);

    @Test
    public void not_retry_if_the_up_command_was_successful() throws IOException, InterruptedException {
        retryingDockerCompose.up();
        verify(dockerCompose).up();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void retry_up_if_the_command_failed_once() throws IOException, InterruptedException {
        doAnswer(new MockitoMultiAnswer() {
            @Override
            protected Object firstCall(InvocationOnMock invocation) throws Exception {
                throw new DockerComposeExecutionException();
            }

            @Override
            protected Object secondCall(InvocationOnMock invocation) throws Exception {
                return SUCCESS;
            }
        }).when(dockerCompose).up();

        retryingDockerCompose.up();
        verify(dockerCompose, times(2)).up();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void throw_the_last_exception_when_failing_more_times_than_the_specified_attempts() throws IOException, InterruptedException {
        DockerComposeExecutionException finalException = new DockerComposeExecutionException();
        doAnswer(new MockitoMultiAnswer() {
            @Override
            protected Object firstCall(InvocationOnMock invocation) throws Exception {
                throw new DockerComposeExecutionException();
            }

            @Override
            protected Object secondCall(InvocationOnMock invocation) throws Exception {
                throw finalException;
            }
        }).when(dockerCompose).up();

        try {
            retryingDockerCompose.up();
            fail("Should have caught exception");
        } catch (DockerComposeExecutionException actualException) {
            assertThat(actualException, is(finalException));
        }

        verify(dockerCompose, times(2)).up();
        verifyNoMoreInteractions(dockerCompose);
    }
}
