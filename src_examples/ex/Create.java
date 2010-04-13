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

import java.util.List;

import org.w3c.dom.Node;

import shared.array.ObjectArray;
import shared.util.Arithmetic;
import shared.util.Control;
import dapper.codelet.Codelet;
import dapper.codelet.CodeletUtilities;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;

/**
 * A {@link Codelet} that simulates creation of random numbers of file handles along {@link OutputHandleResource}s.
 * 
 * @author Roy Liu
 */
public class Create implements Codelet {

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) {

        Arithmetic.randomize();

        for (OutputHandleResource ohr : CodeletUtilities.filter(outResources, OutputHandleResource.class)) {

            String stem = CodeletUtilities.createStem();

            int nhandles = Arithmetic.nextInt(3) + 1;

            if (Arithmetic.nextInt(2) == 0) {

                for (int i = 0; i < nhandles; i++) {

                    String subidentifier = String.format("%s_%08x", stem, i);
                    ohr.put("file_".concat(subidentifier), subidentifier);
                }

            } else {

                ObjectArray<String> newEntries = new ObjectArray<String>(String.class, nhandles, 2);

                for (int i = 0; i < nhandles; i++) {

                    String subidentifier = String.format("%s_%08x", stem, i);
                    newEntries.set("file_".concat(subidentifier), i, 0);
                    newEntries.set(subidentifier, i, 1);
                }

                ohr.put(newEntries);
            }
        }

        Control.sleep(Arithmetic.nextInt(2000));
    }

    /**
     * Default constructor.
     */
    public Create() {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Create";
    }
}
