package com.palantir.docker.compose;


import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.DockerExecutable;
import com.palantir.docker.compose.logging.LogCollector;

public interface DockerExecutionContext {

    DockerMachine machine();

    DockerComposeFiles files();

    ProjectName projectName();

    LogCollector logCollector();

    Docker docker();

    DockerExecutable dockerExecutable();

    DockerCompose dockerCompose();

    DockerComposeExecutable dockerComposeExecutable();

    Cluster containers();

}
