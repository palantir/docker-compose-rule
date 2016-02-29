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
package com.palantir.docker.compose.connection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class ContainerNames implements Iterable<String> {

    private final List<String> containerNames;

    public ContainerNames(String singleContainerName) {
        this(singletonList(singleContainerName));
    }

    public ContainerNames(List<String> containerNames) {
        this.containerNames = containerNames;
    }

    public static ContainerNames parseFromDockerComposePs(String psOutput) {
        String[] splitOnSeparator = psOutput.split("-+\n");
        if (splitOnSeparator.length < 2) {
            return new ContainerNames(emptyList());
        }
        return new ContainerNames(getContainerNamesAtStartOfLines(splitOnSeparator[1]));
    }

    private static List<String> getContainerNamesAtStartOfLines(String psContainerOutput) {
        return Arrays.stream(psContainerOutput.split("\n"))
                     .map(String::trim)
                     .filter(line -> !line.isEmpty())
                     .map(line -> line.split(" "))
                     .map(psColumns -> psColumns[0])
                     .map(name -> name.split("_"))
                     .filter(nameComponents -> nameComponents.length == 3)
                     .map(nameComponents -> nameComponents[1])
                     .collect(toList());
    }

    @Override
    public Iterator<String> iterator() {
        return containerNames.iterator();
    }

    public Stream<String> stream() {
        return containerNames.stream();
    }

    public int size() {
        return containerNames.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerNames);
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
        ContainerNames other = (ContainerNames) obj;
        return Objects.equals(containerNames, other.containerNames);
    }

    @Override
    public String toString() {
        return "ContainerNames [containerNames=" + containerNames + "]";
    }

}
