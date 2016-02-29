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
package com.palantir.docker.compose.configuration;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RemoteEnvironmentValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void dockerHostIsRequiredToBeSet() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                                                    .put("SOME_VARIABLE", "SOME_VALUE")
                                                    .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_HOST);
        RemoteEnvironmentValidator.validate(variables);
    }

    @Test
    public void dockerCertPathIsRequiredIfTlsIsOn() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .build();

        RemoteEnvironmentValidator validator = new RemoteEnvironmentValidator(variables);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_CERT_PATH);
        RemoteEnvironmentValidator.validate(variables);
    }

    @Test
    public void environmentWithAllValidVariablesSet_withoutTLS() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                                                    .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                    .put("SOME_VARIABLE", "SOME_VALUE")
                                                    .build();

        assertThat(RemoteEnvironmentValidator.validate(variables), is(variables));
    }

    @Test
    public void environmentWithAllValidVariablesSet_withTLS() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .put("SOME_VARIABLE", "SOME_VALUE")
                .build();

        assertThat(RemoteEnvironmentValidator.validate(variables), is(variables));
    }
}