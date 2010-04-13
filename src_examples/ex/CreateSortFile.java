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

import static ex.MergeSortTest.LINE_LENGTH;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.codec.Hex;
import shared.util.Arithmetic;
import dapper.codelet.Codelet;
import dapper.codelet.Resource;

/**
 * A member of {@link MergeSortTest} that creates a file for sorting.
 * 
 * @author Roy Liu
 */
public class CreateSortFile implements Codelet {

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        NodeList children = parameters.getChildNodes();

        File file = new File(children.item(0).getTextContent());
        int nlines = Integer.parseInt(children.item(1).getTextContent());

        PrintStream ps = new PrintStream(file);

        for (int i = 0; i < nlines; i++) {

            ps.printf(Hex.bytesToHex(Arithmetic.nextBytes(LINE_LENGTH >>> 1)) //
                    .substring(0, LINE_LENGTH - 1));
            ps.printf("%n");
        }
    }

    /**
     * Default constructor.
     */
    public CreateSortFile() {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Create";
    }
}
