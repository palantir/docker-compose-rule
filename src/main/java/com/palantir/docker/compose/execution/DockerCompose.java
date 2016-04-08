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
package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.Ports;

import java.io.IOException;
import java.io.OutputStream;

public interface DockerCompose {
    void build() throws IOException, InterruptedException;
    void up() throws IOException, InterruptedException;
    void down() throws IOException, InterruptedException;
    void kill() throws IOException, InterruptedException;
    void rm() throws IOException, InterruptedException;
    ContainerNames ps() throws IOException, InterruptedException;
    Container container(String containerName);
    boolean writeLogs(String container, OutputStream output) throws IOException;
    Ports ports(String service) throws IOException, InterruptedException;
}
