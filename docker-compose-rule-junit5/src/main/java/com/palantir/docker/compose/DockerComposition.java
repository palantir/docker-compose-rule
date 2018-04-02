package com.palantir.docker.compose;

import com.palantir.docker.compose.configuration.DaemonHostIpResolver;
import com.palantir.docker.compose.ext.DockerComposeExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerComposeExtension.class)
public @interface DockerComposition {

    String TEMPLATE_RANDOM_PROJECT_NAME = "{RANDOM}";
    String TEMPLATE_NO_LOG_COLLECTOR = "{NONE}";

    /**
     * The docker compose files.
     *
     * @return the docker compose files
     */
    String[] files();

    /**
     * The project name.
     *
     * @return the project name
     */
    String project() default TEMPLATE_RANDOM_PROJECT_NAME;

    /**
     * Determines if docker-compose should tear down and re-start the cluster between each test container.  Defaults to false.
     *
     * @return true if docker-compose should restart cluster between test containers, false otherwise
     */
    boolean tearDownBetweenTests() default false;

    /**
     * The host of the docker machine to use, which can be "localhost" or "127.0.0.1"
     * if using a local docker machine.
     *
     * @return the machine host
     */
    String machineHost() default DaemonHostIpResolver.LOCALHOST;

    int retryAttempts() default 2;

    boolean pullOnStartup() default false;

    boolean removeConflictingContainersOnStartup() default true;

    int nativeHealthCheckTimeoutSeconds() default 120;

    String saveLogsToPath() default TEMPLATE_NO_LOG_COLLECTOR;

    // TODO (cmoore): Implement cluster waits configuration

}
