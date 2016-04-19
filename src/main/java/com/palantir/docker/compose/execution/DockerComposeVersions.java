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
package com.palantir.docker.compose.execution;

public class DockerComposeVersions {

    //copied from http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
    /**
     * Compares two version strings.
     *
     * Use this instead of String.com1pareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * It does not work if "1.10" is supposed to be equal to "1.10.0".
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     *         The result is a positive integer if str1 is _numerically_ greater than str2.
     *         The result is zero if the strings are _numerically_ equal.
     */
    public static Integer versionCompare(String str1, String str2)
    {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i]))
        {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length)
        {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else
        {
            return Integer.signum(vals1.length - vals2.length);
        }
    }

    //Current docker-compose version output format: docker-compose version 1.7.0rc1, build 1ad8866
    public static String parseFromDockerComposeVersion(String versionOutput) {
        String[] splitOnSeparator = versionOutput.split(" ");
        String version = splitOnSeparator[2];
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < version.length(); i ++) {
            if (version.charAt(i) >= '0' && version.charAt(i) <= '9' || version.charAt(i) == '.') {
                builder.append(version.charAt(i));
            } else {
                return builder.toString();
            }
        }
        return builder.toString();
    }
}
