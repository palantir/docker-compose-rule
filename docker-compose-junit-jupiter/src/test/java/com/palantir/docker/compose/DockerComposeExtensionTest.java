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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.events.EventConsumer;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

public class DockerComposeExtensionTest {

    @Test
    public void calls_after_only_once() throws IOException, InterruptedException {
        AtomicInteger count = new AtomicInteger();
        DockerComposeExtension dockerComposeExtension = new DockerComposeExtension() {

            @Override
            public void before() {
                throw new IllegalStateException("some error");
            }

            @Override
            public void after() {
                count.incrementAndGet();
            }

            @Override
            public DockerComposeFiles files() {
                return null;
            }

            @Override
            protected List<ClusterWait> clusterWaits() {
                return null;
            }

            @Override
            protected List<EventConsumer> eventConsumers() {
                return null;
            }
        };

        ExtensionContext extensionContext = Mockito.mock(ExtensionContext.class);
        assertThatThrownBy(() -> dockerComposeExtension.beforeAll(extensionContext))
                .isInstanceOf(IllegalStateException.class);
        dockerComposeExtension.afterAll(extensionContext);
        assertThat(count.get()).isEqualTo(1);
    }
}
