package com.palantir.docker.compose;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;


public class DockerComposeExecutor {

    private static final List<String> dockerComposeLocations = asList("/usr/local/bin/docker-compose",
                                                                      System.getenv("DOCKER_COMPOSE_LOCATION"));
    public static final int COMMAND_TIMEOUT_IN_SECONDS = 120;

    private final File dockerComposeFile;

    public DockerComposeExecutor(File dockerComposeFile) {
        this.dockerComposeFile = dockerComposeFile;
    }

    public Process executeAndWait(String... commands) throws IOException, InterruptedException {
        Process dockerCompose = execute(commands);
        dockerCompose.waitFor(COMMAND_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        return dockerCompose;
    }

    public Process execute(String... commands) throws IOException {
        List<String> args = Lists.newArrayList(getDockerComposePath(), "-f", dockerComposeFile.getAbsolutePath());
        Collections.addAll(args, commands);
        Process dockerCompose = new ProcessBuilder().command(args)
                                                    .redirectErrorStream(true)
                                                    .start();
        return dockerCompose;
    }


    private String getDockerComposePath() {
        return dockerComposeLocations.stream()
                                     .filter(StringUtils::isNotBlank)
                                     .filter(path -> new File(path).exists())
                                     .findAny()
                                     .orElseThrow(() -> new IllegalStateException("Could not find docker-compose, looked in: " + dockerComposeLocations));
    }

}
