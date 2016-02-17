package com.palantir.docker.compose.connection;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.Map;

import com.palantir.docker.compose.configuration.EnvironmentVariables;

public class DockerMachine {

    private final EnvironmentVariables environmentVariables;

    // TODO(fdesouza): make this private/protected or whatever
    public DockerMachine(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    // TODO(fdesouza): createLocalMachine() -> returns DockerMachineBuilder
    public static DockerMachine fromEnvironment() {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(System.getenv());
        return new DockerMachine(environmentVariables);
    }

    public String getIp() {
        return environmentVariables.getDockerHostIp();
    }

    public ProcessBuilder configDockerComposeProcess() {
        ProcessBuilder process = new ProcessBuilder();
        environmentVariables.augmentGivenEnvironment(process.environment());
        return process;
    }

    // The localMachine gets things from the environment

    public static RemoteBuilder builder() {
        return new RemoteBuilder();
    }

    public static class RemoteBuilder {

        private Map<String, String> dockerEnvironment = newHashMap();
        private Map<String, String> additionalEnvironment = newHashMap();

        // This will take in a validator
        public RemoteBuilder() {}

        public RemoteBuilder host(String hostname) {
            dockerEnvironment.put(DOCKER_HOST, hostname);
            return this;
        }

        public RemoteBuilder withTLS(String certPath) {
            dockerEnvironment.put(DOCKER_TLS_VERIFY, "1");
            dockerEnvironment.put(DOCKER_CERT_PATH, certPath);
            return this;
        }

        public RemoteBuilder withoutTLS() {
            dockerEnvironment.remove(DOCKER_TLS_VERIFY);
            dockerEnvironment.remove(DOCKER_CERT_PATH);
            return this;
        }

        public RemoteBuilder withAdditionalEnvironmentVariable(String key, String value) {
            additionalEnvironment.put(key, value);
            return this;
        }

        public RemoteBuilder withEnvironment(Map<String, String> newEnvironment) {
            this.additionalEnvironment = firstNonNull(newEnvironment, newHashMap());
            return this;
        }

        public DockerMachine build() {
            EnvironmentVariables environment = new EnvironmentVariables(this.dockerEnvironment, additionalEnvironment);
            return new DockerMachine(environment);
        }

    }

}
