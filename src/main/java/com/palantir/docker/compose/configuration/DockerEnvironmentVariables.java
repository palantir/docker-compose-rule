package com.palantir.docker.compose.configuration;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

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
    private final Supplier<Map<String, String>> filteredEnvironmentVariables;

    public DockerEnvironmentVariables(Map<String, String> env) {
        this.env = ImmutableMap.copyOf(env);
        this.filteredEnvironmentVariables = Suppliers.memoize(this::generateDockerEnvironmentVariables);
        // TODO (fdesouza): a tad dubious about this check, is it ever true, especially on Mac
        if (env.getOrDefault(OS_NAME, "").startsWith(MAC_OS)) {
            checkEnvVariables();
        }
    }

    public void checkEnvVariables() {
         List<String> missingEnvironmentVariables = getMissingEnvVariables();

        if (!missingEnvironmentVariables.isEmpty()) {
            throw new IllegalStateException("Missing required environment variables: '" + missingEnvironmentVariables
                    + "', please run `docker-machine env <machine-name>` and update your IDE run configuration with"
                    + " the variables listed.");
        }
    }

    private List<String> getMissingEnvVariables() {
        List<String> missingEnvironmentVariables = requiredEnvVariables.stream()
                .filter(envVariable -> Strings.isNullOrEmpty(env.getOrDefault(envVariable, "")))
                .collect(Collectors.toList());

        if (certVerificationDisabled()) {
            missingEnvironmentVariables = enforceNoCertPath(missingEnvironmentVariables);
        }

        return missingEnvironmentVariables;
    }

    private List<String> enforceNoCertPath(List<String> missingEnvironmentVariables) {
        List<String> missingNonCertVariables = Lists.newArrayList(missingEnvironmentVariables);

        boolean missingCertPath = missingEnvironmentVariables.contains(DOCKER_CERT_PATH);
        if (missingCertPath) {
            missingNonCertVariables.remove(DOCKER_TLS_VERIFY);
            missingNonCertVariables.remove(DOCKER_CERT_PATH);
            return missingNonCertVariables;
        } else {
            throw new IllegalStateException(CERT_PATH_PRESENT_BUT_TLS_VERIFY_DISABLED);
        }
    }

    private boolean certVerificationDisabled() {
        return Integer.parseInt(env.getOrDefault(DOCKER_TLS_VERIFY, "0")) == DISABLE_CERT_VERIFICATION;
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

    public Map<String, String> getDockerEnvironmentVariables() {
        return filteredEnvironmentVariables.get();
    }

    private Map<String, String> generateDockerEnvironmentVariables() {
        Map<String, String> filteredVariables = requiredEnvVariables.stream()
                                                                    .filter(envVariable -> isNotBlank(env.getOrDefault(envVariable, "")))
                                                                    .collect(toMap(identity(), env::get));
        return ImmutableMap.copyOf(filteredVariables);
    }

}
