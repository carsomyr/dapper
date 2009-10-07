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

package ex;

import static dapper.Constants.LOCAL;

import java.util.List;

import dapper.server.flow.DummyEdge;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.server.flow.HandleEdge;
import dapper.server.flow.StreamEdge;
import dapper.ui.Program;

/**
 * A demonstration of complex Dapper functionality.
 * 
 * @author Roy Liu
 */
@Program
public class ComplexTest implements FlowBuilder {

    /**
     * Default constructor.
     */
    public ComplexTest(String[] args) {
    }

    public void build(Flow flow, //
            List<FlowEdge> inEdges, //
            List<FlowNode> outNodes) {

        FlowNode dn = new FlowNode("ex.Debug") //
                .setDomainPattern(LOCAL);
        FlowNode cn = new FlowNode("ex.Create") //
                .setDomainPattern(LOCAL);

        FlowNode a = dn.clone();
        FlowNode b = cn.clone();
        FlowNode c = new FlowNode("ex.FanSubflow") //
                .setDomainPattern(LOCAL);
        FlowNode d = dn.clone();
        FlowNode e = dn.clone();
        FlowNode f = dn.clone();
        FlowNode g = new FlowNode("ex.Error") //
                .setDomainPattern(LOCAL);

        flow.add(a);
        flow.add(b);
        flow.add(e, new HandleEdge(a, e));
        flow.add(c, new HandleEdge(b, c).setExpandOnEmbed(true), new StreamEdge(a, c));
        flow.add(d, new DummyEdge(c, d));
        flow.add(f, new HandleEdge(d, f));
        flow.add(g);
        flow.add(new StreamEdge(e, g).setInverted(true));
        flow.add(new StreamEdge(f, g).setInverted(true));
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Complex Test";
    }
}
