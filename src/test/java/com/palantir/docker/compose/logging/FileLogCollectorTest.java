package com.palantir.docker.compose.logging;

import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.emptyArray;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class FileLogCollectorTest {

    @Rule
    public TemporaryFolder logDirectoryParent = new TemporaryFolder();
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerComposeExecutable compose = mock(DockerComposeExecutable.class);
    private File logDirectory;
    private LogCollector logCollector;

    @Before
    public void setup() throws IOException {
        logDirectory = logDirectoryParent.newFolder();
        logCollector = new FileLogCollector(logDirectory);
    }

    @Test
    public void whenNoContainersAreRunningNoLogsAreCollected() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames(emptyList()));
        logCollector.startCollecting(compose);
        logCollector.stopCollecting();
        assertThat(logDirectory.list(), is(emptyArray()));
    }

    @Test
    public void whenOneContainerIsRunningAndTerminatesBeforeStartCollectingIsRunLogsAreCollected() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames("db"));
        when(compose.writeLogs(eq("db"), any(OutputStream.class))).thenAnswer((args) -> {
            OutputStream outputStream = (OutputStream) args.getArguments()[1];
            IOUtils.write("log", outputStream);
            return false;
        });
        logCollector.startCollecting(compose);
        logCollector.stopCollecting();
        assertThat(logDirectory.listFiles(), arrayContaining(fileWithName("db.log")));
        assertThat(new File(logDirectory, "db.log"), is(fileContainingString("log")));
    }

    @Test
    public void whenOneContainerIsRunningAndDoesNotTerminateUntilAfterStartCollectingIsRunLogsAreCollected() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames("db"));
        CountDownLatch latch = new CountDownLatch(1);
        when(compose.writeLogs(eq("db"), any(OutputStream.class))).thenAnswer((args) -> {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new RuntimeException("Latch was not triggered");
            }
            OutputStream outputStream = (OutputStream) args.getArguments()[1];
            IOUtils.write("log", outputStream);
            return false;
        });
        logCollector.startCollecting(compose);
        latch.countDown();
        logCollector.stopCollecting();
        assertThat(logDirectory.listFiles(), arrayContaining(fileWithName("db.log")));
        assertThat(new File(logDirectory, "db.log"), is(fileContainingString("log")));
    }

    @Test
    public void whenOneContainerIsRunningAndDoesNotTerminateTheLogsAreStillCollected() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames("db"));
        CountDownLatch latch = new CountDownLatch(1);
        when(compose.writeLogs(eq("db"), any(OutputStream.class))).thenAnswer((args) -> {
            OutputStream outputStream = (OutputStream) args.getArguments()[1];
            IOUtils.write("log", outputStream);
            try {
                latch.await(1, TimeUnit.SECONDS);
                fail("Latch was not triggered");
            } catch (InterruptedException e) {
                // Success
                return true;
            }
            fail("Latch was not triggered");
            return false;
        });
        logCollector.startCollecting(compose);
        logCollector.stopCollecting();
        assertThat(logDirectory.listFiles(), arrayContaining(fileWithName("db.log")));
        assertThat(new File(logDirectory, "db.log"), is(fileContainingString("log")));
        latch.countDown();
    }

    @Test
    public void twoContainersHaveLogsCollectedInParallel() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames(asList("db", "db2")));
        CountDownLatch dbLatch = new CountDownLatch(1);
        when(compose.writeLogs(eq("db"), any(OutputStream.class))).thenAnswer((args) -> {
            OutputStream outputStream = (OutputStream) args.getArguments()[1];
            IOUtils.write("log", outputStream);
            dbLatch.countDown();
            return true;
        });
        CountDownLatch db2Latch = new CountDownLatch(1);
        when(compose.writeLogs(eq("db2"), any(OutputStream.class))).thenAnswer((args) -> {
            OutputStream outputStream = (OutputStream) args.getArguments()[1];
            IOUtils.write("other", outputStream);
            db2Latch.countDown();
            return true;
        });

        logCollector.startCollecting(compose);
        assertThat(dbLatch.await(1, TimeUnit.SECONDS), is(true));
        assertThat(db2Latch.await(1, TimeUnit.SECONDS), is(true));

        assertThat(logDirectory.listFiles(), arrayContainingInAnyOrder(fileWithName("db.log"), fileWithName("db2.log")));
        assertThat(new File(logDirectory, "db.log"), is(fileContainingString("log")));
        assertThat(new File(logDirectory, "db2.log"), is(fileContainingString("other")));

        logCollector.stopCollecting();
    }

    @Test
    public void aStartedCollectorCannotBeStartedASecondTime() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames("db"));
        logCollector.startCollecting(compose);
        exception.expect(RuntimeException.class);
        exception.expectMessage("Cannot start collecting the same logs twice");
        logCollector.startCollecting(compose);
    }

}
