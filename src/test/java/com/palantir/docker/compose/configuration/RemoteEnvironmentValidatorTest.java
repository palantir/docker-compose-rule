package com.palantir.docker.compose.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;

public class RemoteEnvironmentValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void dockerHostIsRequiredToBeSet() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                                                    .put("SOME_VARIABLE", "SOME_VALUE")
                                                    .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_HOST);
        RemoteEnvironmentValidator.validate(variables);
    }

    @Test
    public void dockerCertPathIsRequiredIfTlsIsOn() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .build();

        RemoteEnvironmentValidator validator = new RemoteEnvironmentValidator(variables);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_CERT_PATH);
        RemoteEnvironmentValidator.validate(variables);
    }

    @Test
    public void environmentWithAllValidVariablesSet_withoutTLS() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                                                    .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                    .put("SOME_VARIABLE", "SOME_VALUE")
                                                    .build();

        assertThat(RemoteEnvironmentValidator.validate(variables), is(variables));
    }

    @Test
    public void environmentWithAllValidVariablesSet_withTLS() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .put("SOME_VARIABLE", "SOME_VALUE")
                .build();

        assertThat(RemoteEnvironmentValidator.validate(variables), is(variables));
    }
}