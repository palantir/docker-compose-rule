package com.palantir.docker.compose.connection;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.DockerEnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;
import com.palantir.docker.compose.configuration.DockerEnvironmentVariables;

public class DockerMachine {

    private final String machineIp;
    private final Map<String, String> dockerEnvironmentVariables;
    private final Map<String, String> additionalEnvironmentVariables;

    public DockerMachine(DockerEnvironmentVariables dockerEnvironment, Map<String, String> additionalEnvironmentVariables) {
        dockerEnvironment.checkEnvVariables();
        this.machineIp = dockerEnvironment.getDockerHostIp();
        this.dockerEnvironmentVariables = dockerEnvironment.getDockerEnvironmentVariables();
        this.additionalEnvironmentVariables = additionalEnvironmentVariables;
    }

    public DockerMachine(DockerEnvironmentVariables dockerEnvironment) {
        this(dockerEnvironment, newHashMap());
    }

    public static DockerMachine fromEnvironment() {
        DockerEnvironmentVariables envVars = new DockerEnvironmentVariables(System.getenv());
        envVars.checkEnvVariables();
        return new DockerMachine(envVars);
    }

    public String getIp() {
        return machineIp;
    }

    public ProcessBuilder configDockerComposeProcess() {
        ProcessBuilder process = new ProcessBuilder();
        Map<String, String> environment = process.environment();
        environment.putAll(dockerEnvironmentVariables);
        environment.putAll(additionalEnvironmentVariables);
        return process;
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
                Objects.equals(dockerEnvironmentVariables, that.dockerEnvironmentVariables) &&
                Objects.equals(additionalEnvironmentVariables, that.additionalEnvironmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineIp, dockerEnvironmentVariables, additionalEnvironmentVariables);
    }

    @Override
    public String toString() {
        return "DockerMachine{" +
                "machineIp='" + machineIp + '\'' +
                ", dockerEnvironmentVariables=" + dockerEnvironmentVariables +
                ", additionalEnvironmentVariables=" + additionalEnvironmentVariables +
                '}';
    }

    public static DockerMachineBuilder builder() {
        return new DockerMachineBuilder();
    }

    public static class DockerMachineBuilder {

        private Map<String, String> environment = newHashMap();
        private Map<String, String> additionalEnvironment = newHashMap();

        public DockerMachineBuilder() {
            environment.put(DOCKER_TLS_VERIFY, "0");
        }

        public DockerMachineBuilder host(String hostname) {
            environment.put(DOCKER_HOST, hostname);
            return this;
        }

        public DockerMachineBuilder withTLS(String certPath) {
            environment.put(DOCKER_TLS_VERIFY, "1");
            environment.put(DOCKER_CERT_PATH, certPath);

            return this;
        }

        public DockerMachineBuilder withoutTLS() {
            environment.remove(DOCKER_TLS_VERIFY);
            environment.remove(DOCKER_CERT_PATH);
            return this;
        }

        public DockerMachineBuilder withAdditionalEnvironmentVariable(String key, String value) {
            additionalEnvironment.put(key, value);
            return this;
        }

        public DockerMachineBuilder withEnvironment(Map<String, String> newEnvironment) {
            this.additionalEnvironment = firstNonNull(newEnvironment, newHashMap());
            return this;
        }

        public DockerMachine build() {
            Map<String, String> combinedEnvironment = Maps.newHashMap(additionalEnvironment);
            combinedEnvironment.putAll(environment);

            DockerEnvironmentVariables dockerEnvironment = new DockerEnvironmentVariables(combinedEnvironment);
            dockerEnvironment.checkEnvVariables();
            return new DockerMachine(dockerEnvironment, additionalEnvironment);
        }

    }

}
