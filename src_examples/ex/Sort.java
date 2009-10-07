/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 Roy Liu, The Regents of the University of California <br />
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
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.w3c.dom.Node;

import dapper.codelet.Codelet;
import dapper.codelet.Resource;

/**
 * A member of {@link MergeSortTest} that sorts its single file input in-memory and has a single stream output.
 * 
 * @author Roy Liu
 */
public class Sort implements Codelet {

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        List<ByteBuffer> lines = new ArrayList<ByteBuffer>();

        for (Scanner scanner = new Scanner(inResources.get(0).getInputStream()); scanner.hasNextLine();) {
            lines.add(ByteBuffer.wrap(scanner.nextLine().getBytes()));
        }

        Collections.sort(lines);

        PrintStream ps = new PrintStream(outResources.get(0).getOutputStream(), true);

        for (ByteBuffer line : lines) {
            ps.println(new String(line.array()));
        }

        ps.close();
    }

    /**
     * Default constructor.
     */
    public Sort() {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Sort";
    }
}
