package com.palantir.docker.compose.execution;

import static com.palantir.docker.compose.execution.DockerComposeExecOption.noOptions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import org.junit.Test;

public class DockerComposeExecOptionShould {

    @Test public void
    be_constructable_with_no_args() {
        DockerComposeExecOption option = noOptions();
        assertThat(option.options(), empty());
    }
}
