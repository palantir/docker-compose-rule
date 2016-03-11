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
package com.palantir.docker.compose.matchers;

import com.google.common.base.MoreObjects;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

public class IOMatchers {
    private IOMatchers() {
    }

    public static Matcher<File> hasFiles(int numberOfFiles) {
        return new ValueCachingMatcher<File>() {
            private String[] files = new String[0];

            @Override
            public void describeTo(Description description) {
                description.appendText("directory ")
                           .appendValue(value)
                           .appendText(" to have " + numberOfFiles + " files");
            }

            @Override
            protected void describeMismatchSafely(File item, Description mismatchDescription) {
                mismatchDescription.appendText("directory ")
                                   .appendValue(item)
                                   .appendText(" had " + files.length + " files ")
                                   .appendText(Arrays.toString(files))
                                   .appendText(" or is not a directory");
            }

            @Override
            protected boolean matchesSafely() {
                files = MoreObjects.firstNonNull(value.list(), new String[0]);
                return files.length == numberOfFiles;
            }
        };
    }

    public static Matcher<File> fileWithName(String filename) {
        return new ValueCachingMatcher<File>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("file with name " + filename);
            }

            @Override
            protected void describeMismatchSafely(File item, Description mismatchDescription) {
                mismatchDescription.appendText("file ")
                                   .appendValue(item)
                                   .appendText(" did not have name " + filename);
            }

            @Override
            protected boolean matchesSafely() {
                return value.getName().equals(filename);
            }
        };
    }

    public static Matcher<File> fileContainingString(String contents) {
        return new ValueCachingMatcher<File>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("file ")
                           .appendValue(value)
                           .appendText(" to contain " + contents);
            }

            @Override
            protected void describeMismatchSafely(File item, Description mismatchDescription) {
                mismatchDescription.appendText("file ")
                                   .appendValue(item)
                                   .appendText(" contained " + safelyReadFile());
            }

            @Override
            protected boolean matchesSafely() {
                return safelyReadFile().contains(contents);
            }

            private String safelyReadFile() {
                try {
                    return FileUtils.readFileToString(value, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading file", e);
                }
            }
        };
    }

    public static Matcher<File> fileExists() {
        return new ValueCachingMatcher<File>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("file ")
                           .appendValue(value)
                           .appendText(" to exist");
            }

            @Override
            protected void describeMismatchSafely(File item, Description mismatchDescription) {
                mismatchDescription.appendText("file ")
                                   .appendValue(item.getAbsolutePath())
                                   .appendText(" did not exist");
            }

            @Override
            protected boolean matchesSafely() {
                return value.exists();
            }
        };
    }

    public static Matcher<File> isDirectory() {
        return new ValueCachingMatcher<File>() {
            @Override
            public void describeTo(Description description) {
                description.appendValue(value)
                           .appendText(" is directory");
            }

            @Override
            protected void describeMismatchSafely(File item, Description mismatchDescription) {
                mismatchDescription.appendValue(item.getAbsolutePath())
                                   .appendText(" is not a directory");
            }

            @Override
            protected boolean matchesSafely() {
                return value.isDirectory();
            }
        };
    }

    public static Matcher<Path> pathFileExists() {
        return new ValueCachingMatcher<Path>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("file ")
                           .appendValue(value)
                           .appendText(" to exist");
            }

            @Override
            protected void describeMismatchSafely(Path item, Description mismatchDescription) {
                mismatchDescription.appendText("file ")
                                   .appendValue(item)
                                   .appendText(" did not exist");
            }

            @Override
            protected boolean matchesSafely() {
                return value.toFile().exists();
            }
        };
    }
}
