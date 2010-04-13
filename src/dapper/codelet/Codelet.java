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

package dapper.codelet;

import java.util.List;

import org.w3c.dom.Node;

/**
 * Defines a basic unit of work in Dapper.
 * 
 * @apiviz.has dapper.codelet.Resource - - - argument
 * @apiviz.uses dapper.codelet.CodeletUtilities
 * @author Roy Liu
 */
public interface Codelet {

    /**
     * Executes a unit of work.
     * 
     * @param inResources
     *            the input {@link Resource}s.
     * @param outResources
     *            the output {@link Resource}s.
     * @param parameters
     *            the parameters.
     * @throws Exception
     *             when something goes awry.
     */
    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws Exception;

    /**
     * Creates a human-readable representation of this calculation.
     */
    public String toString();
}
