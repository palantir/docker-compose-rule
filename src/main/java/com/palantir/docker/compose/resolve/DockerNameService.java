package com.palantir.docker.compose.resolve;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.palantir.docker.compose.Container;
import com.palantir.docker.compose.DockerMachine;

import sun.net.spi.nameservice.NameService;

public class DockerNameService implements NameService {
    private final NameService delegate;

    private static final Map<String, String> containers = new ConcurrentHashMap<>();

    public DockerNameService(NameService delegate) {
        this.delegate = Preconditions.checkNotNull(delegate, "delegate");
    }

    @VisibleForTesting
    static void unregisterAll() {
        containers.clear();
    }

    public static void register(DockerMachine dockerMachine, Collection<Container> toRegister) {
        Set<String> containerNames = toRegister
                .stream()
                .map(Container::getContainerName)
                .collect(Collectors.toSet());

        Preconditions.checkArgument(Sets.intersection(containerNames, containers.keySet()).isEmpty(),
                "Cannot run two containers with the same name simultaneously.");

        Map<String, String> containerNamesToHostname = Maps.toMap(containerNames, container -> dockerMachine.getIp());

        containers.putAll(containerNamesToHostname);
    }

    public static void unregister(Collection<Container> toRemove) {
        toRemove.forEach(c -> containers.remove(c.getContainerName()));
    }

    @Override
    public InetAddress[] lookupAllHostAddr(String hostName) throws UnknownHostException {
        boolean hostIsContainerName = containers.containsKey(hostName);

        if (!hostIsContainerName) {
            return delegate.lookupAllHostAddr(hostName);
        }

        String dockerMachineAddress = containers.get(hostName);

        boolean hostIsIpAddress = InetAddresses.isInetAddress(dockerMachineAddress);

        if (hostIsIpAddress) {
            return new InetAddress[] { InetAddresses.forString(dockerMachineAddress) };
        } else {
            return delegate.lookupAllHostAddr(dockerMachineAddress);
        }
    }

    @Override
    public String getHostByAddr(byte[] bytes) throws UnknownHostException {
        return delegate.getHostByAddr(bytes);
    }
}
