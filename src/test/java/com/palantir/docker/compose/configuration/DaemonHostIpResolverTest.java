package com.palantir.docker.compose.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.DaemonHostIpResolver.LOCALHOST;

import org.junit.Test;

public class DaemonHostIpResolverTest {

    @Test
    public void daemonReturnsLocalHost_withNull() throws Exception {
        assertThat(new DaemonHostIpResolver().resolveIp(null), is(LOCALHOST));
    }

    @Test
    public void daemonReturnsLocalHost_withBlank() throws Exception {
        assertThat(new DaemonHostIpResolver().resolveIp(""), is(LOCALHOST));
    }

    @Test
    public void daemonReturnsLocalHost_withArbitrary() throws Exception {
        assertThat(new DaemonHostIpResolver().resolveIp("arbitrary"), is(LOCALHOST));
    }

}