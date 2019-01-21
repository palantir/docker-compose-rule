/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose.connection;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.configuration.AdditionalEnvironmentValidator;
import com.palantir.docker.compose.configuration.DockerType;
import com.palantir.docker.compose.configuration.RemoteHostIpResolver;
import com.palantir.docker.compose.execution.DockerConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerMachine implements DockerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DockerMachine.class);
    private static final DockerType FALLBACK_DOCKER_TYPE = DockerType.DAEMON;

    private final String hostIp;
    private final Map<String, String> environment;

    public DockerMachine(String hostIp, Map<String, String> environment) {
        this.hostIp = hostIp;
        this.environment = environment;
    }

    public String getIp() {
        return hostIp;
    }

    @Override
    public ProcessBuilder configuredDockerComposeProcess() {
        ProcessBuilder process = new ProcessBuilder();
        augmentGivenEnvironment(process.environment());
        return process;
    }

    private void augmentGivenEnvironment(Map<String, String> environmentToAugment) {
        environmentToAugment.putAll(environment);
    }

    public static LocalBuilder localMachine() {
        Map<String, String> systemEnv = System.getenv();
        Optional<DockerType> dockerType = DockerType.getFirstValidDockerTypeForEnvironment(systemEnv);
        if (!dockerType.isPresent()) {
            log.debug(
                    "Failed to determine Docker type (daemon or remote) based on current environment. "
                            + "Proceeding with {} as the type.", FALLBACK_DOCKER_TYPE);
        }

        return new LocalBuilder(dockerType.orElse(FALLBACK_DOCKER_TYPE), systemEnv);
    }

    public static LocalBuilder localMachine(DockerType dockerType) {
        return new LocalBuilder(dockerType, System.getenv());
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
            dockerType.validateEnvironmentVariables(systemEnvironment);
            AdditionalEnvironmentValidator.validate(additionalEnvironment);
            Map<String, String> combinedEnvironment = newHashMap();
            combinedEnvironment.putAll(systemEnvironment);
            combinedEnvironment.putAll(additionalEnvironment);

            String dockerHost = systemEnvironment.getOrDefault(DOCKER_HOST, "");
            return new DockerMachine(dockerType.resolveIp(dockerHost), ImmutableMap.copyOf(combinedEnvironment));
        }
    }

    public static RemoteBuilder remoteMachine() {
        return new RemoteBuilder();
    }

    public static class RemoteBuilder {

        private final Map<String, String> dockerEnvironment = newHashMap();
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
            DockerType.REMOTE.validateEnvironmentVariables(dockerEnvironment);
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
