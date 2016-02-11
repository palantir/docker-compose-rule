package com.palantir.docker.compose.connection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;

public class DockerMachineBuilderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testBuildMachineWithoutHostResultsInError() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables");
        exception.expectMessage("DOCKER_HOST");
        DockerMachine.builder()
                     .certPath("/path/to/certs")
                     .withoutTLS()
                     .build();
    }

    @Test
    public void testDockerMachineGeneratesCorrectEnvironmentVariables_noTLS() throws Exception {
        DockerMachine dockerMachine = DockerMachine.builder()
                                                   .host("tcp://192.168.99.100")
                                                   .withoutTLS()
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                                                   .put(DOCKER_HOST, "tcp://192.168.99.100")
                                                   .put(DOCKER_TLS_VERIFY, "0")
                                                   .build();
        Map<String, String> dockerEnvironmentVariables = dockerMachine.getDockerEnvironmentVariables();
        assertThat(dockerEnvironmentVariables, is(expected));
    }

    @Test
    public void testDockerMachineGeneratesCorrectEnvironmentVariables_withTLS() throws Exception {
        DockerMachine dockerMachine = DockerMachine.builder()
                                                   .host("tcp://192.168.99.100")
                                                   .withTLS()
                                                   .certPath("/path/to/certs")
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();
        Map<String, String> dockerEnvironmentVariables = dockerMachine.getDockerEnvironmentVariables();
        assertThat(dockerEnvironmentVariables, is(expected));
    }

    @Test
    public void testDockerMachineWithoutCertPathWithTLS_isError() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables");
        exception.expectMessage(DOCKER_CERT_PATH);
        DockerMachine.builder()
                     .host("tcp://192.168.99.100")
                     .withTLS()
                     .build();
    }

    @Test
    public void testDockerMachineWithCertPathWithoutTLS_isError() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
        DockerMachine.builder()
                     .host("tcp://192.168.99.100")
                     .withoutTLS()
                     .certPath("/path/to/certs")
                     .build();
    }
}