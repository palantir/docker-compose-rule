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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.MultiServiceHealthCheck;
import com.palantir.docker.compose.connection.waiting.SingleServiceHealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.logging.LogCollector;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerComposeRuleTest {

    private static final String IP = "127.0.0.1";

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerCompose);
    private DockerComposeFiles mockFiles = mock(DockerComposeFiles.class);
    private DockerMachine machine = mock(DockerMachine.class);
    private LogCollector logCollector = mock(LogCollector.class);
    private DockerComposeRule rule;

    @Before public void
    setup() {
        when(machine.getIp()).thenReturn(IP);
        rule = defaultBuilder().build();
    }

    @Test
    public void docker_compose_build_and_up_is_called_before_tests_are_run() throws IOException, InterruptedException {
        rule.before();
        verify(dockerCompose).build();
        verify(dockerCompose).up();
    }

    @Test
    public void docker_compose_kill_and_rm_are_called_after_tests_are_run() throws IOException, InterruptedException {
        rule.after();
        verify(dockerCompose).kill();
        verify(dockerCompose).rm();
    }

    @Test
    public void docker_compose_wait_for_service_passes_when_check_is_true() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        SingleServiceHealthCheck checkCalledOnce = (container) -> SuccessOrFailure.fromBoolean(timesCheckCalled.incrementAndGet() == 1, "not called once yet");
        DockerComposeRule.builder().from(rule).waitingForService("db", checkCalledOnce).build().before();
        assertThat(timesCheckCalled.get(), is(1));
    }

    @Test
    public void docker_compose_wait_for_service_waits_multiple_services() throws IOException, InterruptedException {
        Container db1 = withComposeExecutableReturningContainerFor("db1");
        Container db2 = withComposeExecutableReturningContainerFor("db2");
        List<Container> containers = ImmutableList.of(db1, db2);

        MultiServiceHealthCheck healthCheck = mock(MultiServiceHealthCheck.class);
        when(healthCheck.areServicesUp(containers)).thenReturn(SuccessOrFailure.success());


        DockerComposeRule.builder().from(rule).waitingForServices(ImmutableList.of("db1", "db2"), healthCheck).build().before();

        verify(healthCheck).areServicesUp(containers);
    }

    @Test
    public void docker_compose_wait_for_service_passes_when_check_is_true_after_being_false() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        SingleServiceHealthCheck checkCalledTwice = (container) -> SuccessOrFailure.fromBoolean(timesCheckCalled.incrementAndGet() == 2, "not called twice yet");
        DockerComposeRule.builder().from(rule).waitingForService("db", checkCalledTwice).build().before();
        assertThat(timesCheckCalled.get(), is(2));
    }

    @Test
    public void throws_if_a_wait_for_service_check_remains_false_till_the_timeout() throws IOException, InterruptedException {
        withComposeExecutableReturningContainerFor("db");

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container '[db]' failed to pass startup check:\noops");

        DockerComposeRule.builder().from(rule).waitingForService("db", (container) -> SuccessOrFailure.failure("oops"), millis(200)).build().before();
    }

    @Test
    public void port_for_container_can_be_retrieved_by_external_mapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");

        DockerPort actualPort = rule.containers().container("db").portMappedExternallyTo(5433);

        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void port_for_container_can_be_retrieved_by_internal_mapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");

        DockerPort actualPort = rule.containers().container("db").portMappedInternallyTo(5432);

        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void when_two_external_ports_on_a_container_are_requested_docker_compose_ps_is_only_executed_once() throws Exception {
        env.ports("db", IP, 5432, 8080);
        withComposeExecutableReturningContainerFor("db");

        rule.containers().container("db").portMappedInternallyTo(5432);
        rule.containers().container("db").portMappedInternallyTo(8080);

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

        DockerComposeRule.builder().from(rule).waitingForService(nonExistentContainer, toHaveAllPortsOpen()).build().before();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logs_can_be_saved_to_a_directory_while_containers_are_running() throws IOException, InterruptedException {
        File logLocation = logFolder.newFolder();
        DockerComposeRule loggingComposition = DockerComposeRule.builder().from(rule).saveLogsTo(logLocation.getAbsolutePath()).build();
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

    @Test
    public void when_skipShutdown_is_true_shutdown_does_not_happen() throws InterruptedException {
        defaultBuilder().skipShutdown(true)
                        .build()
                        .after();
        verifyNoMoreInteractions(dockerCompose);
        verify(logCollector, times(1)).stopCollecting();
    }

    public Container withComposeExecutableReturningContainerFor(String containerName) {
        final Container container = new Container(containerName, dockerCompose);
        when(dockerCompose.container(containerName)).thenReturn(container);
        return container;
    }

    private ImmutableDockerComposeRule.Builder defaultBuilder() {
        return DockerComposeRule.builder().dockerCompose(dockerCompose)
                                          .files(mockFiles)
                                          .machine(machine)
                                          .logCollector(logCollector);
    }
}
