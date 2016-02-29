/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * THIS SOFTWARE CONTAINS PROPRIETARY AND CONFIDENTIAL INFORMATION OWNED BY PALANTIR TECHNOLOGIES INC.
 * UNAUTHORIZED DISCLOSURE TO ANY THIRD PARTY IS STRICTLY PROHIBITED
 *
 * For good and valuable consideration, the receipt and adequacy of which is acknowledged by Palantir and recipient
 * of this file ("Recipient"), the parties agree as follows:
 *
 * This file is being provided subject to the non-disclosure terms by and between Palantir and the Recipient.
 *
 * Palantir solely shall own and hereby retains all rights, title and interest in and to this software (including,
 * without limitation, all patent, copyright, trademark, trade secret and other intellectual property rights) and
 * all copies, modifications and derivative works thereof.  Recipient shall and hereby does irrevocably transfer and
 * assign to Palantir all right, title and interest it may have in the foregoing to Palantir and Palantir hereby
 * accepts such transfer. In using this software, Recipient acknowledges that no ownership rights are being conveyed
 * to Recipient.  This software shall only be used in conjunction with properly licensed Palantir products or
 * services.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
                                   .appendText(" did not contain " + contents);
            }

            @Override
            protected boolean matchesSafely() {
                try {
                    return FileUtils.readFileToString(value, StandardCharsets.UTF_8).contains(contents);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading log file", e);
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
