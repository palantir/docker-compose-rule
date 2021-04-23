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

import com.google.common.collect.Streams;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class GitUtils {
    private static final Pattern SSH_REGEX = Pattern.compile(sshRegex());
    private static final Pattern JUST_A_PATH = Pattern.compile("[\\w/]+", Pattern.UNICODE_CHARACTER_CLASS);

    private GitUtils() {}

    public static Optional<String> parsePathFromGitRemoteUrl(String gitRemoteUrl) {
        if (gitRemoteUrl.startsWith("file://")
                || gitRemoteUrl.startsWith("/")
                || JUST_A_PATH.matcher(gitRemoteUrl).matches()) {
            return Optional.empty();
        }

        return Stream.of(parseHttp(gitRemoteUrl), parseSshOrGit(gitRemoteUrl))
                .flatMap(Streams::stream)
                .map(path -> path.replaceAll("(\\.git)?/?$", ""))
                .map(path -> path.replaceAll("^/", ""))
                .findFirst();
    }

    private static Optional<String> parseSshOrGit(String gitRemoteUrl) {
        Matcher matcher = SSH_REGEX.matcher(gitRemoteUrl);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(matcher.group(1));
    }

    private static Optional<String> parseHttp(String gitRemoteUrl) {
        try {
            URL url = new URL(gitRemoteUrl);
            return Optional.of(url.getPath());
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    private static String sshRegex() {
        String sshOrGit = "(?:(?:ssh|git)://)?";
        String user = "(?:.+@)?";
        String hostname = ".*?";
        String separator = "[:/]";
        String port = "(?:\\d+/)?";
        String squigglyUser = "(?:~[^/]*/)?";
        String pathCapture = "(.*)";

        return sshOrGit + user + hostname + separator + port + squigglyUser + pathCapture;
    }
}
