package com.palantir.docker.compose.connection;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_HOST;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;

public class RemoteBuilderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void aDockerMachineBuiltWithoutAHostResultsInAnError() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables");
        exception.expectMessage("DOCKER_HOST");
        DockerMachine.remoteMachine()
                     .withoutTLS()
                     .build();
    }

    @Test
    public void aDockerMachineBuiltWithoutTLSHasNoTLSEnvironmentVariables() throws Exception {
        DockerMachine dockerMachine = DockerMachine.remoteMachine()
                                                   .host("tcp://192.168.99.100")
                                                   .withoutTLS()
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                                                   .put(DOCKER_HOST, "tcp://192.168.99.100")
                                                   .build();

        validateEnvironmentConfiguredDirectly(dockerMachine, expected);
    }

    @Test
    public void aDockerMachineBuiltWithTLSHasTLSEnvironmentVariablesSet() throws Exception {
        DockerMachine dockerMachine = DockerMachine.remoteMachine()
                                                   .host("tcp://192.168.99.100")
                                                   .withTLS("/path/to/certs")
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();
        validateEnvironmentConfiguredDirectly(dockerMachine, expected);
    }

    @Test
    public void aDockerMachineBuiltWithAdditionalEnvironmentVariables() throws Exception {
        DockerMachine dockerMachine = DockerMachine.remoteMachine()
                                                   .host("tcp://192.168.99.100")
                                                   .withoutTLS()
                                                   .withAdditionalEnvironmentVariable("SOME_VARIABLE", "SOME_VALUE")
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100")
                .put("SOME_VARIABLE", "SOME_VALUE")
                .build();
        validateEnvironmentConfiguredDirectly(dockerMachine, expected);
    }

    private void validateEnvironmentConfiguredDirectly(DockerMachine dockerMachine, Map<String, String> expectedEnvironment) {
        ProcessBuilder process = dockerMachine.configDockerComposeProcess();

        Map<String, String> environment = process.environment();
        expectedEnvironment.forEach((var, val) -> assertThat(environment, hasEntry(var, val)));
    }

}