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

package com.palantir.docker.compose.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.zafarkhaja.semver.Version;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class VersionHelperShould {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void satisfactory_version_core_remains_unchanged() {
        String version = "1.2.3";
        assertThat(VersionHelper.toSemVer(version), is(Version.valueOf("1.2.3")));
    }

    @Test
    public void satisfactory_full_version_remains_unchanged() {
        String version = "1.2.3-rc4+build5";
        Version.Builder builder = new Version.Builder("1.2.3");
        builder.setPreReleaseVersion("rc4");
        builder.setBuildMetadata("build5");

        Version expectedVersion = builder.build();
        assertThat(VersionHelper.toSemVer(version), is(expectedVersion));
    }

    @Test
    public void leading_zeroes_are_removed_from_version_core_and_rest_untouched() {
        String version = "01.02.03-rc4+build5";
        Version.Builder builder = new Version.Builder("1.2.3");
        builder.setPreReleaseVersion("rc4");
        builder.setBuildMetadata("build5");

        Version expectedVersion = builder.build();
        assertThat(VersionHelper.toSemVer(version), is(expectedVersion));
    }

    @Test
    public void unexpected_version_structure_throws_exception() {
        String version = "totally_a_version";
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Unexpected version structure: totally_a_version");
        VersionHelper.toSemVer(version);
    }
}
