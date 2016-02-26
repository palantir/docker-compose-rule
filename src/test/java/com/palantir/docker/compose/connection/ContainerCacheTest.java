package com.palantir.docker.compose.connection;

import com.palantir.docker.compose.execution.DockerComposeExecutable;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContainerCacheTest {

    private static final String CONTAINER_NAME = "container";

    private final DockerComposeExecutable dockerComposeExecutable = mock(DockerComposeExecutable.class);
    private final ContainerCache containers = new ContainerCache(dockerComposeExecutable);

    @Before
    public void setup() {
        when(dockerComposeExecutable.container(CONTAINER_NAME)).thenReturn(new Container(CONTAINER_NAME, dockerComposeExecutable));
    }

    @Test
    public void gettingANewContainerReturnsAContainerWithTheSpecifiedName() {
        Container container = containers.get(CONTAINER_NAME);
        assertThat(container, is(new Container(CONTAINER_NAME, dockerComposeExecutable)));
    }

    @Test
    public void gettingAContainerTwiceReturnsTheSameObject() {
        Container container = containers.get(CONTAINER_NAME);
        Container sameContainer = containers.get(CONTAINER_NAME);
        assertThat(container, is(sameInstance(sameContainer)));
    }

}
