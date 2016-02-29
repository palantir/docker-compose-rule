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
package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.Ports;
import com.palantir.docker.compose.execution.DockerComposeExecutable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MockDockerEnvironment {

    private final DockerComposeExecutable dockerComposeProcess;

    public MockDockerEnvironment(DockerComposeExecutable dockerComposeProcess) {
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public DockerPort availableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isListeningNow();
        return port;
    }

    public DockerPort availableHttpService(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = availableService(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isHttpResponding(any());
        return port;
    }

    public DockerPort unavailableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(false).when(port).isListeningNow();
        return port;
    }

    public DockerPort port(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = dockerPortSpy(ip, externalPortNumber, internalPortNumber);
        when(dockerComposeProcess.ports(service)).thenReturn(new Ports(port));
        return port;
    }

    public void ports(String service, String ip, Integer... portNumbers) throws IOException, InterruptedException {
        List<DockerPort> ports = Arrays.asList(portNumbers)
                                         .stream()
                                         .map(portNumber -> dockerPortSpy(ip, portNumber, portNumber))
                                         .collect(Collectors.toList());
        when(dockerComposeProcess.ports(service)).thenReturn(new Ports(ports));
    }

    private DockerPort dockerPortSpy(String ip, int externalPortNumber, int internalPortNumber) {
        DockerPort port = new DockerPort(ip, externalPortNumber, internalPortNumber);
        return spy(port);
    }

}
