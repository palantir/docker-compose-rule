/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose;

import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.execution.DockerCompose;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;
import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.core.Is.is;
import static org.joda.time.Duration.millis;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerCompositionTest {

    private static final String IP = "127.0.0.1";

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerCompose);
    private final DockerCompositionBuilder dockerComposition = DockerComposition.of(dockerCompose);

    @Test
    public void docker_compose_build_and_up_is_called_before_tests_are_run() throws IOException, InterruptedException {
        dockerComposition.build().before();
        verify(dockerCompose).build();
        verify(dockerCompose).up();
    }

    @Test
    public void docker_compose_kill_and_rm_are_called_after_tests_are_run() throws IOException, InterruptedException {
        dockerComposition.build().after();
        verify(dockerCompose).kill();
        verify(dockerCompose).rm();
    }

    @Test
    public void docker_compose_wait_for_service_passes_when_check_is_true() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        HealthCheck checkCalledOnce = (container) -> SuccessOrFailure.fromBoolean(timesCheckCalled.incrementAndGet() == 1, "not called once yet");
        dockerComposition.waitingForService("db", checkCalledOnce).build().before();
        assertThat(timesCheckCalled.get(), is(1));
    }

    @Test
    public void docker_compose_wait_for_service_passes_when_check_is_true_after_being_false() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        HealthCheck checkCalledTwice = (container) -> SuccessOrFailure.fromBoolean(timesCheckCalled.incrementAndGet() == 2, "not called twice yet");
        dockerComposition.waitingForService("db", checkCalledTwice).build().before();
        assertThat(timesCheckCalled.get(), is(2));
    }

    @Test
    public void throws_if_a_wait_for_service_check_remains_false_till_the_timeout() throws IOException, InterruptedException {
        withComposeExecutableReturningContainerFor("db");

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'db' failed to pass startup check:\noops");

        dockerComposition.waitingForService("db", (container) -> SuccessOrFailure.failure("oops"), millis(200)).build().before();
    }

    @Test
    public void port_for_container_can_be_retrieved_by_external_mapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");

        DockerPort actualPort = dockerComposition.build().portOnContainerWithExternalMapping("db", 5433);

        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void port_for_container_can_be_retrieved_by_internal_mapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");

        DockerPort actualPort = dockerComposition.build().portOnContainerWithInternalMapping("db", 5432);

        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void when_two_external_ports_on_a_container_are_requested_docker_compose_ps_is_only_executed_once() throws IOException, InterruptedException {
        env.ports("db", IP, 5432, 8080);
        withComposeExecutableReturningContainerFor("db");
        DockerComposition composition = dockerComposition.build();

        composition.portOnContainerWithInternalMapping("db", 5432);
        composition.portOnContainerWithInternalMapping("db", 8080);

        verify(dockerCompose, times(1)).ports("db");
    }

    @Test
    public void waiting_for_service_that_does_not_exist_results_in_an_illegal_state_exception() throws IOException, InterruptedException {
        String nonExistentContainer = "nonExistent";
        when(dockerCompose.ports(nonExistentContainer))
            .thenThrow(new IllegalStateException("No container with name 'nonExistent' found"));
        withComposeExecutableReturningContainerFor(nonExistentContainer);

        exception.expect(IllegalStateException.class);
        exception.expectMessage(nonExistentContainer);

        dockerComposition.waitingForService(nonExistentContainer, toHaveAllPortsOpen()).build().before();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logs_can_be_saved_to_a_directory_while_containers_are_running() throws IOException, InterruptedException {
        File logLocation = logFolder.newFolder();
        DockerComposition loggingComposition = dockerComposition.saveLogsTo(logLocation.getAbsolutePath()).build();
        when(dockerCompose.ps()).thenReturn(new ContainerNames("db"));
        CountDownLatch latch = new CountDownLatch(1);
        when(dockerCompose.writeLogs(eq("db"), any(OutputStream.class))).thenAnswer((args) -> {
            OutputStream outputStream = (OutputStream) args.getArguments()[1];
            IOUtils.write("db log", outputStream);
            latch.countDown();
            return true;
        });
        loggingComposition.before();
        assertThat(latch.await(1, TimeUnit.SECONDS), is(true));
        loggingComposition.after();
        assertThat(logLocation.listFiles(), arrayContaining(fileWithName("db.log")));
        assertThat(new File(logLocation, "db.log"), is(fileContainingString("db log")));
    }

    public void withComposeExecutableReturningContainerFor(String containerName) {
        when(dockerCompose.container(containerName)).thenReturn(new Container(containerName, dockerCompose));
    }

}
