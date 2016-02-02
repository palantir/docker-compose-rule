package com.palantir.docker.compose.connection;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ContainerNamesTest {

    @Test
    public void emptyPsOutputResultsInNoContainerNames() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\n");
        assertThat(names, is(new ContainerNames(emptyList())));
    }

    @Test
    public void psOutputWithASingleContainerResultsInASingleContainerName() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents");
        assertThat(names, is(new ContainerNames("db")));
    }

    @Test
    public void psOutputWithTwoContainersResultsInATwoContainerName() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\ndir_db2_1 other stuff");
        assertThat(names, is(new ContainerNames(asList("db", "db2"))));
    }

    @Test
    public void anEmptyLineInPsOutputIsIgnored() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n\n");
        assertThat(names, is(new ContainerNames("db")));
    }

    @Test
    public void aLineWithOnySpacesInPsOutputIsIgnored() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n   \n");
        assertThat(names, is(new ContainerNames("db")));
    }

}
