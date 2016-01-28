package com.palantir.docker.compose;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class DockerEnvironmentVariablesTest {

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

    @Test(expected = IllegalStateException.class)
    public void testTlsDisabledButCertPathPresent() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, "host");
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");
        env.put(DockerEnvironmentVariables.DOCKER_CERT_PATH, "/path/to/certs");

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test(expected = IllegalStateException.class)
    public void testTlsEnabledButCertPathMissing() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_HOST, "host");
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "1");

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingHost() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");
        env.put(DockerEnvironmentVariables.DOCKER_CERT_PATH, "/path/to/certs");

        new DockerEnvironmentVariables(env).checkEnvVariables();
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingHostAndMissingDockerCertPathTlsVerifyDisabled() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerEnvironmentVariables.DOCKER_TLS_VERIFY, "0");

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