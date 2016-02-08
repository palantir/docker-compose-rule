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

public class DockerCompositionLoggingIntegrationTest {

    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    @Rule
    public DockerComposition loggingComposition;

    private File logLocation;

    @Before
    public void setUp() throws Exception {
        logLocation = logFolder.newFolder();
        loggingComposition = DockerComposition.of("src/test/resources/docker-compose.yaml")
                                              .waitingForService("db")
                                              .waitingForService("db2")
                                              .saveLogsTo(logLocation.getAbsolutePath())
                                              .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logsCanBeSavedToADirectory() throws IOException, InterruptedException {
        loggingComposition.after();
        assertThat(logLocation.listFiles(), arrayContainingInAnyOrder(fileWithName("db.log"), fileWithName("db2.log")));
        assertThat(new File(logLocation, "db.log"), is(fileContainingString("Attaching to resources_db_1")));
        assertThat(new File(logLocation, "db2.log"), is(fileContainingString("Attaching to resources_db2_1")));
    }

}
