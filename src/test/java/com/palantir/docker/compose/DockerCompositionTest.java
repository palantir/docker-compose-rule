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
package com.palantir.docker.compose;

import com.palantir.docker.compose.DockerComposition.DockerCompositionBuilder;
import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
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
import java.util.function.Function;

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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

    private final DockerComposeExecutable dockerComposeExecutable = mock(DockerComposeExecutable.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerComposeExecutable);
    private final DockerCompositionBuilder dockerComposition = DockerComposition.of(dockerComposeExecutable)
                                                                                .serviceTimeout(Duration.millis(200));

    @Test
    public void docker_compose_build_and_up_is_called_before_tests_are_run() throws IOException, InterruptedException {
        dockerComposition.build().before();
        verify(dockerComposeExecutable).build();
        verify(dockerComposeExecutable).up();
    }

    @Test
    public void docker_compose_kill_and_rm_are_called_after_tests_are_run() throws IOException, InterruptedException {
        dockerComposition.build().after();
        verify(dockerComposeExecutable).kill();
        verify(dockerComposeExecutable).rm();
    }

    @Test
    public void docker_compose_wait_for_service_with_single_port_waits_for_port_to_be_available_before_tests_are_run() throws IOException, InterruptedException {
        DockerPort port = env.availableService("db", IP, 5432, 5432);
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db").build().before();
        verify(port, atLeastOnce()).isListeningNow();
    }

    @Test
    public void docker_compose_wait_for_http_service_waits_for_address_to_be_available_before_tests_are_run() throws IOException, InterruptedException {
        DockerPort httpPort = env.availableHttpService("http", IP, 8080, 8080);
        Function<DockerPort, String> urlFunction = (port) -> "url";
        withComposeExecutableReturningContainerFor("http");
        dockerComposition.waitingForHttpService("http", 8080, urlFunction).build().before();
        verify(httpPort, atLeastOnce()).isListeningNow();
        verify(httpPort, atLeastOnce()).isHttpResponding(urlFunction);
    }

    @Test
    public void docker_compose_wait_for_service_passes_when_check_is_true() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db", (container) -> timesCheckCalled.incrementAndGet() == 1).build().before();
        assertThat(timesCheckCalled.get(), is(1));
    }

    @Test
    public void docker_compose_wait_for_service_passes_when_check_is_true_after_being_false() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db", (container) -> timesCheckCalled.incrementAndGet() == 2).build().before();
        assertThat(timesCheckCalled.get(), is(2));
    }

    @Test
    public void docker_compose_wait_for_service_with_throws_when_check_is_false() throws IOException, InterruptedException {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'db' failed to pass startup check");
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db", (container) -> false).build().before();
    }

    @Test
    public void docker_compose_wait_for_service_throws_an_exception_when_the_port_is_unavailable() throws IOException, InterruptedException {
        env.unavailableService("db", IP, 5432, 5432);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'db' failed to pass startup check");
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db").build().before();
    }

    @Test
    public void docker_compose_wait_for_two_services_with_single_port_waits_for_port_to_be_available_before_tests_are_run() throws IOException, InterruptedException {
        DockerPort firstDbPort = env.availableService("db", IP, 5432, 5432);
        DockerPort secondDbPort = env.availableService("otherDb", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");
        withComposeExecutableReturningContainerFor("otherDb");
        dockerComposition.waitingForService("db").waitingForService("otherDb").build().before();
        verify(firstDbPort, atLeastOnce()).isListeningNow();
        verify(secondDbPort, atLeastOnce()).isListeningNow();
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
        verify(dockerComposeExecutable, times(1)).ports("db");
    }

    @Test
    public void waiting_for_service_that_does_not_exist_results_in_an_illegal_state_exception() throws IOException, InterruptedException {
        when(dockerComposeExecutable.ports("nonExistent"))
            .thenThrow(new IllegalStateException("No container with name 'nonExistent' found"));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'nonExistent' failed to pass startup check");
        withComposeExecutableReturningContainerFor("nonExistent");
        dockerComposition.waitingForService("nonExistent").build().before();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logs_can_be_saved_to_a_directory_while_containers_are_running() throws IOException, InterruptedException {
        File logLocation = logFolder.newFolder();
        DockerComposition loggingComposition = dockerComposition.saveLogsTo(logLocation.getAbsolutePath()).build();
        when(dockerComposeExecutable.ps()).thenReturn(new ContainerNames("db"));
        CountDownLatch latch = new CountDownLatch(1);
        when(dockerComposeExecutable.writeLogs(eq("db"), any(OutputStream.class))).thenAnswer((args) -> {
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
        when(dockerComposeExecutable.container(containerName)).thenReturn(new Container(containerName, dockerComposeExecutable));
    }

}
