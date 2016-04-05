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

package com.palantir.docker.compose;

import com.google.common.base.Preconditions;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;

import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;

@FunctionalInterface
public interface MultiHealthCheck {
    static MultiHealthCheck fromHealthCheck(HealthCheck healthCheck) {
        return containers -> {
            Preconditions.checkArgument(containers.size() == 1, "Trying to run a single container health check on containers " + containers);
            return healthCheck.isServiceUp(getOnlyElement(containers));
        };
    }

    SuccessOrFailure areServicesUp(List<Container> containers);
}
