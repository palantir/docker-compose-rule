package com.palantir.docker.compose.connection;

import java.util.Objects;

public class PortMapping {

    private final int externalPort;
    private final int internalPort;

    public PortMapping(int externalPort, int internalPort) {
        this.externalPort = externalPort;
        this.internalPort = internalPort;
    }

    public int getExternalPort() {
        return externalPort;
    }

    public int getInternalPort() {
        return internalPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalPort, internalPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PortMapping other = (PortMapping) obj;
        return Objects.equals(externalPort, other.externalPort) &&
               Objects.equals(internalPort, other.internalPort);
    }

    @Override
    public String toString() {
        return "PortMapping [externalPort=" + externalPort + ", internalPort="
                + internalPort + "]";
    }

}
