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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.server.flow.HandleEdge;
import dapper.server.flow.StreamEdge;
import dapper.ui.Program;

/**
 * A demonstration of simple Dapper functionality.
 * 
 * @author Roy Liu
 */
@Program
public class SimpleTest implements FlowBuilder {

    /**
     * The static {@link Logger} instance.
     */
    final static protected Logger Log = LoggerFactory.getLogger(SimpleTest.class.getName());

    /**
     * Default constructor.
     */
    public SimpleTest(String[] args) {
    }

    public void build(Flow flow, //
            List<FlowEdge> inEdges, //
            List<FlowNode> outNodes) {

        flow.setAttachment(flow.toString());

        FlowNode dn = new FlowNode("ex.Debug") //
                .setDomainPattern(LOCAL);

        FlowNode a = dn.clone().setName("a").setAttachment("a");
        FlowNode b = dn.clone().setName("b").setAttachment("b");
        FlowNode c = dn.clone().setName("c").setAttachment("c");
        FlowNode d = dn.clone().setName("d").setAttachment("d");
        FlowNode e = dn.clone().setName("e").setAttachment("e");
        FlowNode f = dn.clone().setName("f").setAttachment("f");
        FlowNode g = dn.clone().setName("g").setAttachment("g");

        flow.add(a);
        flow.add(b);
        flow.add(new StreamEdge(a, b));
        flow.add(c, new HandleEdge(a, c));
        flow.add(d, new HandleEdge(a, d), new HandleEdge(b, d));
        flow.add(e, new HandleEdge(b, e));
        flow.add(f, new HandleEdge(c, f), new HandleEdge(d, f));
        flow.add(g, new HandleEdge(d, g), new HandleEdge(e, g));
        flow.add(new StreamEdge(f, g));
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Simple Test";
    }
}
