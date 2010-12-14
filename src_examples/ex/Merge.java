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

import static ex.MergeSortTest.LINE_LENGTH;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import org.dapper.codelet.Codelet;
import org.dapper.codelet.Resource;
import org.w3c.dom.Node;

/**
 * A member of {@link MergeSortTest} that merges two {@link InputStream}s into a single {@link OutputStream}.
 * 
 * @author Roy Liu
 */
public class Merge implements Codelet {

    @Override
    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        Scanner scanner1 = new Scanner((inResources.get(0)).getInputStream());
        Scanner scanner2 = new Scanner((inResources.get(1)).getInputStream());

        PrintStream ps = new PrintStream(outResources.get(0).getOutputStream(), true);

        String line1 = null, line2 = null;

        for (; scanner1.hasNextLine() && scanner2.hasNextLine();) {

            line1 = (line1 == null) ? scanner1.nextLine() : line1;
            line2 = (line2 == null) ? scanner2.nextLine() : line2;

            if (line1.length() != LINE_LENGTH - 1) {
                throw new IllegalStateException(String.format("Unexpected line length: %d", line1.length()));
            }

            if (line2.length() != LINE_LENGTH - 1) {
                throw new IllegalStateException(String.format("Unexpected line length: %d", line2.length()));
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
     * Creates a human-readable description of this {@link Codelet}.
     */
    @Override
    public String toString() {
        return "Merge";
    }
}
