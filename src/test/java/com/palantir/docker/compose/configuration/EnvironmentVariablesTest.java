package com.palantir.docker.compose.configuration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_TLS_VERIFY;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.TCP_PROTOCOL;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class EnvironmentVariablesTest {

    public static final String HOST_IP = "192.168.99.100";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void environmentWithoutTLSExplicitlySet() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "host")
                .build();

        new EnvironmentVariables(env).augmentGivenEnvironment(Maps.newHashMap()); // assert that this is the same as or contains the same keys as env
    }

    @Test
    public void environmentWithTLSExplicitlyDisabled() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "host")
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        new EnvironmentVariables(env);
    }

    @Test
    public void environmentWithTLSEnabledWithoutACertPath() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "host")
                .put(DOCKER_TLS_VERIFY, "1")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Missing");
        expectedException.expectMessage(DOCKER_CERT_PATH);

        new EnvironmentVariables(env);
    }

    @Test
    public void environmentWithoutDockerHostSet() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Missing");
        expectedException.expectMessage(DOCKER_HOST);
        new EnvironmentVariables(env);
    }

    @Test
    public void environmentWithDockerHostContainingPortReturnsIpPortionOfDockerHost() throws Exception {
        String ipAndPort = String.format("%s:%s", HOST_IP, 2376);

        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, TCP_PROTOCOL + ipAndPort)
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        String hostIp = new EnvironmentVariables(env).getDockerHostIp();

        assertThat(hostIp, is(HOST_IP));
    }

    @Test
    public void environmentSpitsOutOnlyDockerRelatedEnvironmentVariables() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put("SOME_RANDOM_VARIABLE", "SOME_VALUE")
                .put(DOCKER_HOST, TCP_PROTOCOL + HOST_IP)
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, TCP_PROTOCOL + HOST_IP)
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        Map<String, String> dockerEnvironmentVariables = new EnvironmentVariables(env).getDockerEnvironmentVariables();
        assertThat(dockerEnvironmentVariables, is(expected));
    }
}
