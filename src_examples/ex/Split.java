/**
 * <p>
 * Copyright (c) 2008 The Regents of the University of California<br>
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * </p>
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>Neither the name of the author nor the names of any contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.</li>
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
 */

package ex;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.dapper.codelet.Codelet;
import org.dapper.codelet.CodeletUtilities;
import org.dapper.codelet.OutputHandleResource;
import org.dapper.codelet.Resource;
import org.w3c.dom.Node;

/**
 * A member of {@link MergeSortTest} that splits its single {@link InputStream} among multiple file outputs.
 * 
 * @author Roy Liu
 */
public class Split implements Codelet {

    @Override
    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        String path = parameters.getTextContent();

        Map<String, List<Resource>> outResourcesMap = CodeletUtilities.groupByName(outResources);

        List<String> handles = new ArrayList<String>();
        List<PrintStream> outStreams = new ArrayList<PrintStream>();

        for (Resource outResource : outResourcesMap.get("to_sort")) {

            OutputHandleResource ohr = (OutputHandleResource) outResource;

            String stem = CodeletUtilities.createStem();
            String handle = String.format("%s/%s", path, stem);

            ohr.put(handle, stem);

            handles.add(handle);
            outStreams.add(new PrintStream(handle));
        }

        for (Resource outResource : outResourcesMap.get("to_cleanup")) {

            OutputHandleResource ohr = (OutputHandleResource) outResource;

            for (String handle : handles) {
                ohr.put(handle);
            }
        }

        Scanner scanner = new Scanner(inResources.get(0).getInputStream());

        for (int i = 0, modulus = outStreams.size(); scanner.hasNextLine(); i = (i + 1) % modulus) {
            outStreams.get(i).printf("%s%n", scanner.nextLine());
        }

        for (PrintStream outStream : outStreams) {
            outStream.close();
        }
    }

    /**
     * Default constructor.
     */
    public Split() {
    }

    /**
     * Creates a human-readable description of this {@link Codelet}.
     */
    @Override
    public String toString() {
        return "Split";
    }
}
