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

import static dapper.Constants.BLACK;
import static dapper.Constants.DARK_GREEN;
import static dapper.Constants.DARK_ORANGE;
import static dapper.server.flow.FlowEdge.FlowEdgeType.HANDLE;

import java.util.Formatter;

import shared.array.ObjectArray;
import shared.util.Control;
import dapper.client.ClientStatus;
import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;
import dapper.server.ClientState;

/**
 * An edge class that represents the handle output-input relationship between two {@link FlowNode}s.
 * 
 * @author Roy Liu
 */
public class HandleEdge implements FlowEdge {

    FlowNode u, v;

    String name;

    boolean expandOnEmbed;

    ObjectArray<String> handleArray;

    /**
     * Default constructor.
     */
    public HandleEdge(FlowNode u, FlowNode v) {

        this.u = u;
        this.v = v;

        this.name = "";
        this.expandOnEmbed = false;

        //

        this.handleArray = null;
    }

    /**
     * Copies this edge.
     */
    @Override
    public HandleEdge clone() {

        final HandleEdge res;

        try {

            res = (HandleEdge) super.clone();

        } catch (CloneNotSupportedException e) {

            throw new RuntimeException(e);
        }

        res.setU(null);
        res.setV(null);

        res.handleArray = (this.handleArray != null) ? this.handleArray.clone() : this.handleArray;

        return res;
    }

    /**
     * Gets the handle information.
     */
    public ObjectArray<String> getHandleInformation() {
        return this.handleArray;
    }

    /**
     * Sets the handle information.
     */
    public HandleEdge setHandleInformation(ObjectArray<String> handleArray) {

        this.handleArray = handleArray;

        return this;
    }

    /**
     * Checks whether this edge should be expanded into multiple edges on subflow embedding.
     */
    public boolean isExpandOnEmbed() {
        return this.expandOnEmbed;
    }

    /**
     * Sets whether this edge should be expanded into multiple edges on subflow embedding.
     */
    public HandleEdge setExpandOnEmbed(boolean expandOnEmbed) {

        this.expandOnEmbed = expandOnEmbed;

        return this;
    }

    public FlowEdgeType getType() {
        return HANDLE;
    }

    public String getName() {
        return this.name;
    }

    public HandleEdge setName(String name) {

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
        return new OutputHandleResource(this.name);
    }

    public FlowNode getV() {
        return this.v;
    }

    public void setV(FlowNode v) {
        this.v = v;
    }

    public Resource createVResource() {
        return new InputHandleResource(this.name, this.handleArray);
    }

    public void generate() {
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
                color = DARK_GREEN;
                break;

            default:
                color = BLACK;
                break;
            }
        }

        f.format("%n\tnode_%d -> node_%d [%n", getU().getOrder(), getV().getOrder());
        f.format("\t\tstyle = \"solid\",%n");
        f.format("\t\tcolor = \"#%s\"%n", color);
        f.format("\t];%n");
    }
}
