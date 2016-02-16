package com.palantir.docker.compose.configuration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_TLS_VERIFY;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.TCP_PROTOCOL;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;

public class DockerEnvironmentVariablesTest {

    public static final String HOST_IP = "192.168.99.100";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void environmentWithoutTLSExplicitlySet() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "host")
                .build();

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void environmentWithTLSExplicitlyDisabled() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "host")
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void environmentWithTLSDisabledButCertPathPresent() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "host")
                .put(DOCKER_TLS_VERIFY, "0")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
        new DockerEnvironmentVariables(env).checkEnvVariables();
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

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void environmentWithoutDockerHostSet() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Missing");
        expectedException.expectMessage(DOCKER_HOST);
        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void environmentWithDockerHostContainingPortReturnsIpPortionOfDockerHost() throws Exception {
        String ipAndPort = String.format("%s:%s", HOST_IP, 2376);

        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, TCP_PROTOCOL + ipAndPort)
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        String hostIp = new DockerEnvironmentVariables(env).getDockerHostIp();

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

        Map<String, String> dockerEnvironmentVariables = new DockerEnvironmentVariables(env).getDockerEnvironmentVariables();
        assertThat(dockerEnvironmentVariables, is(expected));
    }
}
