package com.palantir.docker.compose;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.palantir.docker.compose.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.IOMatchers.fileWithName;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Function;

public class DockerCompositionTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private final DockerComposeExecutable dockerComposeExecutable = mock(DockerComposeExecutable.class);
    private final DockerMachine dockerMachine = mock(DockerMachine.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerComposeExecutable, dockerMachine);
    private final DockerComposition dockerComposition = new DockerComposition(dockerComposeExecutable, dockerMachine)
                                                               .serviceTimeout(Duration.millis(200));

    @Test
    public void dockerComposeBuildAndUpIsCalledBeforeTestsAreRun() throws IOException, InterruptedException {
        dockerComposition.before();
        verify(dockerComposeExecutable).build();
        verify(dockerComposeExecutable).up();
    }

    @Test
    public void dockerComposeKillAndRmAreCalledAfterTestsAreRun() throws IOException, InterruptedException {
        dockerComposition.after();
        verify(dockerComposeExecutable).kill();
        verify(dockerComposeExecutable).rm();
    }

    @Test
    public void dockerComposeWaitForServiceWithSinglePortWaitsForPortToBeAvailableBeforeTestsAreRun() throws IOException, InterruptedException {
        DockerPort port = env.availableService("db", 5432, 5432);
        dockerComposition.waitingForService("db").before();
        verify(port, atLeastOnce()).isListeningNow();
    }

    @Test
    public void dockerComposeWaitForHttpServiceWaitsForAddressToBeAvailableBeforeTestsAreRun() throws IOException, InterruptedException {
        DockerPort httpPort = env.availableHttpService("http", 8080, 8080);
        Function<DockerPort, String> urlFunction = (port) -> "url";
        dockerComposition.waitingForHttpService("http", 8080, urlFunction).before();
        verify(httpPort, atLeastOnce()).isListeningNow();
        verify(httpPort, atLeastOnce()).isHttpResponding(urlFunction);
    }

    @Test
    public void dockerComposeWaitForServicePassesWhenCheckIsTrue() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        dockerComposition.waitingForService("db", (container) -> timesCheckCalled.incrementAndGet() == 1).before();
        assertThat(timesCheckCalled.get(), is(1));
    }

    @Test
    public void dockerComposeWaitForServicePassesWhenCheckIsTrueAfterBeingFalse() throws IOException, InterruptedException {
        AtomicInteger timesCheckCalled = new AtomicInteger(0);
        dockerComposition.waitingForService("db", (container) -> timesCheckCalled.incrementAndGet() == 2).before();
        assertThat(timesCheckCalled.get(), is(2));
    }

    @Test
    public void dockerComposeWaitForServiceWithThrowsWhenCheckIsFalse() throws IOException, InterruptedException {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'db' failed to pass startup check");
        dockerComposition.waitingForService("db", (container) -> false).before();
    }

    @Test
    public void dockerComposeWaitForServiceThrowsAnExceptionWhenThePortIsUnavailable() throws IOException, InterruptedException {
        env.unavailableService("db", 5432, 5432);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'db' failed to pass startup check");
        dockerComposition.waitingForService("db").before();
    }

    @Test
    public void dockerComposeWaitForTwoServicesWithSinglePortWaitsForPortToBeAvailableBeforeTestsAreRun() throws IOException, InterruptedException {
        DockerPort firstDbPort = env.availableService("db", 5432, 5432);
        DockerPort secondDbPort = env.availableService("otherDb", 5433, 5432);
        dockerComposition.waitingForService("db").waitingForService("otherDb").before();
        verify(firstDbPort, atLeastOnce()).isListeningNow();
        verify(secondDbPort, atLeastOnce()).isListeningNow();
    }

    @Test
    public void portForContainerCanBeRetrievedByExternalMapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", 5433, 5432);
        DockerPort actualPort = dockerComposition.portOnContainerWithExternalMapping("db", 5433);
        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void portForContainerCanBeRetrievedByInternalMapping() throws IOException, InterruptedException {
        DockerPort expectedPort = env.port("db", 5433, 5432);
        DockerPort actualPort = dockerComposition.portOnContainerWithInternalMapping("db", 5432);
        assertThat(actualPort, is(expectedPort));
    }

    @Test
    public void waitingForServiceThatDoesNotExistResultsInAnIllegalStateException() throws IOException, InterruptedException {
        when(dockerComposeExecutable.ports("nonExistent"))
            .thenThrow(new IllegalStateException("No container with name 'nonExistent' found"));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Container 'nonExistent' failed to pass startup check");
        dockerComposition.waitingForService("nonExistent").before();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logsCanBeSavedToADirectoryWhileContainersAreRunning() throws IOException, InterruptedException {
        File logLocation = logFolder.newFolder();
        DockerComposition loggingComposition = dockerComposition.saveLogsTo(logLocation.getAbsolutePath());
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

}
