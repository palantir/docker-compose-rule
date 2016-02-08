package com.palantir.docker.compose.execution;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;

import static org.apache.commons.lang3.Validate.validState;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.PortMappings;


public class DockerComposeExecutable {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeExecutable.class);

    private final DockerComposeExecutor executor;
    private final DockerMachine dockerMachine;

    public DockerComposeExecutable(File dockerComposeFile, DockerMachine dockerMachine) {
        this(new DockerComposeExecutor(dockerComposeFile), dockerMachine);
    }

    public DockerComposeExecutable(DockerComposeExecutor executor, DockerMachine dockerMachine) {
        this.executor = executor;
        this.dockerMachine = dockerMachine;
    }

    public void build() throws IOException, InterruptedException {
        executeDockerComposeCommand("build");
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
        return new Container(containerName, this, dockerMachine);
    }

    /**
     * Blocks until all logs collected from the container.
     * @return Whether the docker container terminated prior to log collection ending.
     */
    public boolean writeLogs(String container, OutputStream output) throws IOException {
        Process executedProcess = executor.execute("logs", "--no-color", container);
        IOUtils.copy(executedProcess.getInputStream(), output);
        try {
            executedProcess.waitFor(DockerComposeExecutor.COMMAND_TIMEOUT.getMillis(), MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    public PortMappings ports(String service) throws IOException, InterruptedException {
        String psOutput = executeDockerComposeCommand("ps", service);
        validState(!Strings.isNullOrEmpty(psOutput), "No container with name '" + service + "' found");
        return PortMappings.parseFromDockerComposePs(psOutput);
    }

    private String executeDockerComposeCommand(String... commands) throws IOException, InterruptedException {
        Process executedProcess = executor.executeAndWait(commands);
        String output = IOUtils.toString(executedProcess.getInputStream());
        log.debug(output);
        validState(executedProcess.exitValue() == 0, constructNonZeroExitErrorMessage(executedProcess.exitValue(), commands));
        return output;
    }

    private String constructNonZeroExitErrorMessage(int exitCode, String... commands) {
        return "'docker-compose " + Arrays.stream(commands).collect(joining(" ")) + "' returned exit code " + exitCode;
    }

}
