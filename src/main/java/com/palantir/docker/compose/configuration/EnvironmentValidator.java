package com.palantir.docker.compose.configuration;

import java.util.Map;

public enum EnvironmentValidator {
    DAEMON {
        @Override
        public Map<String, String> validate(Map<String, String> dockerEnvironment) {
            return DaemonEnvironmentValidator.INSTANCE.validate(dockerEnvironment);
        }
    },

    REMOTE {
        @Override
        public Map<String, String> validate(Map<String, String> dockerEnvironment) {
            return new RemoteEnvironmentValidator(dockerEnvironment).validate();
        }
    },

    ADDITIONAL {
        @Override
        public Map<String, String> validate(Map<String, String> dockerEnvironment) {
            return AdditionalEnvironmentValidator.INSTANCE.validate(dockerEnvironment);
        }
    };

    public abstract Map<String, String> validate(Map<String, String> dockerEnvironment);

}
