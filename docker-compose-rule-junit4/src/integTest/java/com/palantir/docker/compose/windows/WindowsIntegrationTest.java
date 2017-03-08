/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeTrue;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.State;
import java.io.IOException;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

public class WindowsIntegrationTest {

    @Test
    public void can_execute_docker_commands() throws IOException, InterruptedException {
        assumeTrue("Running on Windows", SystemUtils.IS_OS_WINDOWS);

        DockerComposeRule docker = DockerComposeRule.builder()
                .files(DockerComposeFiles.from("src/integTest/resources/windows-docker-compose.yaml"))
                .build();
        docker.before();

        try {
            assertThat(docker.containers().container("hello-world").state(), is(State.DOWN));
        } finally {
            docker.after();
        }
    }

}
