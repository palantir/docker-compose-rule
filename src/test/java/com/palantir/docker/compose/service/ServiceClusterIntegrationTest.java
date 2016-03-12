/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import com.palantir.docker.compose.DockerComposition;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceClusterIntegrationTest {

    private final DockerService postgresFromComposeFileSnippet = DockerService.fromDockerCompositionFile("src/test/resources/postgres-service.yaml")
                                                                              .withHealthCheck("postgres", HealthChecks.toHaveAllPortsOpen());
    private final DockerService postgresDefinedInline = DockerService.fromImage("kiasaki/alpine-postgres", "inlinePostgres")
                                                                     .withPortMapping(5432)
                                                                     .withHealthCheck(HealthChecks.toHaveAllPortsOpen());

    private final ServiceCluster services = ServiceCluster.of(postgresFromComposeFileSnippet, postgresDefinedInline);

    @Rule
    public DockerComposition composition = DockerComposition.fromServiceCluster(services)
                                                            .build();

    @Test
    public void should_run_docker_compose_up_with_docker_compose_files_from_additional_docker_compose_file() throws IOException, InterruptedException {
        assertThat(composition.portOnContainerWithInternalMapping("postgres", 5432).isListeningNow(), is(true));
    }

    @Test
    public void should_run_docker_compose_up_with_docker_compose_files_from_inline_service_definition() throws IOException, InterruptedException {
        assertThat(composition.portOnContainerWithInternalMapping("inlinePostgres", 5432).isListeningNow(), is(true));
    }

}
