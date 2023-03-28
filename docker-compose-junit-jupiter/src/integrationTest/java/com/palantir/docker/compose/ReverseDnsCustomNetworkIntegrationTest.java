/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ReverseDnsCustomNetworkIntegrationTest {
    @RegisterExtension
    public static final DockerComposeExtension docker = DockerComposeExtension.builder()
            .files(DockerComposeFiles.from("src/integrationTest/resources/reverse-dns-custom-network-compose.yaml"))
            .build();

    @Test
    public void container_reverse_dns() throws IOException, InterruptedException {
        System.out.println("Running first dig command");
        String ipAddress = docker.dockerCompose()
                .exec(
                        DockerComposeExecOption.noOptions(),
                        "db",
                        DockerComposeExecArgument.arguments("dig", "+short", "db2.palantir.pt", "A"));
        System.out.println(ipAddress);
        System.out.println("Running second dig command");
        String reverseHostName = docker.dockerCompose()
                .exec(
                        DockerComposeExecOption.noOptions(),
                        "db",
                        DockerComposeExecArgument.arguments("dig", "-x", ipAddress, "+short"));
        System.out.println(reverseHostName);

        System.out.println("Running first dig command on db2");
        String ipAddress2 = docker.dockerCompose()
                .exec(
                        DockerComposeExecOption.noOptions(),
                        "db2",
                        DockerComposeExecArgument.arguments("dig", "+short", "db.palantir.pt", "A"));
        System.out.println(ipAddress2);
        System.out.println("Running second dig command on db2");
        String reverseHostName2 = docker.dockerCompose()
                .exec(
                        DockerComposeExecOption.noOptions(),
                        "db2",
                        DockerComposeExecArgument.arguments("dig", "-x", ipAddress2, "+short"));
        System.out.println(reverseHostName2);

        assertThat(reverseHostName).isEqualTo("db2.palantir.pt.");
        assertThat(reverseHostName2).isEqualTo("db.palantir.pt.");
    }
}
