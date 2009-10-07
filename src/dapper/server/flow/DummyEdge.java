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
