package com.palantir.docker.compose.configuration;

import static com.palantir.docker.compose.configuration.EnvironmentVariables2.MAC_OS;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.OS_NAME;

import java.util.Map;

public enum EnvironmentValidator {
    DAEMON {
        @Override
        Map<String, String> validate(Map<String, String> dockerEnvironment) {
            return DaemonEnvironmentValidator.INSTANCE.validate(dockerEnvironment);
        }
    },

    REMOTE {
        @Override
        Map<String, String> validate(Map<String, String> dockerEnvironment) {
            return new RemoteEnvironmentValidator(dockerEnvironment).validate();
        }
    },

    ADDITIONAL {
        @Override
        Map<String, String> validate(Map<String, String> dockerEnvironment) {
            return AdditionalEnvironmentValidator.INSTANCE.validate(dockerEnvironment);
        }
    };

    abstract Map<String, String> validate(Map<String, String> dockerEnvironment);

    // TODO (fdesouza): this can be tested
    public static EnvironmentValidator getLocalEnvironmentValidator(Map<String, String> systemEnvironment) {
        if (systemEnvironment.getOrDefault(OS_NAME, "").startsWith(MAC_OS)) {
            return REMOTE;
        } else {
            // A corollary to this is that we only support Mac and Linux, which is true currently
            return DAEMON;
        }
    }

}
