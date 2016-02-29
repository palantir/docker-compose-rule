/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * THIS SOFTWARE CONTAINS PROPRIETARY AND CONFIDENTIAL INFORMATION OWNED BY PALANTIR TECHNOLOGIES INC.
 * UNAUTHORIZED DISCLOSURE TO ANY THIRD PARTY IS STRICTLY PROHIBITED
 *
 * For good and valuable consideration, the receipt and adequacy of which is acknowledged by Palantir and recipient
 * of this file ("Recipient"), the parties agree as follows:
 *
 * This file is being provided subject to the non-disclosure terms by and between Palantir and the Recipient.
 *
 * Palantir solely shall own and hereby retains all rights, title and interest in and to this software (including,
 * without limitation, all patent, copyright, trademark, trade secret and other intellectual property rights) and
 * all copies, modifications and derivative works thereof.  Recipient shall and hereby does irrevocably transfer and
 * assign to Palantir all right, title and interest it may have in the foregoing to Palantir and Palantir hereby
 * accepts such transfer. In using this software, Recipient acknowledges that no ownership rights are being conveyed
 * to Recipient.  This software shall only be used in conjunction with properly licensed Palantir products or
 * services.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.palantir.docker.compose.matchers;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.connection.DockerMachine;
import org.hamcrest.Description;

import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class DockerMachineEnvironmentMatcher extends ValueCachingMatcher<DockerMachine> {

    private final Map<String, String> expected;

    public DockerMachineEnvironmentMatcher(Map<String, String> expected) {
        this.expected = expected;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Docker Machine to have these environment variables:\n");
        description.appendValue(expected);
    }

    @Override
    protected boolean matchesSafely() {
        return missingEnvironmentVariables().isEmpty();
    }

    @Override
    protected void describeMismatchSafely(DockerMachine item, Description mismatchDescription) {
        mismatchDescription.appendText("\nThese environment variables were missing:\n");
        mismatchDescription.appendValue(missingEnvironmentVariables());
    }

    public static DockerMachineEnvironmentMatcher containsEnvironment(Map<String, String> environment) {
        return new DockerMachineEnvironmentMatcher(ImmutableMap.copyOf(environment));
    }

    private Map<String, String> missingEnvironmentVariables() {
        Map<String, String> environment = value.configDockerComposeProcess()
                                               .environment();
        return expected.entrySet()
                       .stream()
                       .filter(required -> !hasEntry(required.getKey(), required.getValue()).matches(environment))
                       .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
