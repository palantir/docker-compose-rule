package com.palantir.docker.compose;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DockerEnvironmentVariablesTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testTlsMissingAndCertPathMissing() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, "host");

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testTlsDisabledAndCertPathMissing() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, "host");
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testTlsDisabledButCertPathPresent() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, "host");
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");
        env.put(DockerEnvironmentVariables.DOCKER_CERT_PATH, "/path/to/certs");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(DockerEnvironmentVariables.CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testTlsEnabledButCertPathMissing() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, "host");
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "1");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Missing");
        expectedException.expectMessage(DockerEnvironmentVariables.DOCKER_CERT_PATH);

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testMissingHostWithTlsVerifyDisabledAndCertPathPresent() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");
        env.put(DockerEnvironmentVariables.DOCKER_CERT_PATH, "/path/to/certs");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(DockerEnvironmentVariables.CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testMissingHostAndMissingDockerCertPathTlsVerifyDisabled() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Missing");
        expectedException.expectMessage(DockerEnvironmentVariables.DOCKER_HOST);
        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test
    public void testGetHostIp() throws Exception {
        String protocol = DockerEnvironmentVariables.TCP_PROTOCOL;
        String ip = "192.168.99.100";
        String port = "2376";
        String ipAndPort = String.format("%s:%s", ip, port);

        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, protocol + ipAndPort);
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");

        String hostIp = new DockerEnvironmentVariables(env).getDockerHostIp();

        assertThat(hostIp, is(ip));
    }

    @Test
    public void testGetHostIp2() throws Exception {
        String protocol = DockerEnvironmentVariables.TCP_PROTOCOL;
        String ip = "192.168.99.100";

        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, protocol + ip);
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");

        String hostIp = new DockerEnvironmentVariables(env).getDockerHostIp();

        assertThat(hostIp, is(ip));
    }
}