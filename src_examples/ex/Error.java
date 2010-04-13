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

import shared.util.Arithmetic;
import shared.util.Control;
import dapper.codelet.Codelet;
import dapper.codelet.Resource;

/**
 * A {@link Codelet} that flips a coin to see if an exception should be raised.
 * 
 * @author Roy Liu
 */
public class Error implements Codelet {

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) {

        if (Arithmetic.nextInt(16) == 0) {

            // Check that inner class loading works.
            new Runnable() {

                public void run() {
                    throw new IllegalStateException("This exception was artificially induced");
                }

            }.run();
        }

        Control.sleep(Arithmetic.nextInt(2000));
    }

    /**
     * Default constructor.
     */
    public Error() {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "(Error)";
    }
}
