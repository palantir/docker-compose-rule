package com.palantir.docker.compose.configuration;

public enum DockerType {

    DAEMON, REMOTE;

    public static final String OS_NAME = "os.name";
    public static final String MAC_OS = "Mac";

    public static DockerType getLocalDockerType() {
        if (System.getProperty(OS_NAME, "").startsWith(MAC_OS)) {
            return REMOTE;
        } else {
            return DAEMON;
        }
    }

}
