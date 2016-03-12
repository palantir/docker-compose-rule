/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import com.palantir.docker.compose.DockerCompositionBuilder;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ServiceClusterTest {

    @Test
    public void service_cluster_with_one_service_adds_healthchecks_from_that_service_to_a_docker_composition() {
        HealthCheck healthCheck = mock(HealthCheck.class);
        DockerService service = DockerService.fromDockerCompositionFile("src/test/resources/docker-compose.yaml")
                                             .withHealthCheck("service", healthCheck);
        ServiceCluster cluster = ServiceCluster.of(service);

        DockerCompositionBuilder composition = mock(DockerCompositionBuilder.class);
        cluster.addToComposition(composition);

        verify(composition).waitingForService("service", healthCheck, DockerService.DEFAULT_TIMEOUT);
    }

    @Test
    public void service_cluster_with_one_service_has_docker_compose_file_from_that_service() {
        DockerService service = DockerService.fromDockerCompositionFile("src/test/resources/docker-compose.yaml");
        ServiceCluster cluster = ServiceCluster.of(service);

        assertThat(cluster.dockerComposeFiles(), is(DockerComposeFiles.from("src/test/resources/docker-compose.yaml")));
    }

    @Test
    public void service_cluster_with_two_services_add_both_healthchecks_from_those_services() {
        HealthCheck firstServiceHealthCheck = mock(HealthCheck.class);
        DockerService firstService = DockerService.fromDockerCompositionFile("src/test/resources/docker-compose.yaml")
                                                  .withHealthCheck("firstService", firstServiceHealthCheck);
        HealthCheck secondServiceHealthCheck = mock(HealthCheck.class);
        DockerService secondService = DockerService.fromDockerCompositionFile("src/test/resources/docker-compose.yaml")
                                                   .withHealthCheck("secondService", secondServiceHealthCheck);
        ServiceCluster cluster = ServiceCluster.of(firstService, secondService);

        DockerCompositionBuilder composition = mock(DockerCompositionBuilder.class);
        cluster.addToComposition(composition);

        verify(composition).waitingForService("firstService", firstServiceHealthCheck, DockerService.DEFAULT_TIMEOUT);
        verify(composition).waitingForService("secondService", secondServiceHealthCheck, DockerService.DEFAULT_TIMEOUT);
    }

    @Test
    public void service_cluster_with_two_services_has_docker_compose_file_from_both_services() {
        DockerService firstService = DockerService.fromDockerCompositionFile("src/test/resources/docker-compose.yaml");
        DockerService secondService = DockerService.fromDockerCompositionFile("src/test/resources/environment/docker-compose.yaml");
        ServiceCluster cluster = ServiceCluster.of(firstService, secondService);

        assertThat(cluster.dockerComposeFiles(),
                is(DockerComposeFiles.from("src/test/resources/docker-compose.yaml",
                                           "src/test/resources/environment/docker-compose.yaml")));
    }

    @Test
    public void two_service_clusters_can_be_combined_into_one_that_includes_both_docker_compose_files() {
        DockerService firstService = DockerService.fromDockerCompositionFile("src/test/resources/docker-compose.yaml");
        DockerService secondService = DockerService.fromDockerCompositionFile("src/test/resources/environment/docker-compose.yaml");
        ServiceCluster firstCluster = ServiceCluster.of(firstService);
        ServiceCluster secondCluster = ServiceCluster.of(secondService);
        ServiceCluster cluster = firstCluster.combine(secondCluster);

        assertThat(cluster.dockerComposeFiles(),
                is(DockerComposeFiles.from("src/test/resources/docker-compose.yaml",
                                           "src/test/resources/environment/docker-compose.yaml")));
    }

    @Test
    public void a_service_cluster_can_be_combined_with_another_service_that_includes_both_docker_compose_files() {
        DockerService firstService = DockerService.fromDockerCompositionFile("src/test/resources/docker-compose.yaml");
        DockerService secondService = DockerService.fromDockerCompositionFile("src/test/resources/environment/docker-compose.yaml");
        ServiceCluster firstCluster = ServiceCluster.of(firstService);
        ServiceCluster cluster = firstCluster.combine(secondService);

        assertThat(cluster.dockerComposeFiles(),
                is(DockerComposeFiles.from("src/test/resources/docker-compose.yaml",
                                           "src/test/resources/environment/docker-compose.yaml")));
    }

}
