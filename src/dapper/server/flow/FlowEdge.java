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

package dapper.server.flow;

import shared.event.EnumType;
import shared.parallel.Edge;
import dapper.codelet.Nameable;
import dapper.codelet.Resource;

/**
 * An edge class representing the output-input relationship between two {@link FlowNode}s.
 * 
 * @apiviz.owns dapper.server.flow.FlowEdge.FlowEdgeType
 * @author Roy Liu
 */
public interface FlowEdge extends Edge<FlowNode>, EnumType<FlowEdge.FlowEdgeType>, Cloneable, Renderable, Nameable {

    /**
     * An enumeration of {@link FlowEdge} types.
     */
    public enum FlowEdgeType {

        /**
         * Indicates a handle relationship interpreted in a problem-specific context.
         */
        HANDLE, //

        /**
         * Indicates a TCP stream relationship.
         */
        STREAM, //

        /**
         * Indicates a dummy relationship.
         */
        DUMMY;
    }

    /**
     * Regenerates the identifier.
     */
    public void generate();

    /**
     * Sets the name.
     */
    public FlowEdge setName(String name);

    /**
     * Copies this edge.
     */
    public FlowEdge clone();

    /**
     * Creates a {@link Resource} for the start node.
     */
    public Resource createUResource();

    /**
     * Creates a {@link Resource} for the end node.
     */
    public Resource createVResource();
}
