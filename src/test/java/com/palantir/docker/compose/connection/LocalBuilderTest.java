package com.palantir.docker.compose.connection;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.DockerType.DAEMON;
import static com.palantir.docker.compose.configuration.DockerType.REMOTE;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;
import static com.palantir.docker.compose.configuration.HostIpResolver.LOCALHOST;
import static com.palantir.docker.compose.matchers.DockerMachineEnvironmentMatcher.containsEnvironment;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.connection.DockerMachine.LocalBuilder;

public class LocalBuilderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void localBuilderWithAdditionalEnvironmentSetTwiceOverridesPreviousEnvironment_daemon() throws Exception {
        Map<String, String> environment1 = ImmutableMap.of("ENV_1", "VAL_1");
        Map<String, String> environment2 = ImmutableMap.of("ENV_2", "VAL_2");
        DockerMachine localMachine = new LocalBuilder(DAEMON, newHashMap()).withEnvironment(environment1)
                                                                           .withEnvironment(environment2)
                                                                           .build();
        assertThat(localMachine, not(containsEnvironment(environment1)));
        assertThat(localMachine, containsEnvironment(environment2));
    }

    @Test
    public void localBuilderWithAdditionalEnvironmentSetAndIndividualEnvironmentIsUnionOfTheTwo_daemon() throws Exception {
        Map<String, String> environment = ImmutableMap.<String, String>builder()
                                                       .put("ENV_1", "VAL_1")
                                                       .put("ENV_2", "VAL_2")
                                                       .build();
        DockerMachine localMachine = new LocalBuilder(DAEMON, newHashMap()).withEnvironment(environment)
                                                                           .withAdditionalEnvironmentVariable("ENV_3", "VAL_3")
                                                                           .build();
        assertThat(localMachine, containsEnvironment(environment));
        assertThat(localMachine, containsEnvironment(ImmutableMap.of("ENV_3", "VAL_3")));
    }

    @Test
    public void localBuilderWithAdditionalEnvironmentSetTwiceOverridesPreviousEnvironment_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .build();
        Map<String, String> environment1 = ImmutableMap.of("ENV_1", "VAL_1");
        Map<String, String> environment2 = ImmutableMap.of("ENV_2", "VAL_2");
        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).withEnvironment(environment1)
                                                                           .withEnvironment(environment2)
                                                                           .build();
        assertThat(localMachine, not(containsEnvironment(environment1)));
        assertThat(localMachine, containsEnvironment(environment2));
    }

    @Test
    public void localBuilderWithAdditionalEnvironmentSetAndIndividualEnvironmentIsUnionOfTheTwo_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .build();
        Map<String, String> environment = ImmutableMap.<String, String>builder()
                .put("ENV_1", "VAL_1")
                .put("ENV_2", "VAL_2")
                .build();
        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).withEnvironment(environment)
                                                                              .withAdditionalEnvironmentVariable("ENV_3", "VAL_3")
                                                                              .build();
        assertThat(localMachine, containsEnvironment(environment));
        assertThat(localMachine, containsEnvironment(ImmutableMap.of("ENV_3", "VAL_3")));
    }

    @Test
    public void localBuilderWithAdditionalEnvironmentGetsVariableOverriden() throws Exception {
        Map<String, String> environment = ImmutableMap.<String, String>builder()
                .put("ENV_1", "VAL_1")
                .put("ENV_2", "VAL_2")
                .build();
        DockerMachine localMachine = new LocalBuilder(DAEMON, newHashMap()).withEnvironment(environment)
                                                                           .withAdditionalEnvironmentVariable("ENV_2", "DIFFERENT_VALUE")
                                                                           .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put("ENV_1", "VAL_1")
                .put("ENV_2", "DIFFERENT_VALUE")
                .build();
        assertThat(localMachine, not(containsEnvironment(environment)));
        assertThat(localMachine, containsEnvironment(expected));
    }

    @Test
    public void localBuilderHasInvalidVariables_daemon() throws Exception {
        Map<String, String> invalidDockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("These variables were set");
        exception.expectMessage(DOCKER_HOST);
        exception.expectMessage(DOCKER_CERT_PATH);
        exception.expectMessage(DOCKER_TLS_VERIFY);
        exception.expectMessage("They cannot be set when connecting to a local docker daemon");

        new LocalBuilder(DAEMON, invalidDockerVariables).build();
    }

    @Test
    public void localBuilderHasInvalidAdditionalVariables_daemon() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("The following variables");
        exception.expectMessage(DOCKER_HOST);
        exception.expectMessage("cannot exist in your additional environment variable block");

        new LocalBuilder(DAEMON, newHashMap()).withAdditionalEnvironmentVariable(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                              .build();
    }

    @Test
    public void localBuilderHasInvalidAdditionalVariables_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                                                          .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                          .put(DOCKER_TLS_VERIFY, "1")
                                                          .put(DOCKER_CERT_PATH, "/path/to/certs")
                                                          .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("The following variables");
        exception.expectMessage(DOCKER_HOST);
        exception.expectMessage("cannot exist in your additional environment variable block");

        new LocalBuilder(REMOTE, dockerVariables).withAdditionalEnvironmentVariable(DOCKER_HOST, "tcp://192.168.99.101:2376")
                                                 .build();
    }

    @Test
    public void localBuilderReturnsLocalhostAsIp_daemon() throws Exception {
        DockerMachine localMachine = new LocalBuilder(DAEMON, newHashMap()).build();
        assertThat(localMachine.getIp(), is(LOCALHOST));
    }

    @Test
    public void localBuilderReturnsDockerHostAsIp_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine.getIp(), is("192.168.99.100"));
    }

    @Test
    public void localBuilderHasMissingDockerHost_remote() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_HOST);
        new LocalBuilder(REMOTE, newHashMap()).build();
    }

    @Test
    public void localBuilderBuildsWithoutTls_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                                                          .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                          .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine, containsEnvironment(dockerVariables));
    }

    @Test
    public void localBuilderHasMissingCertPath_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_CERT_PATH);
        new LocalBuilder(REMOTE, dockerVariables).build();
    }

    @Test
    public void localBuilderBuildsWithTls_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine, containsEnvironment(dockerVariables));
    }
}