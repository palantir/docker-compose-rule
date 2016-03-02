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
package com.palantir.docker.compose.execution;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SynchronousDockerComposeExecutableShould {
    @Mock private Process executedProcess;
    @Mock private DockerComposeExecutable dockerComposeExecutable;
    private SynchronousDockerComposeExecutable dockerCompose;

    @Before
    public void setup() throws IOException {
        when(dockerComposeExecutable.execute(anyVararg())).thenReturn(executedProcess);
        dockerCompose = new SynchronousDockerComposeExecutable(dockerComposeExecutable);

        givenTheUnderlyingProcessHasOutput("");
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(0);
    }

    @Test public void
    respond_with_the_exit_code_of_the_executed_process() throws IOException {
        int expectedExitCode = 1;
        
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(expectedExitCode);

        assertThat(dockerCompose.run("rm", "-f").exitCode(), is(expectedExitCode));
    }

    @Test public void
    respond_with_the_output_of_the_executed_process() throws IOException {
        String expectedOutput = "some output";

        givenTheUnderlyingProcessHasOutput(expectedOutput);

        assertThat(dockerCompose.run("rm", "-f").output(), is(expectedOutput));
    }

    private void givenTheUnderlyingProcessHasOutput(String output) {
        byte[] outputBytes = output.getBytes(StandardCharsets.UTF_8);
        when(executedProcess.getInputStream())
                .thenReturn(new ByteArrayInputStream(outputBytes));
    }

    private void givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(int exitCode) {
        when(executedProcess.exitValue()).thenReturn(exitCode);
    }

}
