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
package com.palantir.docker.compose.execution;

import com.google.common.collect.Lists;
import com.palantir.docker.compose.connection.DockerMachine;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.joda.time.Duration.standardMinutes;


public class DockerComposeExecutor {

    private static final List<String> dockerComposeLocations = asList("/usr/local/bin/docker-compose",
                                                                      System.getenv("DOCKER_COMPOSE_LOCATION"));
    public static final Duration COMMAND_TIMEOUT = standardMinutes(2);

    private final File dockerComposeFile;
    private final DockerMachine dockerMachine;

    public DockerComposeExecutor(File dockerComposeFile, DockerMachine dockerMachine) {
        this.dockerComposeFile = dockerComposeFile;
        this.dockerMachine = dockerMachine;
    }

    public Process executeAndWait(String... commands) throws IOException, InterruptedException {
        Process dockerCompose = execute(commands);
        dockerCompose.waitFor(COMMAND_TIMEOUT.getMillis(), MILLISECONDS);
        return dockerCompose;
    }

    public Process execute(String... commands) throws IOException {
        List<String> args = Lists.newArrayList(getDockerComposePath(), "-f", dockerComposeFile.getAbsolutePath());
        Collections.addAll(args, commands);
        return dockerMachine.configDockerComposeProcess()
                            .command(args)
                            .redirectErrorStream(true)
                            .start();
    }

    private static String getDockerComposePath() {
        return dockerComposeLocations.stream()
                                     .filter(StringUtils::isNotBlank)
                                     .filter(path -> new File(path).exists())
                                     .findAny()
                                     .orElseThrow(() -> new IllegalStateException("Could not find docker-compose, looked in: " + dockerComposeLocations));
    }

}
