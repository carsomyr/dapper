/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 The Regents of the University of California <br />
 * <br />
 * This library is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option)
 * any later version. <br />
 * <br />
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. <br />
 * <br />
 * You should have received a copy of the GNU Lesser General Public License along with this library. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 */

package ex;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.w3c.dom.Node;

import dapper.codelet.Codelet;
import dapper.codelet.CodeletUtilities;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;

/**
 * A member of {@link MergeSortTest} that splits its single {@link InputStream} among multiple file outputs.
 * 
 * @author Roy Liu
 */
public class Split implements Codelet {

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        String path = parameters.getTextContent();

        Map<String, List<Resource>> outResourcesMap = CodeletUtilities.groupByName(outResources);

        List<String> handles = new ArrayList<String>();
        List<PrintStream> outStreams = new ArrayList<PrintStream>();

        for (Resource outResource : outResourcesMap.get("to_sort")) {

            OutputHandleResource ohr = (OutputHandleResource) outResource;

            String stem = CodeletUtilities.createStem();
            String handle = String.format(path.concat("/").concat(stem));

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
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Split";
    }
}
