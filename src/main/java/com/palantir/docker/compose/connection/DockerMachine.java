package com.palantir.docker.compose.connection;

import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.configuration.DockerEnvironmentVariables;

public class DockerMachine {

    private final String machineIp;
    private final Map<String, String> dockerEnvironmentVariables;

    public DockerMachine(DockerEnvironmentVariables dockerEnvironment) {
        dockerEnvironment.checkEnvVariables();
        this.machineIp = dockerEnvironment.getDockerHostIp();
        this.dockerEnvironmentVariables = dockerEnvironment.getDockerEnvironmentVariables();
    }

    public static DockerMachine fromEnvironment() {
        DockerEnvironmentVariables envVars = new DockerEnvironmentVariables(System.getenv());
        envVars.checkEnvVariables();
        return new DockerMachine(envVars);
    }

    public String getIp() {
        return machineIp;
    }

    public Map<String, String> getDockerEnvironmentVariables() {
        return ImmutableMap.copyOf(dockerEnvironmentVariables);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DockerMachine that = (DockerMachine) o;
        return Objects.equals(machineIp, that.machineIp) &&
                Objects.equals(dockerEnvironmentVariables, that.dockerEnvironmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineIp, dockerEnvironmentVariables);
    }

    @Override
    public String toString() {
        return "DockerMachine{" +
                "machineIp='" + machineIp + '\'' +
                ", dockerEnvironmentVariables=" + dockerEnvironmentVariables +
                '}';
    }

    public static DockerMachineBuilder builder() {
        return new DockerMachineBuilder();
    }

    public static class DockerMachineBuilder {

        private Map<String, String> environment = new HashMap<>();

        public DockerMachineBuilder() {
            environment.put(DOCKER_TLS_VERIFY, "0");
        }

        public DockerMachineBuilder host(String hostname) {
            environment.put(DOCKER_HOST, hostname);
            return this;
        }

        public DockerMachineBuilder certPath(String certPath) {
            environment.put(DOCKER_CERT_PATH, certPath);
            return this;
        }

        public DockerMachineBuilder withTLS() {
            environment.put(DOCKER_TLS_VERIFY, "1");
            return this;
        }

        public DockerMachineBuilder withoutTLS() {
            environment.put(DOCKER_TLS_VERIFY, "0");
            environment.remove(DOCKER_CERT_PATH);
            return this;
        }

        public DockerMachine build() {
            DockerEnvironmentVariables dockerEnvironment = new DockerEnvironmentVariables(environment);
            dockerEnvironment.checkEnvVariables();
            return new DockerMachine(dockerEnvironment);
        }

    }

}
