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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import org.w3c.dom.Node;

import dapper.codelet.Codelet;
import dapper.codelet.Resource;

/**
 * A member of {@link MergeSortTest} that merges two {@link InputStream}s into a single {@link OutputStream}.
 * 
 * @author Roy Liu
 */
public class Merge implements Codelet {

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        Scanner scanner1 = new Scanner((inResources.get(0)).getInputStream());
        Scanner scanner2 = new Scanner((inResources.get(1)).getInputStream());

        PrintStream ps = new PrintStream(outResources.get(0).getOutputStream(), true);

        String line1 = null, line2 = null;

        for (; scanner1.hasNextLine() && scanner2.hasNextLine();) {

            line1 = (line1 == null) ? scanner1.nextLine() : line1;
            line2 = (line2 == null) ? scanner2.nextLine() : line2;

            if (line1.length() != LINE_LENGTH - 1) {
                throw new IllegalStateException( //
                        String.format("Unexpected line length: %d", line1.length()));
            }

            if (line2.length() != LINE_LENGTH - 1) {
                throw new IllegalStateException( //
                        String.format("Unexpected line length: %d", line2.length()));
            }

            if (line1.compareTo(line2) < 0) {

                ps.println(line1);
                line1 = null;

            } else {

                ps.println(line2);
                line2 = null;
            }
        }

        if (line1 != null) {
            ps.println(line1);
        }

        if (line2 != null) {
            ps.println(line2);
        }

        for (; scanner1.hasNextLine();) {
            ps.println(scanner1.nextLine());
        }

        for (; scanner2.hasNextLine();) {
            ps.println(scanner2.nextLine());
        }

        if (scanner1.ioException() != null) {
            throw scanner1.ioException();
        }

        if (scanner2.ioException() != null) {
            throw scanner2.ioException();
        }

        ps.close();
    }

    /**
     * Default constructor.
     */
    public Merge() {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Merge";
    }
}
