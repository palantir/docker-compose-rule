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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileLogCollector implements LogCollector {

    private static final Logger log = LoggerFactory.getLogger(FileLogCollector.class);

    private static final long STOP_TIMEOUT_IN_MILLIS = 50;

    private final File logDirectory;

    private ExecutorService executor = null;

    public FileLogCollector(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    @Override
    public synchronized void startCollecting(DockerCompose dockerCompose) throws IOException, InterruptedException {
        if (executor != null) {
            throw new RuntimeException("Cannot start collecting the same logs twice");
        }
        ContainerNames containerNames = dockerCompose.ps();
        if (containerNames.size() == 0) {
            return;
        }
        executor = Executors.newFixedThreadPool(containerNames.size());
        containerNames.stream()
                      .forEachOrdered(container -> this.collectLogs(container, dockerCompose));
    }

    private void collectLogs(String container, DockerCompose dockerCompose)  {
        executor.submit(() -> {
            File outputFile = new File(logDirectory, container + ".log");
            log.info("Writing logs for container '{}' to '{}'", container, outputFile.getAbsolutePath());
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                dockerCompose.writeLogs(container, outputStream);
            } catch (IOException e) {
                throw new RuntimeException("Error reading log", e);
            }
        });
    }

    @Override
    public synchronized void stopCollecting() throws InterruptedException {
        if (executor == null) {
            return;
        }
        if (!executor.awaitTermination(STOP_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS)) {
            log.warn("docker containers were still running when log collection stopped");
            executor.shutdownNow();
        }
    }

}
