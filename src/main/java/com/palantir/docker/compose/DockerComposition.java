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

import com.google.common.collect.ImmutableMap;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import org.apache.commons.lang3.Validate;
import org.joda.time.Duration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class DockerComposition extends ExternalResource {

    private static final Logger log = LoggerFactory.getLogger(DockerComposition.class);

    private final DockerComposeExecutable dockerComposeProcess;
    private final ContainerCache containers;
    private final Map<Container, Function<Container, Boolean>> servicesToWaitFor;
    private final Duration serviceTimeout;
    private final LogCollector logCollector;

    public static DockerCompositionBuilder of(String dockerComposeFile) {
        return of(DockerComposeFiles.from(dockerComposeFile));
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles) {
        return of(dockerComposeFiles,
                  DockerMachine.localMachine()
                               .build());
    }

    public static DockerCompositionBuilder of(String dockerComposeFile, DockerMachine dockerMachine) {
        return of(DockerComposeFiles.from(dockerComposeFile), dockerMachine);
    }

    public static DockerCompositionBuilder of(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine) {
        return of(new DockerComposeExecutable(dockerComposeFiles, dockerMachine));
    }

    public static DockerCompositionBuilder of(DockerComposeExecutable executable) {
        return new DockerCompositionBuilder(executable);
    }

    private DockerComposition(DockerComposeExecutable dockerComposeProcess,
                              Map<Container, Function<Container, Boolean>> servicesToWaitFor,
                              Duration serviceTimeout,
                              LogCollector logCollector,
                              ContainerCache containers) {
        this.dockerComposeProcess = dockerComposeProcess;
        this.servicesToWaitFor = ImmutableMap.copyOf(servicesToWaitFor);
        this.serviceTimeout = serviceTimeout;
        this.logCollector = logCollector;
        this.containers = containers;
    }

    @Override
    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");
        dockerComposeProcess.build();
        dockerComposeProcess.up();

        log.debug("Starting log collection");

        logCollector.startCollecting(dockerComposeProcess);
        log.debug("Waiting for services");
        servicesToWaitFor.entrySet().forEach(serviceCheck -> waitForService(serviceCheck.getKey(), serviceCheck.getValue()));
        log.debug("docker-compose cluster started");
    }

    private void waitForService(Container service, Function<Container, Boolean> serviceCheck) {
        log.debug("Waiting for service '{}'", service);
        try {
            Awaitility.await()
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .atMost(serviceTimeout.getMillis(), TimeUnit.MILLISECONDS)
                .until(() -> serviceCheck.apply(service));
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException("Container '" + service.getContainerName() + "' failed to pass startup check");
        }
    }

    @Override
    public void after() {
        try {
            log.debug("Killing docker-compose cluster");
            dockerComposeProcess.down();
            dockerComposeProcess.kill();
            dockerComposeProcess.rm();
            logCollector.stopCollecting();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error cleaning up docker compose cluster", e);
        }
    }

    public DockerPort portOnContainerWithExternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return containers.get(container)
                         .portMappedExternallyTo(portNumber);
    }

    public DockerPort portOnContainerWithInternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return containers.get(container)
                         .portMappedInternallyTo(portNumber);
    }

    public static class DockerCompositionBuilder {

        private final Map<String, Function<Container, Boolean>> containersToWaitFor = new HashMap<>();
        private final DockerComposeExecutable dockerComposeProcess;
        private final ContainerCache containers;
        private Duration serviceTimeout = Duration.standardMinutes(2);
        private LogCollector logCollector = new DoNothingLogCollector();

        public DockerCompositionBuilder(DockerComposeExecutable dockerComposeProcess) {
            this.dockerComposeProcess = dockerComposeProcess;
            this.containers = new ContainerCache(dockerComposeProcess);
        }

        public DockerCompositionBuilder waitingForService(String serviceName) {
            return waitingForService(serviceName, (container) -> container.waitForPorts(serviceTimeout));
        }

        public DockerCompositionBuilder waitingForHttpService(String serviceName, int internalPort, Function<DockerPort, String> urlFunction) {
            return waitingForService(serviceName, (container) -> container.waitForHttpPort(internalPort, urlFunction, serviceTimeout));
        }

        public DockerCompositionBuilder waitingForService(String serviceName, Function<Container, Boolean> check) {
            containersToWaitFor.put(serviceName, check);
            return this;
        }

        public DockerCompositionBuilder serviceTimeout(Duration timeout) {
            this.serviceTimeout = timeout;
            return this;
        }

        public DockerCompositionBuilder saveLogsTo(String path) {
            File logDirectory = new File(path);
            Validate.isTrue(!logDirectory.isFile(), "Log directory cannot be a file");
            if (!logDirectory.exists()) {
                Validate.isTrue(logDirectory.mkdirs(), "Error making log directory");
            }
            this.logCollector = new FileLogCollector(logDirectory);
            return this;
        }

        public DockerComposition build() {
            Map<Container, Function<Container, Boolean>> servicesToWaitFor = containersToWaitFor.entrySet()
                                                                                                .stream()
                                                                                                .collect(toMap(e -> containers.get(e.getKey()), Map.Entry::getValue));
            return new DockerComposition(dockerComposeProcess, servicesToWaitFor, serviceTimeout, logCollector, containers);
        }

    }

}
