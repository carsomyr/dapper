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

import static dapper.Constants.BLACK;
import static dapper.Constants.DARK_ORANGE;
import static dapper.Constants.LIGHT_GRAY;
import static dapper.codelet.Resource.ResourceType.INPUT_STREAM;
import static dapper.codelet.Resource.ResourceType.OUTPUT_STREAM;
import static dapper.server.flow.FlowEdge.FlowEdgeType.STREAM;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;

import shared.util.Control;
import dapper.client.ClientStatus;
import dapper.codelet.Identified;
import dapper.codelet.Resource;
import dapper.codelet.StreamResource;
import dapper.server.ClientState;

/**
 * An edge class that represents the TCP stream relationship between two {@link FlowNode}s.
 * 
 * @author Roy Liu
 */
public class StreamEdge implements FlowEdge, Identified {

    FlowNode u, v;

    boolean inverted;

    String name, identifier;

    /**
     * Default constructor.
     */
    public StreamEdge(FlowNode u, FlowNode v) {

        this.u = u;
        this.v = v;

        this.inverted = false;
        this.name = "";
        this.identifier = null;
    }

    /**
     * Copies this edge.
     */
    @Override
    public StreamEdge clone() {

        final StreamEdge res;

        try {

            res = (StreamEdge) super.clone();

        } catch (CloneNotSupportedException e) {

            throw new RuntimeException(e);
        }

        res.setU(null);
        res.setV(null);

        return res;
    }

    /**
     * Checks whether connections are established in the reverse direction.
     */
    public boolean isInverted() {
        return this.inverted;
    }

    /**
     * Sets whether connections are established in the reverse direction.
     */
    public StreamEdge setInverted(boolean inverted) {

        this.inverted = inverted;

        return this;
    }

    public FlowEdgeType getType() {
        return STREAM;
    }

    public String getName() {
        return this.name;
    }

    public StreamEdge setName(String name) {

        Control.checkTrue(name != null, //
                "Name must be non-null");

        this.name = name;

        return this;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public FlowNode getU() {
        return this.u;
    }

    public void setU(FlowNode u) {
        this.u = u;
    }

    public Resource createUResource() {
        return new StreamResource<OutputStream>(OUTPUT_STREAM, this.identifier, this.name, //
                !this.inverted ? this.v.getClientState().getAddress() : null);
    }

    public FlowNode getV() {
        return this.v;
    }

    public void setV(FlowNode v) {
        this.v = v;
    }

    public Resource createVResource() {
        return new StreamResource<InputStream>(INPUT_STREAM, this.identifier, this.name, //
                !this.inverted ? null : this.u.getClientState().getAddress());
    }

    public void generate() {
        this.identifier = FlowUtilities.createIdentifier(StreamEdge.class);
    }

    public void render(Formatter f) {

        final ClientState csh = getU().getClientState();
        final ClientStatus status = (csh != null) ? csh.getStatus() : null;
        final String color;

        if (status != null) {

            switch (status) {

            case EXECUTE:
                color = DARK_ORANGE;
                break;

            default:
                color = BLACK;
                break;
            }

        } else {

            switch (getU().getLogicalNode().getStatus()) {

            case EXECUTE:
            case FINISHED:
                color = LIGHT_GRAY;
                break;

            default:
                color = BLACK;
                break;
            }
        }

        f.format("%n\tnode_%d -> node_%d [%n", getU().getOrder(), getV().getOrder());
        f.format("\t\tstyle = \"dashed\",%n");
        f.format("\t\tcolor = \"#%s\",%n", color);
        f.format("\t\tlabel = \"%s\"%n", this.inverted ? "*" : "");
        f.format("\t];%n");
    }
}
