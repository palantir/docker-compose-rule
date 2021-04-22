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
package com.palantir.docker.compose;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerExecutionException;
import com.palantir.docker.compose.logging.LogCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DockerComposeRuleShould {

    private static final String IP = "127.0.0.1";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private DockerComposeFiles mockFiles = mock(DockerComposeFiles.class);
    private DockerMachine machine = mock(DockerMachine.class);
    private LogCollector logCollector = mock(LogCollector.class);
    private DockerComposeRule rule;

    @Before
    public void before() {
        when(machine.getIp()).thenReturn(IP);
        rule = defaultBuilder().build();
    }

    private DockerComposeRule.Builder defaultBuilder() {
        return DockerComposeRule.builder()
                .dockerCompose(dockerCompose)
                .files(mockFiles)
                .machine(machine)
                .logCollector(logCollector);
    }

    @Test
    @SuppressWarnings("IllegalThrows")
    public void collects_log_when_startup_fails() throws Throwable {
        Statement statement = mock(Statement.class);
        Description description = mock(Description.class);

        doThrow(new DockerExecutionException("")).when(dockerCompose).up();
        rule = defaultBuilder().build();

        try {
            exception.expect(DockerExecutionException.class);
            rule.apply(statement, description).evaluate();
        } finally {
            verify(logCollector, times(1)).collectLogs(dockerCompose);
        }
    }
}
