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

import com.google.common.collect.ImmutableList;
import java.util.regex.Pattern;
import org.junit.Test;

public class PatternCollectionTest {
    private final PatternCollection patternCollection =
            new PatternCollection(ImmutableList.of(Pattern.compile("foo"), Pattern.compile("bar")));

    @Test
    public void matching_one_pattern() {
        assertThat(patternCollection.anyMatch("afoolol")).isTrue();
    }

    @Test
    public void matching_two_patterns() {
        assertThat(patternCollection.anyMatch("foobar")).isTrue();
    }

    @Test
    public void matching_zero_patterns() {
        assertThat(patternCollection.anyMatch("bbbbb")).isFalse();
    }
}
