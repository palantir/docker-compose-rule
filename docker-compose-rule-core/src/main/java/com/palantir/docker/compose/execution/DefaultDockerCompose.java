/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.docker.compose.execution;

import static org.apache.commons.lang3.Validate.validState;
import static org.joda.time.Duration.standardMinutes;

import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerName;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.Ports;
import com.palantir.docker.compose.helpers.VersionHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDockerCompose implements DockerCompose {

    public static final Version VERSION_1_7_0 = Version.valueOf("1.7.0");
    private static final Duration LOG_TIMEOUT = standardMinutes(1);
    private static final Logger log = LoggerFactory.getLogger(DefaultDockerCompose.class);

    private final Command command;
    private final DockerMachine dockerMachine;
    private final DockerComposeExecutable rawExecutable;


    public DefaultDockerCompose(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine, ProjectName projectName) {
        this(DockerComposeExecutable.builder()
                .dockerComposeFiles(dockerComposeFiles)
                .dockerConfiguration(dockerMachine)
                .projectName(projectName)
                .build(), dockerMachine);
    }

    public DefaultDockerCompose(DockerComposeExecutable rawExecutable, DockerMachine dockerMachine) {
        this.rawExecutable = rawExecutable;
        this.command = new Command(rawExecutable, log::trace);
        this.dockerMachine = dockerMachine;
    }

    @Override
    public Version version() throws IOException, InterruptedException {
        String version = command.execute(Command.throwingOnError(), "version", "--short");
        return VersionHelper.toSemVer(version);
    }

    @Override
    public void pull() throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "pull");
    }

    @Override
    public void build() throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "build");
    }

    @Override
    public void up() throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "up", "-d");
    }

    @Override
    public void down() throws IOException, InterruptedException {
        command.execute(swallowingDownCommandDoesNotExist(), "down", "--volumes");
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "stop");

    }

    @Override
    public void kill() throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "kill");
    }

    @Override
    public void rm() throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "rm", "--force", "-v");
    }

    @Override
    public void up(Container container) throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "up", "-d",  container.getContainerName());
    }

    @Override
    public void start(Container container) throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "start", container.getContainerName());
    }

    @Override
    public void stop(Container container) throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "stop", container.getContainerName());
    }

    @Override
    public void kill(Container container) throws IOException, InterruptedException {
        command.execute(Command.throwingOnError(), "kill", container.getContainerName());
    }

    @Override
    public String exec(DockerComposeExecOption dockerComposeExecOption, String containerName,
            DockerComposeExecArgument dockerComposeExecArgument) throws IOException, InterruptedException {
        verifyDockerComposeVersionAtLeast(VERSION_1_7_0, "You need at least docker-compose 1.7 to run docker-compose exec");
        String[] fullArgs = constructFullDockerComposeExecArguments(dockerComposeExecOption, containerName, dockerComposeExecArgument);
        return command.execute(Command.throwingOnError(), fullArgs);
    }

    @Override
    public String run(DockerComposeRunOption dockerComposeRunOption, String containerName,
            DockerComposeRunArgument dockerComposeRunArgument) throws IOException, InterruptedException {
        String[] fullArgs = constructFullDockerComposeRunArguments(dockerComposeRunOption, containerName, dockerComposeRunArgument);
        return command.execute(Command.throwingOnError(), fullArgs);
    }

    private void verifyDockerComposeVersionAtLeast(Version targetVersion, String message) throws IOException, InterruptedException {
        validState(version().greaterThanOrEqualTo(targetVersion), message);
    }

    private static String[] constructFullDockerComposeExecArguments(DockerComposeExecOption dockerComposeExecOption,
            String containerName, DockerComposeExecArgument dockerComposeExecArgument) {
        // The "-T" option here disables pseudo-TTY allocation, which is not useful here since we are not using
        // terminal features here (e.g. we are not sending ^C to kill the executed process).
        // Disabling pseudo-TTY allocation means this will work on OS's that don't support TTY (i.e. Windows)
        ImmutableList<String> fullArgs = new ImmutableList.Builder<String>().add("exec")
                                                                            .add("-T")
                                                                            .addAll(dockerComposeExecOption.options())
                                                                            .add(containerName)
                                                                            .addAll(dockerComposeExecArgument.arguments())
                                                                            .build();
        return fullArgs.toArray(new String[fullArgs.size()]);
    }

    private static String[] constructFullDockerComposeRunArguments(DockerComposeRunOption dockerComposeRunOption,
            String containerName, DockerComposeRunArgument dockerComposeRunArgument) {
        ImmutableList<String> fullArgs = new ImmutableList.Builder<String>().add("run")
                .addAll(dockerComposeRunOption.options())
                .add(containerName)
                .addAll(dockerComposeRunArgument.arguments())
                .build();
        return fullArgs.toArray(new String[fullArgs.size()]);
    }

    @Override
    public List<ContainerName> ps() throws IOException, InterruptedException {
        String psOutput = command.execute(Command.throwingOnError(), "ps");
        return ContainerNames.parseFromDockerComposePs(psOutput);
    }

    @Override
    public Optional<String> id(Container container) throws IOException, InterruptedException {
        return id(container.getContainerName());
    }

    @Override
    public String config() throws IOException, InterruptedException {
        return command.execute(Command.throwingOnError(), "config");
    }

    @Override
    public List<String> services() throws IOException, InterruptedException {
        String servicesOutput = command.execute(Command.throwingOnError(), "config", "--services");
        return Arrays.asList(servicesOutput.split("(\r|\n)+"));
    }

    @Override
    public boolean writeLogs(String container, OutputStream output) throws IOException {
        try {
            Process executedProcess = logs(container);
            IOUtils.copy(executedProcess.getInputStream(), output);
            boolean processFinished = executedProcess.waitFor(LOG_TIMEOUT.getMillis(), TimeUnit.MILLISECONDS);
            boolean timedOut = !processFinished;
            if (timedOut) {
                log.error("Log collection timed out after {} millis. Destroying log reading process for container {}",
                        LOG_TIMEOUT.getMillis(),
                        container);
                executedProcess.destroyForcibly();
            }
            return processFinished;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private Optional<String> id(String containerName) throws IOException, InterruptedException {
        String id = command.execute(Command.throwingOnError(), "ps", "-q", containerName);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    private Process logs(String container) throws IOException, InterruptedException {
        verifyDockerComposeVersionAtLeast(VERSION_1_7_0,
                "You need at least docker-compose 1.7 to run docker-compose logs");
        return rawExecutable.execute("logs", "--no-color", container);
    }

    @Override
    public Ports ports(String service) throws IOException, InterruptedException {
        return Ports.parseFromDockerComposePs(psOutput(service), dockerMachine.getIp());
    }

    private static ErrorHandler swallowingDownCommandDoesNotExist() {
        return (exitCode, output, commandName, commands) -> {
            if (downCommandWasPresent(output)) {
                Command.throwingOnError().handle(exitCode, output, commandName, commands);
            }

            log.warn("It looks like `docker-compose down` didn't work.");
            log.warn("This probably means your version of docker-compose doesn't support the `down` command");
            log.warn("Updating to version 1.6+ of docker-compose is likely to fix this issue.");
        };
    }

    private static boolean downCommandWasPresent(String output) {
        return !output.contains("No such command");
    }

    private String psOutput(String service) throws IOException, InterruptedException {
        String psOutput = command.execute(Command.throwingOnError(), "ps", service);
        validState(!Strings.isNullOrEmpty(psOutput), "No container with name '" + service + "' found");
        return psOutput;
    }
}
