/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.DockerCompositionBuilder;
import com.palantir.docker.compose.configuration.DockerComposeFiles;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ServiceCluster {

    private final List<DockerService> services;

    public ServiceCluster(List<DockerService> services) {
        this.services = services;
    }

    public static ServiceCluster of(DockerService... services) {
        return new ServiceCluster(Arrays.asList(services));
    }

    public ServiceCluster combine(ServiceCluster otherCluster) {
        ImmutableList<DockerService> combinedServices = ImmutableList.<DockerService>builder()
                .addAll(services)
                .addAll(otherCluster.services)
                .build();
        return new ServiceCluster(combinedServices);
    }

    public ServiceCluster combine(DockerService otherService) {
        ImmutableList<DockerService> combinedServices = ImmutableList.<DockerService>builder()
                .addAll(services)
                .add(otherService)
                .build();
        return new ServiceCluster(combinedServices);
    }

    public DockerComposeFiles dockerComposeFiles() {
        List<File> composeFiles = services.stream()
                .map(DockerService::dockerComposeFileLocation)
                .map(Optional::get)
                .collect(toList());
        return new DockerComposeFiles(composeFiles);
    }

    public void addToComposition(DockerCompositionBuilder builder) {
        services.forEach(service -> service.addWaits(builder));
    }

}
