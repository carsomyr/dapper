/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2010 Roy Liu, The Regents of the University of California <br />
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

package dapper.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import shared.util.Control;
import edu.uci.ics.jung.algorithms.flows.EdmondsKarpMaxFlow;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

/**
 * An implementation of {@link MatchingAlgorithm} that uses the <a
 * href="http://en.wikipedia.org/wiki/Edmonds%E2%80%93Karp_algorithm">Edmonds-Karp maximum flow</a> algorithm
 * underneath, as provided by the <a href="http://jung.sourceforge.net/">JUNG</a> project's {@link EdmondsKarpMaxFlow}
 * class.
 * 
 * @author Roy Liu
 */
public class MaximumFlowMatching implements MatchingAlgorithm {

    /**
     * Default constructor.
     */
    public MaximumFlowMatching() {
    }

    public <R extends Requirement<S>, S> Map<R, S> match(Collection<R> requirements, Collection<S> satisfiers) {

        ArrayList<R> rList = new ArrayList<R>();
        ArrayList<R> rListTrivial = new ArrayList<R>();

        // Consider only nontrivial requirements for matching.
        for (R requirement : requirements) {
            (!requirement.isTrivial() ? rList : rListTrivial).add(requirement);
        }

        List<S> sList = new ArrayList<S>(satisfiers);

        int nr = rList.size();
        int ns = sList.size();

        DirectedGraph<Integer, Integer> dag = new DirectedSparseGraph<Integer, Integer>();

        // Add vertices and edges.

        for (int i = 0, n = nr + ns; i < n; i++) {
            dag.addVertex(i);
        }

        int ie = 0;

        for (int ir = 0; ir < nr; ir++) {

            for (int is = 0; is < ns; is++) {

                if (rList.get(ir).isSatisfied(sList.get(is))) {
                    dag.addEdge(ie++, ir, is + nr);
                }
            }
        }

        int ne = ie;

        // Add the source, sink, and associated edges.

        dag.addVertex(nr + ns);
        dag.addVertex(nr + ns + 1);

        for (int ir = 0; ir < nr; ir++) {
            dag.addEdge(ie++, nr + ns, ir);
        }

        for (int is = 0; is < ns; is++) {
            dag.addEdge(ie++, is + nr, nr + ns + 1);
        }

        // Compute the maximum flow.

        Map<Integer, Number> edgeFlowMap = new HashMap<Integer, Number>();

        final int edgeFactoryOffset = ie;

        new EdmondsKarpMaxFlow<Integer, Integer>(dag, nr + ns, nr + ns + 1, //
                new Transformer<Integer, Number>() {

                    public Number transform(Integer input) {
                        return 1;
                    }
                }, //
                edgeFlowMap, //
                new Factory<Integer>() {

                    int ie = edgeFactoryOffset;

                    public Integer create() {
                        return this.ie++;
                    }
                } //
        ).evaluate();

        // Match requirements with satisfiers.

        Set<S> sRemaining = new HashSet<S>(sList);
        Map<R, S> res = new HashMap<R, S>();

        for (ie = 0; ie < ne; ie++) {

            if ((Integer) edgeFlowMap.get(ie) > 0) {

                R r = rList.get(dag.getSource(ie));
                S s = sList.get(dag.getDest(ie) - nr);

                sRemaining.remove(s);
                Control.checkTrue(res.put(r, s) == null, //
                        "Requirements must be unique under object equality");
            }
        }

        Iterator<R> rItr = rListTrivial.iterator();
        Iterator<S> sItr = sRemaining.iterator();

        for (int i = 0, n = Math.min(rListTrivial.size(), sRemaining.size()); i < n; i++) {
            Control.checkTrue(res.put(rItr.next(), sItr.next()) == null, //
                    "Requirements must be unique under object equality");
        }

        return res;
    }
}
