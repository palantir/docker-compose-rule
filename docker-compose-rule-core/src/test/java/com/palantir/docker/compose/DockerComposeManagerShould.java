/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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
import static com.palantir.docker.compose.matchers.IoMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IoMatchers.fileWithName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.joda.time.Duration.millis;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.State;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerExecutionException;
import com.palantir.docker.compose.logging.LogCollector;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.HamcrestCondition;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class DockerComposeManagerShould {

    private static final String IP = "127.0.0.1";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    @Mock
    private HealthCheck<List<Container>> healthCheck;

    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final Docker mockDocker = mock(Docker.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerCompose);
    private DockerComposeFiles mockFiles = mock(DockerComposeFiles.class);
    private DockerMachine machine = mock(DockerMachine.class);
    private LogCollector logCollector = mock(LogCollector.class);
    private DockerComposeManager dockerComposeManager;

    @Before
    public void before() {
        when(machine.getIp()).thenReturn(IP);
        dockerComposeManager = defaultBuilder().build();
    }

    private DockerComposeManager.Builder defaultBuilder() {
        return new DockerComposeManager.Builder()
                .dockerCompose(dockerCompose)
                .files(mockFiles)
                .machine(machine)
                .logCollector(logCollector);
    }

    @Test
    public void call_build_and_up_before_tests_are_run() throws IOException, InterruptedException {
        dockerComposeManager.before();
        verify(dockerCompose).build();
        verify(dockerCompose).up();
    }

    @Test
    public void calls_shutdownStrategy_in_after_method() throws IOException, InterruptedException {
        ShutdownStrategy shutdownStrategy = mock(ShutdownStrategy.class);
        dockerComposeManager = new DockerComposeManager.Builder()
                .docker(mockDocker)
                .dockerCompose(dockerCompose)
                .files(mockFiles)
                .shutdownStrategy(shutdownStrategy)
                .build();

        dockerComposeManager.after();
        InOrder inOrder = Mockito.inOrder(shutdownStrategy);
        inOrder.verify(shutdownStrategy).stop(dockerCompose);
        inOrder.verify(shutdownStrategy).shutdown(dockerCompose, mockDocker);
    }

    @Test
    public void calls_pull_build_and_up_when_tests_are_run_and_pullOnStartup_is_true()
            throws InterruptedException, IOException {
        defaultBuilder().pullOnStartup(true).build().before();
        verify(dockerCompose).pull();
        verify(dockerCompose).build();
        verify(dockerCompose).up();
    }

    @Test
    public void pass_wait_for_service_when_check_is_true() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        HealthCheck<Container> checkCalledOnce = _container ->
                SuccessOrFailure.fromBoolean(timesCheckCalled.incrementAndGet() == 1, "not called once yet");
        defaultBuilder().waitingForService("db", checkCalledOnce).build().before();
        assertThat(timesCheckCalled.get()).isEqualTo(1);
    }

    @Test
    public void wait_for_multiple_services_on_wait() throws IOException, InterruptedException {
        Container db1 = withComposeExecutableReturningContainerFor("db1");
        Container db2 = withComposeExecutableReturningContainerFor("db2");
        List<Container> containers = ImmutableList.of(db1, db2);

        when(healthCheck.isHealthy(containers)).thenReturn(SuccessOrFailure.success());

        defaultBuilder()
                .waitingForServices(ImmutableList.of("db1", "db2"), healthCheck)
                .build()
                .before();

        verify(healthCheck).isHealthy(containers);
    }

    @Test
    public void pass_wait_for_service_when_check_is_true_after_being_false() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        HealthCheck<Container> checkCalledTwice = _container ->
                SuccessOrFailure.fromBoolean(timesCheckCalled.incrementAndGet() == 2, "not called twice yet");
        defaultBuilder().waitingForService("db", checkCalledTwice).build().before();
        assertThat(timesCheckCalled.get()).isEqualTo(2);
    }

    @Test
    public void throw_exception_if_a_wait_for_service_check_remains_false_until_the_timeout()
            throws IOException, InterruptedException {
        withComposeExecutableReturningContainerFor("db");

        exception.expect(IllegalStateException.class);
        exception.expectMessage("failed to pass a startup check");
        exception.expectMessage("oops");

        defaultBuilder()
                .waitingForService("db", _container -> SuccessOrFailure.failure("oops"), millis(200))
                .build()
                .before();
    }

    @Test
    public void retrieve_port_for_container_by_external_mapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");

        DockerPort actualPort =
                dockerComposeManager.containers().container("db").portMappedExternallyTo(5433);

        assertThat(actualPort).isEqualTo(expectedPort);
    }

    @Test
    public void retrieve_port_for_container_by_internal_mapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");

        @SuppressWarnings("deprecation") // intentionally using the deprecated method temporarily
        DockerPort actualPort =
                dockerComposeManager.containers().container("db").portMappedInternallyTo(5432);

        assertThat(actualPort).isEqualTo(expectedPort);
    }

    @Test
    public void execute_ps_once_when_two_external_ports_on_a_container_are_requested() throws Exception {
        env.ports("db", IP, 5432, 8080);
        withComposeExecutableReturningContainerFor("db");

        dockerComposeManager.containers().container("db").port(5432);
        dockerComposeManager.containers().container("db").port(8080);

        verify(dockerCompose, times(1)).ports("db");
    }

    @Test
    public void throw_illegal_state_exception_when_waiting_for_service_that_does_not_exist()
            throws IOException, InterruptedException {
        String nonExistentContainer = "nonExistent";
        when(dockerCompose.ports(nonExistentContainer))
                .thenThrow(new IllegalStateException("No container with name 'nonExistent' found"));
        withComposeExecutableReturningContainerFor(nonExistentContainer);

        exception.expect(IllegalStateException.class);
        exception.expectMessage(nonExistentContainer);

        defaultBuilder()
                .waitingForService(nonExistentContainer, toHaveAllPortsOpen(), Duration.millis(100))
                .build()
                .before();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void be_able_to_save_logs_to_a_directory_while_containers_are_running()
            throws IOException, InterruptedException {
        File logLocation = logFolder.newFolder();
        DockerComposeManager loggingComposition = new DockerComposeManager.Builder()
                .docker(mockDocker)
                .dockerCompose(dockerCompose)
                .files(mockFiles)
                .machine(machine)
                .saveLogsTo(logLocation.getAbsolutePath())
                .build();
        when(dockerCompose.services()).thenReturn(ImmutableList.of("db"));
        when(dockerCompose.id(any())).thenReturn(Optional.of("abcde"));
        when(mockDocker.state("abcde")).thenReturn(State.HEALTHY);
        CountDownLatch latch = new CountDownLatch(1);
        when(dockerCompose.writeLogs(eq("db"), any(OutputStream.class))).thenAnswer(args -> {
            OutputStream outputStream = (OutputStream) args.getArguments()[1];
            IOUtils.write("db log", outputStream);
            latch.countDown();
            return null;
        });
        loggingComposition.before();
        loggingComposition.after();
        assertThat(latch.await(1, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(logLocation.listFiles()).is(new HamcrestCondition<>(arrayContaining(fileWithName("db.log"))));
        assertThat(new File(logLocation, "db.log")).is(new HamcrestCondition<>(is(fileContainingString("db log"))));
    }

    @Test
    public void not_shut_down_when_skipShutdown_is_true() throws IOException, InterruptedException {
        new DockerComposeManager.Builder()
                .dockerCompose(dockerCompose)
                .files(mockFiles)
                .machine(machine)
                .logCollector(logCollector)
                .skipShutdown(true)
                .build()
                .after();
        verifyNoMoreInteractions(dockerCompose);
        verify(logCollector, times(1)).collectLogs(dockerCompose);
    }

    @Test
    public void before_fails_when_docker_up_throws_exception() throws IOException, InterruptedException {
        doThrow(new DockerExecutionException("")).when(dockerCompose).up();
        dockerComposeManager = defaultBuilder().build();
        exception.expect(DockerExecutionException.class);
        dockerComposeManager.before();
    }

    @Test
    public void before_retries_when_docker_up_reports_conflicting_containers()
            throws IOException, InterruptedException {
        String conflictingContainer = "conflictingContainer";
        doThrow(new DockerExecutionException("The name \"" + conflictingContainer + "\" is already in use"))
                .doNothing()
                .when(dockerCompose)
                .up();
        dockerComposeManager = defaultBuilder().docker(mockDocker).build();
        dockerComposeManager.before();

        verify(dockerCompose, times(2)).up();
        verify(mockDocker).rm(ImmutableSet.of(conflictingContainer));
    }

    @Test
    public void when_remove_conflicting_containers_on_startup_is_set_to_false_before_does_not_retry_on_conflicts()
            throws IOException, InterruptedException {
        String conflictingContainer = "conflictingContainer";
        doThrow(new DockerExecutionException("The name \"" + conflictingContainer + "\" is already in use"))
                .when(dockerCompose)
                .up();
        dockerComposeManager = defaultBuilder()
                .docker(mockDocker)
                .removeConflictingContainersOnStartup(false)
                .build();

        exception.expect(DockerExecutionException.class);
        exception.expectMessage("The name \"conflictingContainer\" is already in use");
        dockerComposeManager.before();
    }

    @Test
    public void union_cluster_waits_from_builder_instead_of_overwriting() {
        ClusterWait firstWait = mock(ClusterWait.class);
        ClusterWait secondWait = mock(ClusterWait.class);

        DockerComposeManager twoAssignments = defaultBuilder()
                .clusterWaits(ImmutableList.of(firstWait))
                .clusterWaits(ImmutableList.of(secondWait))
                .build();

        assertThat(twoAssignments.clusterWaits()).containsExactly(firstWait, secondWait);
    }

    @Test
    public void disallow_overwriting_in_builder() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Builder of DockerComposeManager is strict, attribute is already set: docker");

        new DockerComposeManager.Builder()
                .docker(mock(Docker.class))
                .docker(mock(Docker.class))
                .build();
    }

    public Container withComposeExecutableReturningContainerFor(String containerName) {
        return new Container(containerName, mockDocker, dockerCompose);
    }
}
