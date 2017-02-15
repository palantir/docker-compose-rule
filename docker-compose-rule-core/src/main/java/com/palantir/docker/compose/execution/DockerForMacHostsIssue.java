/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;

/**
 * Check whether Mac OS X users have pointed localunixsocket to localhost.
 *
 * <p>docker-compose takes an order of magnitude longer to run commands without this tip!
 *
 * @see <a href="https://github.com/docker/compose/issues/3419#issuecomment-221793401">Docker Compose Issue #3419</a>
 */
public class DockerForMacHostsIssue {

    private static final String REDIRECT_LINE = "127.0.0.1 localunixsocket\n";
    private static final String WARNING_MESSAGE = "\n\n **** WARNING: Your tests may be slow ****\n"
            + "Please add the following line to /etc/hosts:\n    "
            + REDIRECT_LINE
            + "\nFor more information, see https://github.com/docker/compose/issues/3419#issuecomment-221793401\n\n";
    private static volatile boolean checkPerformed = false;

    public static void issueWarning() {
        if (!checkPerformed) {
            if (onMacOsX() && !localunixsocketRedirectedInEtcHosts()) {
                System.err.print(WARNING_MESSAGE);
            }
        }
        checkPerformed = true;
    }

    private static boolean onMacOsX() {
        return System.getProperty("os.name", "generic").equals("Mac OS X");
    }

    private static boolean localunixsocketRedirectedInEtcHosts() {
        try {
            return Files.toString(new File("/etc/hosts"), UTF_8).contains(REDIRECT_LINE);
        } catch (IOException e) {
            return true;  // Better to be silent than issue false warnings
        }
    }

    public static void main(String[] args) {
        issueWarning();
    }

    private DockerForMacHostsIssue() {}
}
