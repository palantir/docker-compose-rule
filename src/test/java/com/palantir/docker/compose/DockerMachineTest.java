package com.palantir.docker.compose;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class DockerMachineTest {

    @Test
    public void testTlsMissingAndCertPathMissing() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerMachine.DOCKER_HOST, "host");
        DockerMachine.checkEnvVariables(env);
    }

    @Test
    public void testTlsDisabledAndCertPathMissing() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerMachine.DOCKER_HOST, "host");
        env.put(DockerMachine.DOCKER_TLS_VERIFY, "0");
        DockerMachine.checkEnvVariables(env);
    }

    @Test(expected = IllegalStateException.class)
    public void testTlsDisabledButCertPathPresent() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerMachine.DOCKER_HOST, "host");
        env.put(DockerMachine.DOCKER_TLS_VERIFY, "0");
        env.put(DockerMachine.DOCKER_CERT_PATH, "/path/to/certs");
        DockerMachine.checkEnvVariables(env);
    }

    @Test(expected = IllegalStateException.class)
    public void testTlsEnabledButCertPathMissing() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerMachine.DOCKER_HOST, "host");
        env.put(DockerMachine.DOCKER_TLS_VERIFY, "1");
        DockerMachine.checkEnvVariables(env);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingHost() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerMachine.DOCKER_TLS_VERIFY, "0");
        env.put(DockerMachine.DOCKER_CERT_PATH, "/path/to/certs");
        DockerMachine.checkEnvVariables(env);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingHost2() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put(DockerMachine.DOCKER_TLS_VERIFY, "0");
        DockerMachine.checkEnvVariables(env);
    }
}