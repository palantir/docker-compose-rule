package com.palantir.docker.compose.matchers;

import com.palantir.docker.compose.connection.DockerPort;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.palantir.docker.compose.matchers.AvailablePortMatcher.areAvailable;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;

public class AvailablePortMatcherTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void noUnavailablePortsIsGood() throws Exception {
        List<DockerPort> unavailablePorts = emptyList();
        assertThat(unavailablePorts, areAvailable());
    }

    @Test
    public void someUnavailablePortsResultsInUsefulErrorMessages() throws Exception {
        List<DockerPort> unavailablePorts = newArrayList(new DockerPort("0.0.0.0", 1234, 1234),
                                                         new DockerPort("1.2.3.4", 2345, 3456));
        exception.expect(AssertionError.class);
        exception.expectMessage("For host with ip address: 0.0.0.0");
        exception.expectMessage("external port '1234' mapped to internal port '1234' was unavailable");
        exception.expectMessage("For host with ip address: 1.2.3.4");
        exception.expectMessage("external port '2345' mapped to internal port '3456' was unavailable");
        assertThat(unavailablePorts, areAvailable());
    }

}