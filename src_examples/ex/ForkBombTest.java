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

import static dapper.Constants.LOCAL;

import java.util.List;

import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.ui.Program;

/**
 * A demonstration of a subflow that embeds itself repeatedly. Designed to black box and stress test the Dapper server
 * logic. Named after the traditional notion of a process "fork bomb".
 * 
 * @author Roy Liu
 */
@Program
public class ForkBombTest implements FlowBuilder {

    /**
     * Default constructor.
     */
    public ForkBombTest(String[] args) {
    }

    public void build(Flow flow, //
            List<FlowEdge> inEdges, //
            List<FlowNode> outNodes) {
        flow.add(new FlowNode("ex.ForkBomb")) //
                .setDomainPattern(LOCAL);
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Fork Bomb Test";
    }
}
