package com.palantir.docker.compose.execution;

import com.google.common.collect.Lists;
import com.palantir.docker.compose.connection.DockerMachine;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.joda.time.Duration.standardMinutes;


public class DockerComposeExecutor {

    private static final List<String> dockerComposeLocations = asList("/usr/local/bin/docker-compose",
                                                                      System.getenv("DOCKER_COMPOSE_LOCATION"));
    public static final Duration COMMAND_TIMEOUT = standardMinutes(2);

    private final File dockerComposeFile;
    private final DockerMachine dockerMachine;

    public DockerComposeExecutor(File dockerComposeFile, DockerMachine dockerMachine) {
        this.dockerComposeFile = dockerComposeFile;
        this.dockerMachine = dockerMachine;
    }

    public Process executeAndWait(String... commands) throws IOException, InterruptedException {
        Process dockerCompose = execute(commands);
        dockerCompose.waitFor(COMMAND_TIMEOUT.getMillis(), MILLISECONDS);
        return dockerCompose;
    }

    public Process execute(String... commands) throws IOException {
        List<String> args = Lists.newArrayList(getDockerComposePath(), "-f", dockerComposeFile.getAbsolutePath());
        Collections.addAll(args, commands);
        return dockerMachine.configDockerComposeProcess()
                            .command(args)
                            .redirectErrorStream(true)
                            .start();
    }

    private static String getDockerComposePath() {
        return dockerComposeLocations.stream()
                                     .filter(StringUtils::isNotBlank)
                                     .filter(path -> new File(path).exists())
                                     .findAny()
                                     .orElseThrow(() -> new IllegalStateException("Could not find docker-compose, looked in: " + dockerComposeLocations));
    }

}
