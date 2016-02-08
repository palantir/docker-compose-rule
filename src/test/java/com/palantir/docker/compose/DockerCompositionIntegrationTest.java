package com.palantir.docker.compose;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerCompositionIntegrationTest {

    @Rule
    public DockerComposition composition = DockerComposition.of("src/test/resources/docker-compose.yaml")
                                                            .waitingForService("db")
                                                            .waitingForService("db2")
                                                            .build();

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    @Test
    public void shouldRunDockerComposeUpUsingTheSpecifiedDockerComposeFileToBringPostgresUp() throws InterruptedException, IOException {
        assertThat(composition.portOnContainerWithExternalMapping("db", 5433).isListeningNow(), is(true));
    }

    @Test
    public void afterTestIsExecutedTheLaunchedPostgresContainerIsNoLongerListening() throws IOException, InterruptedException {
        composition.after();
        assertThat(composition.portOnContainerWithExternalMapping("db", 5433).isListeningNow(), is(false));
    }

    @Test
    public void canAccessExternalPortForInternalPortOfMachine() throws IOException, InterruptedException {
        assertThat(composition.portOnContainerWithInternalMapping("db", 5432).isListeningNow(), is(true));
    }

}
