package com.palantir.docker.compose.configuration;

import org.junit.Test;

import static com.palantir.docker.compose.configuration.DaemonHostIpResolver.LOCALHOST;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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