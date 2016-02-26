package com.palantir.docker.compose.matchers;

import com.palantir.docker.compose.connection.DockerPort;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Collection;
import java.util.stream.Collectors;

public class AvailablePortMatcher extends TypeSafeMatcher<Collection<DockerPort>> {

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

    private static Collection<String> buildClosedPortsErrorMessage(Collection<DockerPort> unavailablePorts) {
        return unavailablePorts.stream()
                               .map(port -> "For host with ip address: " + port.getIp() + " external port '" + port.getExternalPort() + "' mapped to internal port '" + port.getInternalPort() + "' was unavailable")
                               .collect(Collectors.toList());
    }

    public static AvailablePortMatcher areAvailable() {
        return new AvailablePortMatcher();
    }

}
