/**
 * <p>
 * Copyright (C) 2008 The Regents of the University of California<br />
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

import static dapper.Constants.GRAY;
import static dapper.Constants.LIGHT_GRAY;
import static dapper.server.flow.FlowEdge.FlowEdgeType.DUMMY;

import java.util.Formatter;

import shared.util.Control;
import dapper.codelet.Resource;

/**
 * A edge class that represents the dummy relationship between two {@link FlowNode}s.
 * 
 * @author Roy Liu
 */
public class DummyEdge implements FlowEdge {

    FlowNode u, v;

    String name;

    /**
     * Default constructor.
     */
    public DummyEdge(FlowNode u, FlowNode v) {

        this.u = u;
        this.v = v;

        this.name = "";
    }

    /**
     * Copies this edge.
     */
    @Override
    public DummyEdge clone() {

        final DummyEdge res;

        try {

            res = (DummyEdge) super.clone();

        } catch (CloneNotSupportedException e) {

            throw new RuntimeException(e);
        }

        res.setU(null);
        res.setV(null);

        return res;
    }

    public FlowEdgeType getType() {
        return DUMMY;
    }

    public String getName() {
        return this.name;
    }

    public DummyEdge setName(String name) {

        Control.checkTrue(name != null, //
                "Name must be non-null");

        this.name = name;

        return this;
    }

    public FlowNode getU() {
        return this.u;
    }

    public void setU(FlowNode u) {
        this.u = u;
    }

    public Resource createUResource() {
        throw new UnsupportedOperationException();
    }

    public FlowNode getV() {
        return this.v;
    }

    public void setV(FlowNode v) {
        this.v = v;
    }

    public Resource createVResource() {
        throw new UnsupportedOperationException();
    }

    public void generate() {
        // Do nothing.
    }

    public void render(Formatter f) {

        final String color;

        switch (getU().getLogicalNode().getStatus()) {

        case EXECUTE:
        case FINISHED:
            color = LIGHT_GRAY;
            break;

        default:
            color = GRAY;
            break;
        }

        f.format("%n\tnode_%d -> node_%d [%n", getU().getOrder(), getV().getOrder());
        f.format("\t\tstyle = \"dotted\",%n");
        f.format("\t\tcolor = \"#%s\",%n", color);
        f.format("\t];%n");
    }
}
