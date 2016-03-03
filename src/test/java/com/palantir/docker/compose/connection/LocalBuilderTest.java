/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * THIS SOFTWARE CONTAINS PROPRIETARY AND CONFIDENTIAL INFORMATION OWNED BY PALANTIR TECHNOLOGIES INC.
 * UNAUTHORIZED DISCLOSURE TO ANY THIRD PARTY IS STRICTLY PROHIBITED
 *
 * For good and valuable consideration, the receipt and adequacy of which is acknowledged by Palantir and recipient
 * of this file ("Recipient"), the parties agree as follows:
 *
 * This file is being provided subject to the non-disclosure terms by and between Palantir and the Recipient.
 *
 * Palantir solely shall own and hereby retains all rights, title and interest in and to this software (including,
 * without limitation, all patent, copyright, trademark, trade secret and other intellectual property rights) and
 * all copies, modifications and derivative works thereof.  Recipient shall and hereby does irrevocably transfer and
 * assign to Palantir all right, title and interest it may have in the foregoing to Palantir and Palantir hereby
 * accepts such transfer. In using this software, Recipient acknowledges that no ownership rights are being conveyed
 * to Recipient.  This software shall only be used in conjunction with properly licensed Palantir products or
 * services.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.palantir.docker.compose.connection;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.connection.DockerMachine.LocalBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.DaemonHostIpResolver.LOCALHOST;
import static com.palantir.docker.compose.configuration.DockerType.DAEMON;
import static com.palantir.docker.compose.configuration.DockerType.REMOTE;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;
import static com.palantir.docker.compose.matchers.DockerMachineEnvironmentMatcher.containsEnvironment;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
    public void local_builder_has_missing_docker_host_remote() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_HOST);
        new LocalBuilder(REMOTE, newHashMap()).build();
    }

    @Test
    public void local_builder_builds_without_tls_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                                                          .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                          .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine, containsEnvironment(dockerVariables));
    }

    @Test
    public void local_builder_has_missing_cert_path_remote() throws Exception {
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
    public void local_builder_builds_with_tls_remote() throws Exception {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine, containsEnvironment(dockerVariables));
    }
}