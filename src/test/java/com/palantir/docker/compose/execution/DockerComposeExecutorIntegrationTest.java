package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.DockerComposition;
import com.palantir.docker.compose.connection.DockerMachine;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DockerComposeExecutorIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void dockerComposeGetsEnvironmentVariablesFromDockerMachineAndPassesItIntoATestContainer() throws Exception {
        DockerMachine dockerMachine = DockerMachine.localMachine()
                                                   .withAdditionalEnvironmentVariable("SOME_VARIABLE", "SOME_VALUE")
                                                   .build();

        DockerComposition dockerComposition = DockerComposition.of("src/test/resources/environment/docker-compose.yaml",
                                                                   dockerMachine)
                                                               .waitingForService("env-test")
                                                               .saveLogsTo(temporaryFolder.getRoot().getAbsolutePath())
                                                               .build();
        try {
            dockerComposition.before();
        } finally {
            dockerComposition.after();
        }

        Path logLocation = temporaryFolder.getRoot()
                                          .toPath()
                                          .resolve("env-test.log");

        assertThat(logLocation.toFile(), is(fileContainingString("SOME_VARIABLE=SOME_VALUE")));
    }
}