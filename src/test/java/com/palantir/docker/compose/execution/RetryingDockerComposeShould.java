package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.ContainerNames;
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
import static org.mockito.Mockito.when;

public class RetryingDockerComposeShould {
    private static final Object SUCCESS = null;

    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final RetryingDockerCompose retryingDockerCompose = new RetryingDockerCompose(2, dockerCompose);
    private final ContainerNames someContainerNames = new ContainerNames("hey");

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
    public void throw_the_last_exception_when_up_fails_more_times_than_the_specified_attempts() throws IOException, InterruptedException {
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

    @Test
    public void not_retry_if_the_ps_command_was_successful_and_return_the_correct_container_names() throws IOException, InterruptedException {
        when(dockerCompose.ps()).thenReturn(someContainerNames);

        assertThat(retryingDockerCompose.ps(), is(someContainerNames));
        verify(dockerCompose).ps();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void retry_ps_if_the_command_failed_once_and_return_the_last_container_names() throws IOException, InterruptedException {
        when(dockerCompose.ps()).thenAnswer(new MockitoMultiAnswer() {
            @Override
            protected Object firstCall(InvocationOnMock invocation) throws Exception {
                throw new DockerComposeExecutionException();
            }

            @Override
            protected Object secondCall(InvocationOnMock invocation) throws Exception {
                return someContainerNames;
            }
        });

        assertThat(retryingDockerCompose.ps(), is(someContainerNames));
        verify(dockerCompose, times(2)).ps();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void throw_the_last_exception_when_ps_fails_more_times_than_the_specified_attempts() throws IOException, InterruptedException {
        DockerComposeExecutionException finalException = new DockerComposeExecutionException();
        when(dockerCompose.ps()).thenAnswer(new MockitoMultiAnswer() {
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
            retryingDockerCompose.ps();
            fail("Should have caught exception");
        } catch (DockerComposeExecutionException actualException) {
            assertThat(actualException, is(finalException));
        }

        verify(dockerCompose, times(2)).ps();
        verifyNoMoreInteractions(dockerCompose);
    }
}
