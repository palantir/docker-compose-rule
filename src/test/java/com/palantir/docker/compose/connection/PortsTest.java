package com.palantir.docker.compose.connection;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PortsTest {

    public static final String LOCALHOST_IP = "127.0.0.1";
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerPort port = mock(DockerPort.class);

    @Before
    public void setup() {
        when(port.getInternalPort()).thenReturn(7001);
        when(port.getExternalPort()).thenReturn(7000);
    }

    @Test
    public void noPortsInPsOutputResultsInNoPorts() throws IOException, InterruptedException {
        String psOutput = "------";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, null);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void singleTcpPortMappingResultsInSinglePort() throws IOException, InterruptedException {
        String psOutput = "0.0.0.0:5432->5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void singleTcpPortMappingResultsInSinglePortWithIpOtherThanLocalhost() throws IOException, InterruptedException {
        String psOutput = "10.0.1.2:1234->2345/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort("10.0.1.2", 1234, 2345)));
        assertThat(ports, is(expected));
    }

    @Test
    public void twoTcpPortMappingsResultsInTwoPorts() throws IOException, InterruptedException {
        String psOutput = "0.0.0.0:5432->5432/tcp, 0.0.0.0:5433->5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432),
                                                new DockerPort(LOCALHOST_IP, 5433, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void nonMappedExposedPortResultsInNoPorts() throws IOException, InterruptedException {
        String psOutput = "5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void actualDockerComposeOutputCanBeParsed() throws IOException, InterruptedException {
        String psOutput =
                "       Name                      Command               State                                         Ports                                        \n" +
                        "-------------------------------------------------------------------------------------------------------------------------------------------------\n" +
                        "magritte_magritte_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:7000->7000/tcp, 7001/tcp, 7002/tcp, 7003/tcp, 7004/tcp, 7005/tcp, 7006/tcp \n" +
                        "";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 7000, 7000)));
        assertThat(ports, is(expected));
    }

    @Test
    public void noRunningContainerFoundForServiceResultsInAnISE() throws IOException, InterruptedException {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No container found");
        Ports.parseFromDockerComposePs("", "");
    }


    @Test
    public void whenAllPortsAreListeningWaitToBeListeningReturnsWithoutException() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(true);
        new Ports(port).waitToBeListeningWithin(Duration.millis(200));
    }

    @Test
    public void whenPortIsUnavailableWaitToBeListeningThrowsAnISE() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(false);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("ConditionTimeoutException"); // Bug in awaitility means it doesn't call hamcrest describeMismatch, this will be "Internal port '7001' mapped to '7000'" was unavailable in practice
        new Ports(port).waitToBeListeningWithin(Duration.millis(200));
    }

    @Test
    public void whenPortBecomesAvailableAfterAWaitWaitToBeListeningReturnsWithoutException() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(false, true);
        new Ports(port).waitToBeListeningWithin(Duration.millis(200));
    }

}
