package com.palantir.docker.compose;

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

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.palantir.docker.compose.DockerComposition.DockerCompositionBuilder;
import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.execution.DockerComposeExecutable;

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
    public void dockerComposeBuildAndUpIsCalledBeforeTestsAreRun() throws IOException, InterruptedException {
        dockerComposition.build().before();
        verify(dockerComposeExecutable).build();
        verify(dockerComposeExecutable).up();
    }

    @Test
    public void dockerComposeKillAndRmAreCalledAfterTestsAreRun() throws IOException, InterruptedException {
        dockerComposition.build().after();
        verify(dockerComposeExecutable).kill();
        verify(dockerComposeExecutable).rm();
    }

    @Test
    public void dockerComposeWaitForServiceWithSinglePortWaitsForPortToBeAvailableBeforeTestsAreRun() throws IOException, InterruptedException {
        DockerPort port = env.availableService("db", IP, 5432, 5432);
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db").build().before();
        verify(port, atLeastOnce()).isListeningNow();
    }

    @Test
    public void dockerComposeWaitForHttpServiceWaitsForAddressToBeAvailableBeforeTestsAreRun() throws IOException, InterruptedException {
        DockerPort httpPort = env.availableHttpService("http", IP, 8080, 8080);
        Function<DockerPort, String> urlFunction = (port) -> "url";
        withComposeExecutableReturningContainerFor("http");
        dockerComposition.waitingForHttpService("http", 8080, urlFunction).build().before();
        verify(httpPort, atLeastOnce()).isListeningNow();
        verify(httpPort, atLeastOnce()).isHttpResponding(urlFunction);
    }

    @Test
    public void dockerComposeWaitForServicePassesWhenCheckIsTrue() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db", (container) -> timesCheckCalled.incrementAndGet() == 1).build().before();
        assertThat(timesCheckCalled.get(), is(1));
    }

    @Test
    public void dockerComposeWaitForServicePassesWhenCheckIsTrueAfterBeingFalse() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db", (container) -> timesCheckCalled.incrementAndGet() == 2).build().before();
        assertThat(timesCheckCalled.get(), is(2));
    }

    @Test
    public void dockerComposeWaitForServiceWithThrowsWhenCheckIsFalse() throws IOException, InterruptedException {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'db' failed to pass startup check");
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db", (container) -> false).build().before();
    }

    @Test
    public void dockerComposeWaitForServiceThrowsAnExceptionWhenThePortIsUnavailable() throws IOException, InterruptedException {
        env.unavailableService("db", IP, 5432, 5432);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'db' failed to pass startup check");
        withComposeExecutableReturningContainerFor("db");
        dockerComposition.waitingForService("db").build().before();
    }

    @Test
    public void dockerComposeWaitForTwoServicesWithSinglePortWaitsForPortToBeAvailableBeforeTestsAreRun() throws IOException, InterruptedException {
        DockerPort firstDbPort = env.availableService("db", IP, 5432, 5432);
        DockerPort secondDbPort = env.availableService("otherDb", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");
        withComposeExecutableReturningContainerFor("otherDb");
        dockerComposition.waitingForService("db").waitingForService("otherDb").build().before();
        verify(firstDbPort, atLeastOnce()).isListeningNow();
        verify(secondDbPort, atLeastOnce()).isListeningNow();
    }

    @Test
    public void portForContainerCanBeRetrievedByExternalMapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");
        DockerPort actualPort = dockerComposition.build().portOnContainerWithExternalMapping("db", 5433);
        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void portForContainerCanBeRetrievedByInternalMapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", IP, 5433, 5432);
        withComposeExecutableReturningContainerFor("db");
        DockerPort actualPort = dockerComposition.build().portOnContainerWithInternalMapping("db", 5432);
        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void whenTwoExternalPortsOnAContainerAreRequestedDockerComposePsIsOnlyExecutedOnce() throws IOException, InterruptedException {
        env.ports("db", IP, 5432, 8080);
        withComposeExecutableReturningContainerFor("db");
        DockerComposition composition = dockerComposition.build();
        composition.portOnContainerWithInternalMapping("db", 5432);
        composition.portOnContainerWithInternalMapping("db", 8080);
        verify(dockerComposeExecutable, times(1)).ports("db");
    }

    @Test
    public void waitingForServiceThatDoesNotExistResultsInAnIllegalStateException() throws IOException, InterruptedException {
        when(dockerComposeExecutable.ports("nonExistent"))
            .thenThrow(new IllegalStateException("No container with name 'nonExistent' found"));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'nonExistent' failed to pass startup check");
        withComposeExecutableReturningContainerFor("nonExistent");
        dockerComposition.waitingForService("nonExistent").build().before();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logsCanBeSavedToADirectoryWhileContainersAreRunning() throws IOException, InterruptedException {
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
