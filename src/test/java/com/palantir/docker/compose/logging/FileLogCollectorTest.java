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
package com.palantir.docker.compose.logging;

import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.execution.DockerCompose;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class FileLogCollectorTest {

    @Rule
    public TemporaryFolder logDirectoryParent = new TemporaryFolder();
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerCompose compose = mock(DockerCompose.class);
    private File logDirectory;
    private LogCollector logCollector;

    @Before
    public void setup() throws IOException {
        logDirectory = logDirectoryParent.newFolder();
        logCollector = new FileLogCollector(logDirectory);
    }

    @Test
    public void cannot_be_created_when_trying_to_use_a_file_as_the_log_directory() throws IOException {
        File file = logDirectoryParent.newFile("cannot-use");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("cannot be a file");

        new FileLogCollector(file);
    }

    @Test
    public void creates_the_log_directory_if_it_does_not_already_exist() throws IOException {
        File doesNotExistYetDirectory = logDirectoryParent.getRoot()
                .toPath()
                .resolve("doesNotExist")
                .toFile();
        new FileLogCollector(doesNotExistYetDirectory);
        assertThat(doesNotExistYetDirectory.exists(), is(true));
    }

    @Test
    public void cannot_be_created_if_the_log_directory_does_not_exist_and_cannot_be_created() throws IOException {
        File cannotBeCreatedDirectory = cannotBeCreatedDirectory();

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Error making");
        exception.expectMessage(cannotBeCreatedDirectory.getAbsolutePath());

        new FileLogCollector(cannotBeCreatedDirectory);
    }

    @Test
    public void when_no_containers_are_running_no_logs_are_collected() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames(emptyList()));
        logCollector.startCollecting(compose);
        logCollector.stopCollecting();
        assertThat(logDirectory.list(), is(emptyArray()));
    }

    @Test
    public void when_one_container_is_running_and_terminates_before_start_collecting_is_run_logs_are_collected() throws IOException, InterruptedException {
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
    public void when_one_container_is_running_and_does_not_terminate_until_after_start_collecting_is_run_logs_are_collected() throws IOException, InterruptedException {
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
    public void when_one_container_is_running_and_does_not_terminate_the_logs_are_still_collected() throws IOException, InterruptedException {
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
    public void two_containers_have_logs_collected_in_parallel() throws IOException, InterruptedException {
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
    public void a_started_collector_cannot_be_starteda_second_time() throws IOException, InterruptedException {
        when(compose.ps()).thenReturn(new ContainerNames("db"));
        logCollector.startCollecting(compose);
        exception.expect(RuntimeException.class);
        exception.expectMessage("Cannot start collecting the same logs twice");
        logCollector.startCollecting(compose);
    }

    private File cannotBeCreatedDirectory() {
        File cannotBeCreatedDirectory = mock(File.class);
        when(cannotBeCreatedDirectory.isFile()).thenReturn(false);
        when(cannotBeCreatedDirectory.mkdirs()).thenReturn(false);
        when(cannotBeCreatedDirectory.getAbsolutePath()).thenReturn("cannot/exist/directory");
        return cannotBeCreatedDirectory;
    }

}
