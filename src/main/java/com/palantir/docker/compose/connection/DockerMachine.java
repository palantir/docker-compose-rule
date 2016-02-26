package com.palantir.docker.compose.connection;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.configuration.AdditionalEnvironmentValidator;
import com.palantir.docker.compose.configuration.DaemonEnvironmentValidator;
import com.palantir.docker.compose.configuration.DaemonHostIpResolver;
import com.palantir.docker.compose.configuration.DockerType;
import com.palantir.docker.compose.configuration.HostIpResolver;
import com.palantir.docker.compose.configuration.RemoteEnvironmentValidator;
import com.palantir.docker.compose.configuration.RemoteHostIpResolver;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

public class DockerMachine {

    private final String hostIp;
    private final Map<String, String> environment;

    public DockerMachine(String hostIp, Map<String, String> environment) {
        this.hostIp = hostIp;
        this.environment = environment;
    }

    public String getIp() {
        return hostIp;
    }

    public ProcessBuilder configDockerComposeProcess() {
        ProcessBuilder process = new ProcessBuilder();
        augmentGivenEnvironment(process.environment());
        return process;
    }

    private void augmentGivenEnvironment(Map<String, String> environmentToAugment) {
        environmentToAugment.putAll(environment);
    }

    public static LocalBuilder localMachine() {
        return new LocalBuilder(DockerType.getLocalDockerType(), System.getenv());
    }

    public static class LocalBuilder {

        private final DockerType dockerType;
        private final Map<String, String> systemEnvironment;
        private Map<String, String> additionalEnvironment = new HashMap<>();

        LocalBuilder(DockerType dockerType, Map<String, String> systemEnvironment) {
            this.dockerType = dockerType;
            this.systemEnvironment = ImmutableMap.copyOf(systemEnvironment);
        }

        public LocalBuilder withAdditionalEnvironmentVariable(String key, String value) {
            additionalEnvironment.put(key, value);
            return this;
        }

        public LocalBuilder withEnvironment(Map<String, String> newEnvironment) {
            this.additionalEnvironment = newHashMap(firstNonNull(newEnvironment, newHashMap()));
            return this;
        }

        public DockerMachine build() {
            HostIpResolver hostIp;
            if (DockerType.DAEMON == dockerType) {
                DaemonEnvironmentValidator.validate(systemEnvironment);
                hostIp = new DaemonHostIpResolver();
            } else {
                RemoteEnvironmentValidator.validate(systemEnvironment);
                hostIp = new RemoteHostIpResolver();
            }
            AdditionalEnvironmentValidator.validate(additionalEnvironment);
            Map<String, String> environment = ImmutableMap.<String, String>builder()
                    .putAll(systemEnvironment)
                    .putAll(additionalEnvironment)
                    .build();

            String dockerHost = systemEnvironment.getOrDefault(DOCKER_HOST, "");
            return new DockerMachine(hostIp.resolveIp(dockerHost), environment);
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
            this.additionalEnvironment = newHashMap(firstNonNull(newEnvironment, newHashMap()));
            return this;
        }

        public DockerMachine build() {
            RemoteEnvironmentValidator.validate(dockerEnvironment);
            AdditionalEnvironmentValidator.validate(additionalEnvironment);

            String dockerHost = dockerEnvironment.getOrDefault(DOCKER_HOST, "");
            String hostIp = new RemoteHostIpResolver().resolveIp(dockerHost);

            Map<String, String> environment = ImmutableMap.<String, String>builder()
                                                          .putAll(dockerEnvironment)
                                                          .putAll(additionalEnvironment)
                                                          .build();
            return new DockerMachine(hostIp, environment);
        }

    }

}
