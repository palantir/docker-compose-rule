package com.palantir.docker.compose.resolve;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.InetAddresses;
import com.palantir.docker.compose.Container;
import com.palantir.docker.compose.DockerMachine;

import sun.net.spi.nameservice.NameService;

@RunWith(MockitoJUnitRunner.class)
public class DockerNameServiceTest {
    private static final String MACHINE_HOST = "machine ip";
    private static final String MACHINE_IP = "123.124.125.126";
    private static final String SECOND_MACHINE_IP = "126.125.124.123";
    private static final String CONTAINER_NAME = "container name";
    private static final String SECOND_CONTAINER_NAME = "second container name";
    private static final InetAddress[] delegateAddresses = new InetAddress[0];

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @Mock
    private NameService delegate;

    @Mock
    private Container dockerContainer;

    @Mock
    private Container secondDockerContainer;

    @Mock
    private DockerMachine dockerMachine;

    @Mock
    private DockerMachine secondDockerMachine;

    private DockerNameService dockerNameService;

    @Before
    public void before() throws UnknownHostException {
        dockerNameService = new DockerNameService(delegate);
        when(dockerMachine.getIp()).thenReturn(MACHINE_HOST);
        when(dockerContainer.getContainerName()).thenReturn(CONTAINER_NAME);
        when(secondDockerContainer.getContainerName()).thenReturn(SECOND_CONTAINER_NAME);
        when(delegate.lookupAllHostAddr(any())).thenReturn(delegateAddresses);
    }

    @After
    public void after() {
        DockerNameService.unregisterAll();
    }

    @Test
    public void withNoRegistrationRequestsGoToDelegate() throws UnknownHostException {
        assertThat(dockerNameService.lookupAllHostAddr("foo"), is(delegateAddresses));
    }

    @Test
    public void afterRegistrationRequestsForOtherHostnamesGoToDelegate() throws UnknownHostException {
        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));

        String other = "other container name";

        assertThat(dockerNameService.lookupAllHostAddr(other), is(delegateAddresses));
    }

    @Test
    public void afterRegistrationRequestsForContainerHostnameAreRewrittenToDockerMachineHost()
            throws UnknownHostException {
        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));
        assertThat(dockerNameService.lookupAllHostAddr(CONTAINER_NAME), is(delegateAddresses));
    }

    @Test
    public void ifContainerAddressIsAHostnameThenTheIpIsReturnedDirectly() throws UnknownHostException {
        when(dockerMachine.getIp()).thenReturn(MACHINE_IP);
        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));

        InetAddress[] addresses = new InetAddress[] {InetAddresses.forString(MACHINE_IP)};

        assertThat(dockerNameService.lookupAllHostAddr(CONTAINER_NAME), is(addresses));

        verify(delegate, never()).lookupAllHostAddr(MACHINE_IP);
    }

    @Test
    public void getHostByAddrIsPassedThrough() throws UnknownHostException {
        byte[] data = "foo".getBytes(StandardCharsets.UTF_8);
        String host = "host";
        when(delegate.getHostByAddr(data)).thenReturn(host);
        assertThat(dockerNameService.getHostByAddr(data), is(host));
    }

    @Test
    public void canRegisterAndUseDisjointContainerSetsSimultaneously() throws UnknownHostException {
        when(dockerMachine.getIp()).thenReturn(MACHINE_IP);
        when(secondDockerMachine.getIp()).thenReturn(SECOND_MACHINE_IP);

        InetAddress[] addresses = new InetAddress[] {InetAddresses.forString(MACHINE_IP)};
        InetAddress[] secondAddresses = new InetAddress[] {InetAddresses.forString(SECOND_MACHINE_IP)};

        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));
        DockerNameService.register(secondDockerMachine, ImmutableSet.of(secondDockerContainer));

        assertThat(dockerNameService.lookupAllHostAddr(CONTAINER_NAME), is(addresses));
        assertThat(dockerNameService.lookupAllHostAddr(SECOND_CONTAINER_NAME), is(secondAddresses));
    }

    @Test
    public void conjointContainerSetsCauseExceptionToBeThrown() {
        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));

        expected.expect(IllegalArgumentException.class);
        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));
    }

    @Test
    public void canReregisterAndUseContainerAfterUnregistering() throws UnknownHostException {
        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));

        DockerNameService.unregister(ImmutableSet.of(dockerContainer));

        DockerNameService.register(dockerMachine, ImmutableSet.of(dockerContainer));

        assertThat(dockerNameService.lookupAllHostAddr(CONTAINER_NAME), is(delegateAddresses));
    }
}