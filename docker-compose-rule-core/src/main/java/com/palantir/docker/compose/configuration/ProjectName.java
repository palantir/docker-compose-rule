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
package com.palantir.docker.compose.configuration;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@PackageVisible
public abstract class ProjectName {

    @Parameter
    protected abstract String projectName();

    @Check
    protected void validate() {
        checkState(projectName().trim().length() > 0, "ProjectName must not be blank.");

        checkState(validCharacters(),
                "ProjectName '%s' not allowed, please use lowercase letters and numbers only.", projectName());
    }

    // Only allows strings that docker-compose-cli would not modify
    // https://github.com/docker/compose/blob/85e2fb63b3309280a602f1f76d77d3a82e53b6c2/compose/cli/command.py#L84
    protected boolean validCharacters() {
        Predicate<String> illegalCharacters = Pattern.compile("[^a-z0-9]").asPredicate();
        return !illegalCharacters.test(projectName());
    }

    public String asString() {
        return projectName();
    }

    public List<String> constructComposeFileCommand() {
        return ImmutableList.of("--project-name", projectName());
    }

    public static ProjectName random() {
        return ImmutableProjectName.of(UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * A name consisting of lowercase letters and numbers only.
     */
    public static ProjectName fromString(String name) {
        return ImmutableProjectName.of(name);
    }
}
