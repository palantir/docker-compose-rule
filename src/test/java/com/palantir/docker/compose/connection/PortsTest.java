package com.palantir.docker.compose.connection;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PortsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerPort port = mock(DockerPort.class);
    private final Ports ports = new Ports(port);

    @Before
    public void setup() {
        when(port.getInternalPort()).thenReturn(7001);
        when(port.getExternalPort()).thenReturn(7000);
    }

    @Test
    public void whenAllPortsAreListeningWaitToBeListeningReturnsWithoutException() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(true);
        ports.waitToBeListeningWithin(Duration.millis(200));
    }

    @Test
    public void whenPortIsUnavailableWaitToBeListeningThrowsAnISE() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(false);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Internal port '7001' mapped to '7000' was unavailable");
        ports.waitToBeListeningWithin(Duration.millis(200));
    }

    @Test
    public void whenPortBecomesAvailableAfterAWaitWaitToBeListeningReturnsWithoutException() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(false, true);
        ports.waitToBeListeningWithin(Duration.millis(200));
    }

}
