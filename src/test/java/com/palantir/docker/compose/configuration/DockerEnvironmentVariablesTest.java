package com.palantir.docker.compose.configuration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testTlsMissingAndCertPathMissing() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_HOST, "host")
                .build();

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testTlsDisabledAndCertPathMissing() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_HOST, "host")
                .put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0")
                .build();

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testTlsDisabledButCertPathPresent() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_HOST, "host")
                .put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0")
                .put(DockerEnvironmentVariables.DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(DockerEnvironmentVariables.CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testTlsEnabledButCertPathMissing() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_HOST, "host")
                .put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "1")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Missing");
        expectedException.expectMessage(DockerEnvironmentVariables.DOCKER_CERT_PATH);

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testMissingHostWithTlsVerifyDisabledAndCertPathPresent() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0")
                .put(DockerEnvironmentVariables.DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(DockerEnvironmentVariables.CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testMissingHostAndMissingDockerCertPathTlsVerifyDisabled() throws Exception {
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0")
                .build();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Missing");
        expectedException.expectMessage(DockerEnvironmentVariables.DOCKER_HOST);
        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testGetHostIpWithPort() throws Exception {
        String ip = "192.168.99.100";
        String port = "2376";
        String ipAndPort = String.format("%s:%s", ip, port);

        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_HOST, TCP_PROTOCOL + ipAndPort)
                .put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0")
                .build();

        String hostIp = new DockerEnvironmentVariables(env).getDockerHostIp();

        assertThat(hostIp, is(ip));
    }

    @Test
    public void testGetHostIpWithoutPort() throws Exception {
        String ip = "192.168.99.100";

        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put(DockerEnvironmentVariables.DOCKER_HOST, TCP_PROTOCOL + ip)
                .put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0")
                .build();

        String hostIp = new DockerEnvironmentVariables(env).getDockerHostIp();

        assertThat(hostIp, is(ip));
    }

    @Test
    public void testGettingOnlyDockerRelatedEnvironmentVariables() throws Exception {
        String ip = "192.168.99.100";
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put("SOME_RANDOM_VARIABLE", "SOME_VALUE")
                .put(DOCKER_HOST, TCP_PROTOCOL + ip)
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, TCP_PROTOCOL + ip)
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .put(DOCKER_TLS_VERIFY, "0")
                .build();

        Map<String, String> dockerEnvironmentVariables = new DockerEnvironmentVariables(env).getDockerEnvironmentVariables();
        assertThat(dockerEnvironmentVariables, is(expected));
    }
}
