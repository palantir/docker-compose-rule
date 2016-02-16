package com.palantir.docker.compose.execution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;

import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.palantir.docker.compose.DockerComposition;
import com.palantir.docker.compose.connection.DockerMachine;

public class DockerComposeExecutorIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void dockerComposeGetsEnvironmentVariablesFromDockerMachineAndPassesItIntoATestContainer() throws Exception {
        DockerMachine dockerMachine = DockerMachine.builder()
                                                   .host("tcp://192.168.99.100:2376")
                                                   .withoutTLS()
                                                   .withAdditionalEnvironmentVariable("SOME_VARIABLE", "SOME_VALUE")
                                                   .build();

        DockerComposition dockerComposition = DockerComposition.of("src/test/resources/environment/docker-compose.yaml",
                                                                   dockerMachine)
                                                               .waitingForService("env-test")
                                                               .saveLogsTo(temporaryFolder.getRoot().getAbsolutePath())
                                                               .build();

        dockerComposition.before();
        dockerComposition.after();
        Path logLocation = temporaryFolder.getRoot()
                                          .toPath()
                                          .resolve("env-test.log");

        assertThat(logLocation.toFile(), is(fileContainingString("SOME_VARIABLE=SOME_VALUE")));
    }
}