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
package com.palantir.docker.compose.execution;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConflictingContainerRemovingDockerComposeShould {
    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final Docker docker = mock(Docker.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void require_retry_attempts_to_be_at_least_1() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("retryAttempts must be at least 1, was 0");
        new ConflictingContainerRemovingDockerCompose(dockerCompose, docker, 0);
    }

    @Test
    public void call_up_only_once_if_successful() throws IOException, InterruptedException {
        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker);
        conflictingContainerRemovingDockerCompose.up();

        verify(dockerCompose, times(1)).up();
        verifyZeroInteractions(docker);
    }

    @Test
    public void call_rm_and_retry_up_if_conflicting_containers_exist() throws IOException, InterruptedException {
        String conflictingContainer = "conflictingContainer";
        doThrow(new DockerExecutionException("The name \"" + conflictingContainer + "\" is already in use"))
                .doNothing()
                .when(dockerCompose)
                .up();

        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker);
        conflictingContainerRemovingDockerCompose.up();

        verify(dockerCompose, times(2)).up();
        verify(docker).rm(ImmutableSet.of(conflictingContainer));
    }

    @Test
    public void retry_specified_number_of_times() throws IOException, InterruptedException {
        String conflictingContainer = "conflictingContainer";
        DockerExecutionException dockerException =
                new DockerExecutionException("The name \"" + conflictingContainer + "\" is already in use");
        doThrow(dockerException)
                .doThrow(dockerException)
                .doNothing()
                .when(dockerCompose)
                .up();

        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker, 3);
        conflictingContainerRemovingDockerCompose.up();

        verify(dockerCompose, times(3)).up();
        verify(docker, times(2)).rm(ImmutableSet.of(conflictingContainer));
    }

    @Test
    public void ignore_docker_execution_exceptions_in_rm() throws IOException, InterruptedException {
        String conflictingContainer = "conflictingContainer";
        doThrow(new DockerExecutionException("The name \"" + conflictingContainer + "\" is already in use"))
                .doNothing()
                .when(dockerCompose)
                .up();
        doThrow(DockerExecutionException.class).when(docker).rm(anySetOf(String.class));

        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker);
        conflictingContainerRemovingDockerCompose.up();

        verify(dockerCompose, times(2)).up();
        verify(docker).rm(ImmutableSet.of(conflictingContainer));
    }

    @Test
    public void fail_on_non_docker_execution_exceptions_in_rm() throws IOException, InterruptedException {
        String conflictingContainer = "conflictingContainer";
        doThrow(new DockerExecutionException("The name \"" + conflictingContainer + "\" is already in use"))
                .doNothing()
                .when(dockerCompose)
                .up();
        doThrow(RuntimeException.class).when(docker).rm(anySetOf(String.class));

        exception.expect(RuntimeException.class);
        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker);
        conflictingContainerRemovingDockerCompose.up();
    }

    @Test
    public void throw_exception_if_retry_attempts_exceeded() throws IOException, InterruptedException {
        String conflictingContainer = "conflictingContainer";
        doThrow(new DockerExecutionException("The name \"" + conflictingContainer + "\" is already in use"))
                .when(dockerCompose)
                .up();

        exception.expect(DockerExecutionException.class);
        exception.expectMessage("docker-compose up failed");
        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker);
        conflictingContainerRemovingDockerCompose.up();
    }

    @Test
    public void parse_container_names_from_error_message() {
        String conflictingContainer = "conflictingContainer";

        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker);
        Set<String> conflictingContainerNames = conflictingContainerRemovingDockerCompose.getConflictingContainerNames(
                "The name \"" + conflictingContainer + "\" is already in use");

        assertEquals(ImmutableSet.of(conflictingContainer), conflictingContainerNames);
    }

    @Test
    public void parse_container_names_from_error_message_since_v13() {
        String conflictingContainer = "conflictingContainer";

        ConflictingContainerRemovingDockerCompose conflictingContainerRemovingDockerCompose =
                new ConflictingContainerRemovingDockerCompose(dockerCompose, docker);
        Set<String> conflictingContainerNames = conflictingContainerRemovingDockerCompose.getConflictingContainerNames(
                "The container name \"" + conflictingContainer + "\" is already in use");

        assertEquals(ImmutableSet.of(conflictingContainer), conflictingContainerNames);
    }
}
