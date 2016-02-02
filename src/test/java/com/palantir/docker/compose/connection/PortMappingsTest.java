package com.palantir.docker.compose.connection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.core.Is.is;

public class PortMappingsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void noPortsInPsOutputResultsInNoPorts() throws IOException, InterruptedException {
        String psOutput = "------";
        Iterable<PortMapping> ports = PortMappings.parseFromDockerComposePs(psOutput);
        assertThat(ports, is(emptyIterable()));
    }

    @Test
    public void singleTcpPortMappingResultsInSinglePort() throws IOException, InterruptedException {
        String psOutput = ":5432->5432/tcp";
        Iterable<PortMapping> ports = PortMappings.parseFromDockerComposePs(psOutput);
        assertThat(ports, contains(new PortMapping(5432, 5432)));
    }

    @Test
    public void twoTcpPortMappingsResultsInTwoPorts() throws IOException, InterruptedException {
        String psOutput = ":5432->5432/tcp, 0.0.0.0:5433->5432/tcp";
        Iterable<PortMapping> ports = PortMappings.parseFromDockerComposePs(psOutput);
        assertThat(ports, contains(new PortMapping(5432, 5432), new PortMapping(5433, 5432)));
    }

    @Test
    public void nonMappedExposedPortResultsInNoPorts() throws IOException, InterruptedException {
        String psOutput = "5432/tcp";
        Iterable<PortMapping> ports = PortMappings.parseFromDockerComposePs(psOutput);
        assertThat(ports, is(emptyIterable()));
    }

    @Test
    public void actualDockerComposeOutputCanBeParsed() throws IOException, InterruptedException {
        String psOutput =
                "       Name                      Command               State                                         Ports                                        \n" +
                "-------------------------------------------------------------------------------------------------------------------------------------------------\n" +
                "magritte_magritte_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:7000->7000/tcp, 7001/tcp, 7002/tcp, 7003/tcp, 7004/tcp, 7005/tcp, 7006/tcp \n" +
                "";
        Iterable<PortMapping> ports = PortMappings.parseFromDockerComposePs(psOutput);
        assertThat(ports, contains(new PortMapping(7000, 7000)));
    }

    @Test
    public void noRunningContainerFoundForServiceResultsInAnISE() throws IOException, InterruptedException {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No container found");
        PortMappings.parseFromDockerComposePs("");
    }

}
