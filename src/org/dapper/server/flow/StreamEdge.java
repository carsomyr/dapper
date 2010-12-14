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

package org.dapper.server.flow;

import static org.dapper.Constants.BLACK;
import static org.dapper.Constants.DARK_ORANGE;
import static org.dapper.Constants.LIGHT_GRAY;
import static org.dapper.codelet.Resource.ResourceType.INPUT_STREAM;
import static org.dapper.codelet.Resource.ResourceType.OUTPUT_STREAM;
import static org.dapper.server.flow.FlowEdge.FlowEdgeType.STREAM;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;

import org.dapper.client.ClientStatus;
import org.dapper.codelet.Identified;
import org.dapper.codelet.Resource;
import org.dapper.codelet.StreamResource;
import org.dapper.server.ClientState;
import org.shared.util.Control;

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
     * Gets whether connections are established in the reverse direction.
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

    @Override
    public FlowEdgeType getType() {
        return STREAM;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public StreamEdge setName(String name) {

        Control.checkTrue(name != null, //
                "Name must be non-null");

        this.name = name;

        return this;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public FlowNode getU() {
        return this.u;
    }

    @Override
    public void setU(FlowNode u) {
        this.u = u;
    }

    @Override
    public Resource createUResource() {
        return new StreamResource<OutputStream>(OUTPUT_STREAM, this.identifier, this.name, //
                !this.inverted ? this.v.getClientState().getAddress() : null);
    }

    @Override
    public FlowNode getV() {
        return this.v;
    }

    @Override
    public void setV(FlowNode v) {
        this.v = v;
    }

    @Override
    public Resource createVResource() {
        return new StreamResource<InputStream>(INPUT_STREAM, this.identifier, this.name, //
                !this.inverted ? null : this.u.getClientState().getAddress());
    }

    @Override
    public void generate() {
        this.identifier = FlowUtilities.createIdentifier(StreamEdge.class);
    }

    @Override
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

        f.format("%n");
        f.format("    node_%d -> node_%d [%n", getU().getOrder(), getV().getOrder());
        f.format("        style = \"dashed\",%n");
        f.format("        color = \"#%s\",%n", color);
        f.format("        label = \"%s\"%n", this.inverted ? "*" : "");
        f.format("    ];%n");
    }
}
