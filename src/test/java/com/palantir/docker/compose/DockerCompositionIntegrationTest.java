package com.palantir.docker.compose;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.core.Is.is;

public class DockerCompositionIntegrationTest {

    @Rule
    public DockerComposition composition = new DockerComposition("src/test/resources/docker-compose.yaml")
                                                .waitingForService("db")
                                                .waitingForService("db2");

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

    @SuppressWarnings("unchecked")
    @Test
    public void logsCanBeSavedToADirectory() throws IOException, InterruptedException {
        File logLocation = logFolder.newFolder();
        DockerComposition loggingComposition = composition.saveLogsTo(logLocation.getAbsolutePath());
        loggingComposition.before();
        loggingComposition.after();
        assertThat(logLocation.listFiles(), arrayContainingInAnyOrder(fileWithName("db.log"), fileWithName("db2.log")));
        assertThat(new File(logLocation, "db.log"), is(fileContainingString("Attaching to resources_db_1")));
        assertThat(new File(logLocation, "db2.log"), is(fileContainingString("Attaching to resources_db2_1")));
    }

}
