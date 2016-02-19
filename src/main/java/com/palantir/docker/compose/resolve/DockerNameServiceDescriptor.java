package com.palantir.docker.compose.resolve;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;
import sun.net.spi.nameservice.dns.DNSNameService;

public class DockerNameServiceDescriptor implements NameServiceDescriptor {

    @Override
    public NameService createNameService() throws Exception {
        return new DockerNameService(new DNSNameService());
    }

    @Override
    public String getProviderName() {
        return "docker-compose-rule";
    }

    @Override
    public String getType() {
        return "dns";
    }
}
