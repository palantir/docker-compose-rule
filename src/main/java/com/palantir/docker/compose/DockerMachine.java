package com.palantir.docker.compose;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

public class DockerMachine {

    public static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";
    public static final String DOCKER_HOST = "DOCKER_HOST";
    public static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";
    private static final int DISABLE_CERT_VERIFICATION = 0;

    private static final List<String> requiredEnvVariables = ImmutableList.of(
            DOCKER_TLS_VERIFY,
            DOCKER_HOST,
            DOCKER_CERT_PATH);

    private final String ip;

    public DockerMachine(String ip) {
        this.ip = ip;
    }

    public static DockerMachine fromEnvironment() {
        if (System.getProperty("os.name").startsWith("Mac")) {
            checkEnvVariables(System.getenv());
        }

        return new DockerMachine(getDockerHostIp());
    }

    protected static void checkEnvVariables(Map<String, String> env) {
        List<String> missingEnvironmentVariables = requiredEnvVariables.stream()
                .filter(envVariable -> isNullOrEmpty(env.getOrDefault(envVariable, "")))
                .collect(toList());

        boolean missingCertPath = missingEnvironmentVariables.contains(DOCKER_CERT_PATH);
        boolean disabledCertVerification =
                Integer.parseInt(env.getOrDefault(DOCKER_TLS_VERIFY, "0")) == DISABLE_CERT_VERIFICATION;

        // Allow/Ensure that the DOCKER_CERT_PATH env variable is not set if DOCKER_TLS_VERIFY is missing or set to 0.
        if (disabledCertVerification) {
            if (missingCertPath) {
                missingEnvironmentVariables.remove(DOCKER_TLS_VERIFY);
                missingEnvironmentVariables.remove(DOCKER_CERT_PATH);
            } else {
                throw new IllegalStateException("Docker still attempts to use " + DOCKER_CERT_PATH + " to verify a "
                        + "connection even when " + DOCKER_TLS_VERIFY + " is set to 0 (or is missing). "
                        + "Please enable " + DOCKER_TLS_VERIFY + " or unset " + DOCKER_CERT_PATH);
            }
        }

        if (!missingEnvironmentVariables.isEmpty()) {
            throw new IllegalStateException("Missing required environment variables: '" + missingEnvironmentVariables
                    + "', please run `docker-machine env <machine-name>` and update your IDE run configuration with"
                    + " the variables listed.");
        }
    }

    private static String getDockerHostIp() {
        String dockerHostEnvVariable = System.getenv("DOCKER_HOST");
        if (!isNullOrEmpty(dockerHostEnvVariable)) {
            return dockerHostEnvVariable.substring(6).split(":")[0];
        }
        return "127.0.0.1";
    }

    public String getIp() {
        return ip;
    }

    public DockerPort getPort(PortMapping portMapping) {
        return new DockerPort(ip, portMapping.getExternalPort(), portMapping.getInternalPort());
    }

    public Ports portsFor(PortMappings exposedPorts) {
        return new Ports(exposedPorts.stream().map(this::getPort).collect(toList()));
    }

}
