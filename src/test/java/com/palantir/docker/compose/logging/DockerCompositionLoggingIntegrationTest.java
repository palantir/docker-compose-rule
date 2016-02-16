package com.palantir.docker.compose.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.is;

import static com.palantir.docker.compose.matchers.IOMatchers.fileContainingString;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.palantir.docker.compose.DockerComposition;
import com.palantir.docker.compose.connection.DockerMachine;

public class DockerCompositionLoggingIntegrationTest {

    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private DockerComposition loggingComposition;

    @Before
    public void setUp() throws Exception {
        loggingComposition = DockerComposition.of("src/test/resources/docker-compose.yaml", DockerMachine.fromEnvironment())
                                              .waitingForService("db")
                                              .waitingForService("db2")
                                              .saveLogsTo(logFolder.getRoot().getAbsolutePath())
                                              .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logsCanBeSavedToADirectory() throws IOException, InterruptedException {
        try {
            loggingComposition.before();
        } finally {
            loggingComposition.after();
        }
        assertThat(logFolder.getRoot().listFiles(), arrayContainingInAnyOrder(fileWithName("db.log"), fileWithName("db2.log")));
        assertThat(new File(logFolder.getRoot(), "db.log"), is(fileContainingString("Attaching to resources_db_1")));
        assertThat(new File(logFolder.getRoot(), "db2.log"), is(fileContainingString("Attaching to resources_db2_1")));
    }

}
