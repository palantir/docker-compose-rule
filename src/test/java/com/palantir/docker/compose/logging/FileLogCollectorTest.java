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

}
