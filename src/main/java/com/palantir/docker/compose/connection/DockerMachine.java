package com.palantir.docker.compose.connection;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.EnvironmentValidator.getLocalEnvironmentValidator;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_TLS_VERIFY;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.configuration.EnvironmentValidator;
import com.palantir.docker.compose.configuration.EnvironmentVariables2;
import com.palantir.docker.compose.configuration.HostIpResolver;

public class DockerMachine {

    private final EnvironmentVariables2 environmentVariables;

    // TODO(fdesouza): make this private/protected or whatever
    public DockerMachine(EnvironmentVariables2 environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public String getIp() {
        return environmentVariables.getHostIp();
    }

    public ProcessBuilder configDockerComposeProcess() {
        ProcessBuilder process = new ProcessBuilder();
        environmentVariables.augmentGivenEnvironment(process.environment());
        return process;
    }

    // The localMachine gets things from the environment
    public static LocalBuilder localMachine() {
        return new LocalBuilder(System.getenv());
    }

    public static class LocalBuilder {

        private final Map<String, String> systemEnvironment;
        private Map<String, String> additionalEnvironment = new HashMap<>();

        public LocalBuilder(Map<String, String> systemEnvironment) {
            this.systemEnvironment = ImmutableMap.copyOf(systemEnvironment);
        }

        public LocalBuilder withAdditionalEnvironmentVariable(String key, String value) {
            additionalEnvironment.put(key, value);
            return this;
        }

        public LocalBuilder withEnvironment(Map<String, String> newEnvironment) {
            this.additionalEnvironment = ImmutableMap.copyOf(firstNonNull(newEnvironment, newHashMap()));
            return this;
        }

        public DockerMachine build() {
            EnvironmentVariables2 environment = new EnvironmentVariables2(getLocalEnvironmentValidator(systemEnvironment),
                                                                          HostIpResolver.getLocalIpResolver(systemEnvironment),
                                                                          systemEnvironment,
                                                                          additionalEnvironment);
            return new DockerMachine(environment);
        }
    }

    public static RemoteBuilder remoteMachine() {
        return new RemoteBuilder();
    }

    public static class RemoteBuilder {

        private Map<String, String> dockerEnvironment = newHashMap();
        private Map<String, String> additionalEnvironment = newHashMap();

        private RemoteBuilder() {}

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
            EnvironmentVariables2 environment = new EnvironmentVariables2(EnvironmentValidator.REMOTE,
                                                                         HostIpResolver.REMOTE,
                                                                         dockerEnvironment,
                                                                         additionalEnvironment);
            return new DockerMachine(environment);
        }

    }

}
