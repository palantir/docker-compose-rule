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
package com.palantir.docker.compose.execution;

import com.google.common.base.Strings;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.Ports;
import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import static java.lang.System.lineSeparator;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.Validate.validState;
import static org.joda.time.Duration.standardMinutes;

public class DockerComposeExecutable {

    private static final Duration COMMAND_TIMEOUT = standardMinutes(2);
    private static final Logger log = LoggerFactory.getLogger(DockerComposeExecutable.class);

    private final DockerComposeExecutor executor;
    private final DockerMachine dockerMachine;

    public DockerComposeExecutable(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine) {
        this(new DockerComposeExecutor(dockerComposeFiles, dockerMachine), dockerMachine);
    }

    public DockerComposeExecutable(DockerComposeExecutor executor, DockerMachine dockerMachine) {
        this.executor = executor;
        this.dockerMachine = dockerMachine;
    }

    public void build() throws IOException, InterruptedException {
        executeDockerComposeCommand("build");
    }

    public void down() throws IOException, InterruptedException {
        executeDockerComposeCommand(swallowingDownCommandDoesNotExist(), "down");
    }

    private ErrorHandler swallowingDownCommandDoesNotExist() {
        return (exitCode, output, commands) -> {
            if(downCommandWasPresent(output)) {
                throwingOnError().handle(exitCode, output, commands);
            }

            log.warn("It looks like `docker-compose down` didn't work.");
            log.warn("This probably means your version of docker-compose doesn't support the `down` command");
            log.warn("Updating to version 1.6+ of docker-compose is likely to fix this issue.");
        };
    }

    private boolean downCommandWasPresent(String output) {
        return !output.contains("No such command");
    }

    public void up() throws IOException, InterruptedException {
        executeDockerComposeCommand("up",  "-d");
    }

    public void kill() throws IOException, InterruptedException {
        executeDockerComposeCommand("kill");
    }

    public void rm() throws IOException, InterruptedException {
        executeDockerComposeCommand("rm", "-f");
    }

    public ContainerNames ps() throws IOException, InterruptedException {
        String psOutput = executeDockerComposeCommand("ps");
        return ContainerNames.parseFromDockerComposePs(psOutput);
    }

    public Container container(String containerName) {
        return new Container(containerName, this);
    }

    /**
     * Blocks until all logs collected from the container.
     * @return Whether the docker container terminated prior to log collection ending.
     */
    public boolean writeLogs(String container, OutputStream output) throws IOException {
        Process executedProcess = executor.execute("logs", "--no-color", container);
        IOUtils.copy(executedProcess.getInputStream(), output);
        try {
            executedProcess.waitFor(COMMAND_TIMEOUT.getMillis(), MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    public Ports ports(String service) throws IOException, InterruptedException {
        String psOutput = executeDockerComposeCommand("ps", service);
        validState(!Strings.isNullOrEmpty(psOutput), "No container with name '" + service + "' found");
        return Ports.parseFromDockerComposePs(psOutput, dockerMachine.getIp());
    }


    private String executeDockerComposeCommand(String... commands) throws IOException, InterruptedException {
        return executeDockerComposeCommand(throwingOnError(), commands);
    }

    private ErrorHandler throwingOnError() {
        return (exitCode, output, commands) -> {
            log.warn(constructNonZeroExitErrorMessage(exitCode, commands));
            log.warn("The output was:");
            log.warn(output);
            throw new IllegalStateException(constructNonZeroExitErrorMessage(exitCode, commands));
        };
    }

    private String executeDockerComposeCommand(ErrorHandler errorHandler, String... commands) throws IOException, InterruptedException {
        Process dockerCompose = executor.execute(commands);
        dockerCompose.waitFor(COMMAND_TIMEOUT.getMillis(), MILLISECONDS);

        String output;

        try (BufferedReader processOutputReader =
                new BufferedReader(new InputStreamReader(dockerCompose.getInputStream(), "UTF-8"))) {
            output = processOutputReader
                .lines()
                .peek(log::debug)
                .collect(joining(lineSeparator()));
        }

        if(dockerCompose.exitValue() != 0) {
            errorHandler.handle(dockerCompose.exitValue(), output, commands);
        }

        return output;
    }

    private String constructNonZeroExitErrorMessage(int exitCode, String... commands) {
        return "'docker-compose " + Arrays.stream(commands).collect(joining(" ")) + "' returned exit code " + exitCode;
    }

}
