package com.palantir.docker.compose.configuration;

import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.EnvironmentVariables.TCP_PROTOCOL;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RemoteHostIpResolverTest {

    private final String IP = "192.168.99.100";
    private final int PORT = 2376;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void resolvingInvalidDockerHostResultsInError_blank() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("DOCKER_HOST cannot be blank/null");
        new RemoteHostIpResolver().resolveIp("");
    }

    @Test
    public void resolvingInvalidDockerHostResultsInError_null() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("DOCKER_HOST cannot be blank/null");
        new RemoteHostIpResolver().resolveIp(null);
    }

    @Test
    public void resolveDockerHost_withPort() throws Exception {
        String dockerHost = String.format("%s%s:%d", TCP_PROTOCOL, IP, PORT);
        assertThat(new RemoteHostIpResolver().resolveIp(dockerHost), Matchers.is(IP));
    }

    @Test
    public void resolveDockerHost_withoutPort() throws Exception {
        String dockerHost = String.format("%s%s", TCP_PROTOCOL, IP);
        assertThat(new RemoteHostIpResolver().resolveIp(dockerHost), Matchers.is(IP));
    }
}