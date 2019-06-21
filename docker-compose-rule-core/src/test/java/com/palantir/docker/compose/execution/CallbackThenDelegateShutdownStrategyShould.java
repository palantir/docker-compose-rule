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

package com.palantir.docker.compose.execution;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import org.junit.Test;
import org.mockito.InOrder;

public class CallbackThenDelegateShutdownStrategyShould {

    @Test
    public void call_callback_then_call_delegate() throws Exception {
        ShutdownStrategy delegate = mock(ShutdownStrategy.class);
        Runnable callback = mock(Runnable.class);

        DockerCompose dockerCompose = mock(DockerCompose.class);
        Docker docker = mock(Docker.class);

        ShutdownStrategy.callbackAndThen(callback, delegate).shutdown(dockerCompose, docker);

        InOrder inOrder = inOrder(callback, delegate);
        inOrder.verify(callback).run();
        inOrder.verify(delegate).shutdown(dockerCompose, docker);
        inOrder.verifyNoMoreInteractions();
    }
}
