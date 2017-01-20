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
package com.palantir.docker.compose.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StateShould {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void parse_actual_docker_compose_output_when_state_is_Up() throws IOException, InterruptedException {
        String psOutput =
                "       Name                      Command               State                                         Ports                                        \n"
                        + "-------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "magritte_magritte_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:7000->7000/tcp, 7001/tcp, 7002/tcp, 7003/tcp, 7004/tcp, 7005/tcp, 7006/tcp \n"
                        + "";
        State state = State.parseFromDockerComposePs(psOutput);
        assertThat(state, is(State.Up));
    }

    @Test
    public void parse_actual_docker_compose_output_when_state_is_Exit() throws IOException, InterruptedException {
        String psOutput =
                "       Name                      Command               State                                         Ports                                        \n"
                        + "-------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "magritte_magritte_1   /bin/sh -c /usr/local/bin/ ...   Exit      0.0.0.0:7000->7000/tcp, 7001/tcp, 7002/tcp, 7003/tcp, 7004/tcp, 7005/tcp, 7006/tcp \n"
                        + "";
        State state = State.parseFromDockerComposePs(psOutput);
        assertThat(state, is(State.Exit));
    }

    @Test
    public void throw_on_unknown_state() throws IOException, InterruptedException {
        String psOutput =
                "       Name                      Command               State                                         Ports                                        \n"
                        + "-------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "magritte_magritte_1   /bin/sh -c /usr/local/bin/ ...   WhatIsThis      0.0.0.0:7000->7000/tcp, 7001/tcp, 7002/tcp, 7003/tcp, 7004/tcp, 7005/tcp, 7006/tcp \n"
                        + "";
        exception.expect(IllegalStateException.class);
        State.parseFromDockerComposePs(psOutput);
    }
}
