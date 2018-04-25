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

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.palantir.docker.compose.execution.DockerCompose;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("unchecked")
public class FileLogCollectorShould {

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
    public void throw_exception_when_created_with_file_as_the_log_directory() throws IOException {
        File file = logDirectoryParent.newFile("cannot-use");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("cannot be a file");

        new FileLogCollector(file);
    }

    @Test
    public void create_the_log_directory_if_it_does_not_already_exist() {
        File doesNotExistYetDirectory = logDirectoryParent.getRoot()
                .toPath()
                .resolve("doesNotExist")
                .toFile();
        new FileLogCollector(doesNotExistYetDirectory);
        assertThat(doesNotExistYetDirectory.exists(), is(true));
    }

    @Test
    public void throw_exception_when_created_if_the_log_directory_does_not_exist_and_cannot_be_created() {
        File cannotBeCreatedDirectory = cannotBeCreatedDirectory();

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Error making");
        exception.expectMessage(cannotBeCreatedDirectory.getAbsolutePath());

        new FileLogCollector(cannotBeCreatedDirectory);
    }

    @Test
    public void not_collect_any_logs_when_no_containers_are_running() throws IOException, InterruptedException {
        when(compose.services()).thenReturn(ImmutableList.of());
        logCollector.startCollecting(compose);
        logCollector.stopCollecting();
        assertThat(logDirectory.list(), is(emptyArray()));
    }

    @Test
    public void collect_logs_when_one_container_is_running_and_terminates_before_start_collecting_is_run()
            throws Exception {
        when(compose.services()).thenReturn(ImmutableList.of("db"));
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
    public void collect_logs_when_one_container_is_running_and_does_not_terminate_until_after_start_collecting_is_run()
            throws Exception {
        when(compose.services()).thenReturn(ImmutableList.of("db"));
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
    public void collect_logs_when_one_container_is_running_and_does_not_terminate()
            throws IOException, InterruptedException {
        when(compose.services()).thenReturn(ImmutableList.of("db"));
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
    public void collect_logs_in_parallel_for_two_containers() throws IOException, InterruptedException {
        when(compose.services()).thenReturn(ImmutableList.of("db", "db2"));
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
    public void throw_exception_when_trying_to_start_a_started_collector_a_second_time()
            throws IOException, InterruptedException {
        when(compose.services()).thenReturn(ImmutableList.of("db"));
        logCollector.startCollecting(compose);
        exception.expect(RuntimeException.class);
        exception.expectMessage("Cannot start collecting the same logs twice");
        logCollector.startCollecting(compose);
    }

    @Test
    public void collect_logs_for_partial_services() throws IOException, InterruptedException {
        when(compose.services()).thenReturn(ImmutableList.of("db", "db2"));
        when(compose.servicesToStart()).thenReturn(ImmutableList.of("db"));

        List<Exception> exceptions = Lists.newArrayList();
        CountDownLatch latch = new CountDownLatch(1);
        when(compose.writeLogs(anyString(), any(OutputStream.class))).thenAnswer((args) -> {
            String container = (String) args.getArguments()[0];
            if (!"db".equals(container)) {
                exceptions.add(new IOException("Logs shouldn't be written for container: " + container));
            } else {
                latch.countDown();
            }
            return true;
        });

        logCollector.startCollecting(compose);
        assertThat(latch.await(1, TimeUnit.SECONDS), is(true));
        logCollector.stopCollecting();
        assertThat(exceptions, is(empty()));
    }

    private static File cannotBeCreatedDirectory() {
        File cannotBeCreatedDirectory = mock(File.class);
        when(cannotBeCreatedDirectory.isFile()).thenReturn(false);
        when(cannotBeCreatedDirectory.mkdirs()).thenReturn(false);
        when(cannotBeCreatedDirectory.getAbsolutePath()).thenReturn("cannot/exist/directory");
        return cannotBeCreatedDirectory;
    }
}
