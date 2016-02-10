package com.palantir.docker.compose.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.palantir.docker.compose.connection.DockerPort;

public class AvailablePortMatcher extends TypeSafeMatcher<Collection<DockerPort>> {

    public static AvailablePortMatcher areAvailable() {
        return new AvailablePortMatcher();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("No ports to be unavailable");
    }

    @Override
    protected boolean matchesSafely(Collection<DockerPort> unavailablePorts) {
        return unavailablePorts.isEmpty();
    }

    @Override
    protected void describeMismatchSafely(Collection<DockerPort> unavailablePorts, Description mismatchDescription) {
        mismatchDescription.appendValueList("These ports were unavailable:\n", "\n", ".", buildClosedPortsErrorMessage(unavailablePorts));
    }

    private static List<String> buildClosedPortsErrorMessage(Collection<DockerPort> unavailablePorts) {
        return unavailablePorts.stream()
                               .map(port -> "For host with ip address: " + port.getIp() + " external port '" + port.getExternalPort() + "' mapped to internal port '" + port.getInternalPort() + "' was unavailable")
                               .collect(Collectors.toList());
    }

}
