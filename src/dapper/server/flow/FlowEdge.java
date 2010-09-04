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
    @Override
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
