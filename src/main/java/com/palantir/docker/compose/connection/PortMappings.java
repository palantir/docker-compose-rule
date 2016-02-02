package com.palantir.docker.compose.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class PortMappings implements Iterable<PortMapping> {

    private static final Pattern PORT_PATTERN = Pattern.compile(":(\\d+)->(\\d+)/tcp");
    private static final int EXTERNAL_PORT = 1;
    private static final int INTERNAL_PORT = 2;

    private final List<PortMapping> ports;

    public PortMappings(List<PortMapping> ports) {
        this.ports = ports;
    }

    public PortMappings(PortMapping port) {
        this(Collections.singletonList(port));
    }

    public static PortMappings parseFromDockerComposePs(String psOutput) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(psOutput), "No container found");
        Matcher matcher = PORT_PATTERN.matcher(psOutput);
        List<PortMapping> ports = new ArrayList<>();
        while (matcher.find()) {
            ports.add(new PortMapping(Integer.parseInt(matcher.group(EXTERNAL_PORT)),
                                      Integer.parseInt(matcher.group(INTERNAL_PORT))));
        }
        return new PortMappings(ports);
    }

    @Override
    public Iterator<PortMapping> iterator() {
        return ports.iterator();
    }

    public Stream<PortMapping> stream() {
        return ports.stream();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ports);
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
        PortMappings other = (PortMappings) obj;
        return Objects.equals(ports, other.ports);
    }

    @Override
    public String toString() {
        return "PortMappings [ports=" + ports + "]";
    }

}
