/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkipShutdownFunction implements ShutdownFunction {

    private static final Logger log = LoggerFactory.getLogger(SkipShutdownFunction.class);

    @Override
    public void shutdown(DockerCompose dockerCompose, Docker docker) {
        log.warn("\n"
                + "******************************************************************************************\n"
                + "* docker-compose-rule has been configured to skip docker-compose shutdown:               *\n"
                + "* this means the containers will be left running after tests finish executing.           *\n"
                + "* If you see this message when running on CI it means you are potentially abandoning     *\n"
                + "* long running processes and leaking resources.                                          *\n"
                + "******************************************************************************************");
    }

}
