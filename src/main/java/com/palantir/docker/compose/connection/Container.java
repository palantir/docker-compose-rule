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
package com.palantir.docker.compose.connection;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.jayway.awaitility.Awaitility;
import com.palantir.docker.compose.execution.DockerCompose;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Container {

    private static final Logger log = LoggerFactory.getLogger(Container.class);

    private final String containerName;
    private final DockerCompose dockerComposeProcess;

    private final Supplier<Ports> portMappings = Suppliers.memoize(this::getDockerPorts);

    public Container(String containerName, DockerCompose dockerComposeProcess) {
        this.containerName = containerName;
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public String getContainerName() {
        return containerName;
    }

    public boolean waitForPorts(Duration timeout) {
        try {
            Ports exposedPorts = portMappings.get();
            exposedPorts.waitToBeListeningWithin(timeout);
            return true;
        } catch (Exception e) {
            log.warn("Container '" + containerName + "' failed to come up: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean waitForHttpPort(int internalPort, Function<DockerPort, String> urlFunction, Duration timeout) {
        try {
            DockerPort port = portMappedInternallyTo(internalPort);
            Awaitility.await()
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                .until(() -> assertThat(port.isListeningNow() && port.isHttpResponding(urlFunction), is(true)));
            return true;
        } catch (Exception e) {
            log.warn("Container '" + containerName + "' failed to come up: " + e.getMessage(), e);
            return false;
        }
    }

    public DockerPort portMappedExternallyTo(int externalPort) throws IOException, InterruptedException {
        return portMappings.get()
                           .stream()
                           .filter(port -> port.getExternalPort() == externalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No port mapped externally to '" + externalPort + "' for container '" + containerName + "'"));
    }

    public DockerPort portMappedInternallyTo(int internalPort) throws IOException, InterruptedException {
        return portMappings.get()
                           .stream()
                           .filter(port -> port.getInternalPort() == internalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No internal port '" + internalPort + "' for container '" + containerName + "'"));
    }

    private Ports getDockerPorts() {
        try {
            return dockerComposeProcess.ports(containerName);
        } catch (IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Container container = (Container) o;
        return Objects.equals(containerName, container.containerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerName);
    }

    @Override
    public String toString() {
        return "Container{" +
                "containerName='" + containerName + '\'' +
                '}';
    }
}
