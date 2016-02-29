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
package com.palantir.docker.compose.configuration;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import static java.util.stream.Collectors.joining;

public class RemoteEnvironmentValidator {

    private static final Set<String> SECURE_VARIABLES = ImmutableSet.of(DOCKER_TLS_VERIFY, DOCKER_CERT_PATH);

    private final Map<String, String> dockerEnvironment;

    public RemoteEnvironmentValidator(Map<String, String> dockerEnvironment) {
        this.dockerEnvironment = dockerEnvironment;
    }

    public Map<String, String> validate() {
        Collection<String> missingVariables = getMissingEnvVariables();
        String errorMessage = missingVariables.stream()
                                              .collect(joining(", ",
                                                               "Missing required environment variables: ",
                                                               ". Please run `docker-machine env <machine-name>` and "
                                                                       + "ensure they are set on the DockerComposition."));

        Preconditions.checkState(missingVariables.isEmpty(), errorMessage);
        return dockerEnvironment;
    }

    private Collection<String> getMissingEnvVariables() {
        Collection<String> requiredVariables = Sets.union(newHashSet(DOCKER_HOST), secureVariablesRequired());
        return requiredVariables.stream()
                                .filter(envVariable -> Strings.isNullOrEmpty(dockerEnvironment.get(envVariable)))
                                .collect(Collectors.toSet());
    }

    private Set<String> secureVariablesRequired() {
        return certVerificationEnabled() ? SECURE_VARIABLES : newHashSet();
    }

    private boolean certVerificationEnabled() {
        return dockerEnvironment.containsKey(DOCKER_TLS_VERIFY);
    }

    public static Map<String, String> validate(Map<String, String> dockerEnvironment) {
        return new RemoteEnvironmentValidator(dockerEnvironment).validate();
    }

}
