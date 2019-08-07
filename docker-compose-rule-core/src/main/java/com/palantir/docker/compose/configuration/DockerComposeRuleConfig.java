/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.palantir.docker.compose.reporting.ReportingConfig;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import one.util.streamex.StreamEx;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DockerComposeRuleConfig {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
    public static final String CONFIG_FILENAME = ".docker-compose-rule.yml";

    public abstract Optional<ReportingConfig> reporting();

    public static class Builder extends ImmutableDockerComposeRuleConfig.Builder {}

    public static Builder builder() {
        return new Builder();
    }

    public static Optional<DockerComposeRuleConfig> findAutomatically() {
        File currentDir = new File(".").getAbsoluteFile();
        Optional<File> configFile = dirAndParents(currentDir)
                .map(dir -> new File(dir, CONFIG_FILENAME))
                .findFirst(File::exists);

        return configFile.map(config -> {
            try {
                return OBJECT_MAPPER.readValue(config, DockerComposeRuleConfig.class);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't deserialize config file", e);
            }
        });
    }

    private static StreamEx<File> dirAndParents(File startDir) {
        return StreamEx.of(Stream.generate(new Supplier<Optional<File>>() {
            private Optional<File> dir = Optional.ofNullable(startDir);

            @Override
            public Optional<File> get() {
                Optional<File> toReturn = dir;
                if (dir.isPresent()) {
                    dir = Optional.ofNullable(dir.get().getParentFile());
                }
                return toReturn;
            }
        }))
                .takeWhile(Optional::isPresent)
                .map(Optional::get);
    }
}
