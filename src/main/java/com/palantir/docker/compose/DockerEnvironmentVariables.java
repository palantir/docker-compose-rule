/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class DockerEnvironmentVariables {
    private static final int DISABLE_CERT_VERIFICATION = 0;

    private static final String LOCALHOST = "127.0.0.1";
    private static final String OS_NAME = "os.name";
    private static final String MAC_OS = "Mac";

    public static final String TCP_PROTOCOL = "tcp://";
    public static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";
    public static final String DOCKER_HOST = "DOCKER_HOST";
    public static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";

    public static final String CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED =
            "Docker still attempts to use " + DOCKER_CERT_PATH + " to verify a "
                    + "connection even when " + DOCKER_TLS_VERIFY + " is set to 0 (or is missing). "
                    + "Please enable " + DOCKER_TLS_VERIFY + " or unset " + DOCKER_CERT_PATH;

    private static final List<String> requiredEnvVariables = ImmutableList.of(
            DOCKER_TLS_VERIFY,
            DOCKER_HOST,
            DOCKER_CERT_PATH);

    private final Map<String, String> env;

    public DockerEnvironmentVariables(Map<String, String> env) {
        this.env = Maps.newHashMap(env);

        if (env.getOrDefault(OS_NAME, "").startsWith(MAC_OS)) {
            checkEnvVariables();
        }
    }

    public void checkEnvVariables() {
        List<String> missingEnvironmentVariables = requiredEnvVariables.stream()
                .filter(envVariable -> Strings.isNullOrEmpty(env.getOrDefault(envVariable, "")))
                .collect(Collectors.toList());

        boolean missingCertPath = missingEnvironmentVariables.contains(DOCKER_CERT_PATH);
        boolean disabledCertVerification =
                Integer.parseInt(env.getOrDefault(
                        DOCKER_TLS_VERIFY, "0")) == DISABLE_CERT_VERIFICATION;

        // Allow/Ensure that the DOCKER_CERT_PATH env variable is not set if DOCKER_TLS_VERIFY is missing or set to 0.
        if (disabledCertVerification) {
            if (missingCertPath) {
                missingEnvironmentVariables.remove(DOCKER_TLS_VERIFY);
                missingEnvironmentVariables.remove(DOCKER_CERT_PATH);
            } else {
                throw new IllegalStateException(CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
            }
        }

        if (!missingEnvironmentVariables.isEmpty()) {
            throw new IllegalStateException("Missing required environment variables: '" + missingEnvironmentVariables
                    + "', please run `docker-machine env <machine-name>` and update your IDE run configuration with"
                    + " the variables listed.");
        }
    }

    public String getDockerHostIp() {
        String dockerHostEnvVariable = env.getOrDefault(DOCKER_HOST, "");

        if (!Strings.isNullOrEmpty(dockerHostEnvVariable)) {
            // StringUtils.substringBetween unfortunately returns null if any of the separators does not match
            String ipAndMaybePort = StringUtils.substringAfter(dockerHostEnvVariable, TCP_PROTOCOL);
            return StringUtils.substringBefore(ipAndMaybePort, ":");
        }

        return LOCALHOST;
    }
}
