/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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
package com.palantir.docker.compose.configuration;

import com.google.common.base.Optional;
import java.util.Map;

public enum DockerType implements HostIpResolver, EnvironmentValidator {
    DAEMON(DaemonEnvironmentValidator.instance(), new DaemonHostIpResolver()),
    REMOTE(RemoteEnvironmentValidator.instance(), new RemoteHostIpResolver());

    private final EnvironmentValidator validator;
    private final HostIpResolver resolver;

    DockerType(EnvironmentValidator validator, HostIpResolver resolver) {
        this.validator = validator;
        this.resolver = resolver;
    }

    @Override
    public Map<String, String> validate(Map<String, String> dockerEnvironment) {
        return validator.validate(dockerEnvironment);
    }

    @Override
    public String resolveIp(String dockerHost) {
        return resolver.resolveIp(dockerHost);
    }

    public static Optional<DockerType> getFirstValidDockerTypeForEnvironment(Map<String, String> environment) {
        for (DockerType currType : DockerType.values()) {
            try {
                currType.validate(environment);
                return Optional.of(currType);
            } catch (IllegalStateException e) {
                // ignore and try next type
            }
        }
        return Optional.absent();
    }

}
