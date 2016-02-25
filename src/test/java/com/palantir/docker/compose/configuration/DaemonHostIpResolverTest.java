package com.palantir.docker.compose.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.HostIpResolver.DAEMON;

import org.junit.Test;

public class DaemonHostIpResolverTest {

    private static final String LOCALHOST = "127.0.0.1";

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