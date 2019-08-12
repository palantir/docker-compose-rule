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

package com.palantir.docker.compose.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class GitUtilsTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> params() {
        return Arrays.asList(new String[][] {
                { "git@github.com:palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                { "user@github.some.corp:palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                { "github.some.url:palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                { "github.some.url:3456/palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                { "ssh://user@github.some.url:3456/palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                { "ssh://github.some.url/palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                { "git@github.com:palantir/docker-compose-rule.git/", "palantir/docker-compose-rule" },
                { "ssh://user@github.some.url/~/palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                { "user@github.some.url/~user/palantir/docker-compose-rule.git", "palantir/docker-compose-rule" },
                });
    }

    private final String input;
    private final String expected;

    public GitUtilsTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void parse_out_git_remote_url() {
        assertThat(GitUtils.parsePathFromGitRemoteUrl(input)).isEqualTo(expected);
    }

}
