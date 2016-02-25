package com.palantir.docker.compose.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.HostIpResolver.DAEMON;
import static com.palantir.docker.compose.configuration.HostIpResolver.LOCALHOST;

import org.junit.Test;

public class DaemonHostIpResolverTest {

    @Test
    public void daemonReturnsLocalHost_withNull() throws Exception {
        assertThat(DAEMON.resolveIp(null), is(LOCALHOST));
    }

    @Test
    public void daemonReturnsLocalHost_withBlank() throws Exception {
        assertThat(DAEMON.resolveIp(""), is(LOCALHOST));
    }

    @Test
    public void daemonReturnsLocalHost_withArbitrary() throws Exception {
        assertThat(DAEMON.resolveIp("arbitrary"), is(LOCALHOST));
    }

}