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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ProjectNameTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void construct_compose_command_should_use_project_name_prefix() {
        List<String> command = ProjectName.random().constructComposeFileCommand();

        assertThat(command, hasSize(2));
        assertThat(command.get(0), is("--project-name"));
    }

    @Test
    public void successive_calls_to_random_should_produce_different_names() {
        List<String> firstCommand = ProjectName.random().constructComposeFileCommand();
        List<String> secondCommand = ProjectName.random().constructComposeFileCommand();

        assertThat(firstCommand, is(not(equalTo(secondCommand))));
    }

    @Test
    public void random_should_be_eight_characters_long() {
        String randomName = ProjectName.random().constructComposeFileCommand().get(1);
        assertThat(randomName.length(), is(8));
    }

    @Test
    public void from_string_factory_should_pass_name_to_command() {
        List<String> command = ProjectName.fromString("projectname").constructComposeFileCommand();
        assertThat(command, contains("--project-name", "projectname"));
    }

    @Test
    public void from_string_factory_should_disallow_names() {
        List<String> command = ProjectName.fromString("projectname").constructComposeFileCommand();
        assertThat(command, contains("--project-name", "projectname"));
    }

    @Test
    public void from_string_should_reject_blanks() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("ProjectName must not be blank.");
        ProjectName.fromString(" ");
    }

    @Test
    public void validation_should_match_docker_compose_cli() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("ProjectName 'Crazy#Proj ect!Name' not allowed, please use lowercase letters and numbers only.");
        ProjectName.fromString("Crazy#Proj ect!Name");
    }
}

