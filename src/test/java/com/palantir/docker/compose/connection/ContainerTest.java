package com.palantir.docker.compose.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.executing.DockerComposeExecutable;
import java.io.IOException;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ContainerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerComposeExecutable dockerComposeProcess = mock(DockerComposeExecutable.class);
    private final DockerMachine dockerMachine = mock(DockerMachine.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerComposeProcess, dockerMachine);
    private final Container service = new Container("service", dockerComposeProcess, dockerMachine);

    @Test
    public void waitingForAContainersPortsWaitsForThePortsFromDockerComposePsToBeAvailable() throws IOException, InterruptedException {
        DockerPort port = env.availableService("service", 5433, 5432);
        assertThat(service.waitForPorts(Duration.millis(100)), is(true));
        verify(port, atLeastOnce()).isListeningNow();
    }

    @Test
    public void waitForAContainersPortsReturnsFalseWhenThePortIsUnavailable() throws IOException, InterruptedException {
        env.unavailableService("service", 5433, 5432);
        assertThat(service.waitForPorts(Duration.millis(100)), is(false));
    }

    @Test
    public void waitingForAContainersHttpPortsWaitsForThePortsFromDockerComposePsToBeAvailable() throws IOException, InterruptedException {
        DockerPort port = env.availableHttpService("service", 5433, 5432);
        assertThat(service.waitForHttpPort(5432, (p) -> "url", Duration.millis(100)), is(true));
        verify(port, atLeastOnce()).isListeningNow();
    }

    @Test
    public void waitForAContainersHttpPortsReturnsFalseWhenThePortIsUnavailable() throws IOException, InterruptedException {
        env.unavailableService("service", 5433, 5432);
        assertThat(service.waitForHttpPort(5432, (p) -> "url", Duration.millis(100)), is(false));
    }

    @Test
    public void portIsReturnedForContainerWhenExternalPortNumberGiven() throws IOException, InterruptedException {
        DockerPort expected = env.availableService("service", 5433, 5432);
        DockerPort port = service.portMappedExternallyTo(5433);
        assertThat(port, is(expected));
    }

    @Test
    public void portIsReturnedForContainerWhenInternalPortNumberGiven() throws IOException, InterruptedException {
        DockerPort expected = env.availableService("service", 5433, 5432);
        DockerPort port = service.portMappedInternallyTo(5432);
        assertThat(port, is(expected));
    }

    @Test
    public void whenTwoPortsAreRequestedDockerPortsIsOnlyCalledOnce() throws IOException, InterruptedException {
        env.ports("service", 8080, 8081);
        service.portMappedInternallyTo(8080);
        service.portMappedInternallyTo(8081);
        verify(dockerComposeProcess, times(1)).ports("service");
    }

    @Test
    public void requestedAPortForAnUnknownExternalPortResultsInAnIAE() throws IOException, InterruptedException {
        env.availableService("service", 5400, 5400); // Service must have ports otherwise we end up with an exception telling you the service is listening at all
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No port mapped externally to '5432' for container 'service'");
        service.portMappedExternallyTo(5432);
    }

    @Test
    public void requestedAPortForAnUnknownInternalPortResultsInAnIAE() throws IOException, InterruptedException {
        env.availableService("service", 5400, 5400);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No internal port '5432' for container 'service'");
        service.portMappedInternallyTo(5432);
    }

}
