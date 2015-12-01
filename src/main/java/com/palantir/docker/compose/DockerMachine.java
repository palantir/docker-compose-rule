package com.palantir.docker.compose;

import static java.util.stream.Collectors.toList;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class DockerMachine {

    private static final List<String> requiredEnvVariables = ImmutableList.of(
            "DOCKER_TLS_VERIFY",
            "DOCKER_HOST",
            "DOCKER_CERT_PATH");

    private final String ip;

    public DockerMachine(String ip) {
        this.ip = ip;
    }

    public static DockerMachine fromEnvironment() {
        if (System.getProperty("os.name").startsWith("Mac")) {
            checkEnvVariables();
        }
        return new DockerMachine(getDockerHostIp());
    }

    private static void checkEnvVariables() {
        List<String> missingEnvironmentVariables = requiredEnvVariables.stream()
                                                                       .filter(envVariable -> isNullOrEmpty(System.getenv(envVariable)))
                                                                       .collect(toList());
        if (!missingEnvironmentVariables.isEmpty()) {
            throw new IllegalStateException("Missing required environment variables: '" + missingEnvironmentVariables
                                                    + "', please run `docker-machine env <machine-name>` and update your IDE run configuration with the variables listed.");
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
