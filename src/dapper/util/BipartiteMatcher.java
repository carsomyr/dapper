/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2009 Roy Liu, The Regents of the University of California <br />
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
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>. <br />
 * <br />
 * This file is derived from previous work, whose license terms are attached. <br />
 * <br />
 * Copyright (c) 2007, 2008 Massachusetts Institute of Technology <br />
 * Copyright (c) 2005, 2006 Regents of the University of California <br />
 * All rights reserved. <br />
 * <br />
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>Neither the name of the University of California, Berkeley nor the name of the Massachusetts Institute of
 * Technology nor the names of its contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission.</li>
 * </ul>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dapper.util;

import java.util.ArrayList;
import java.util.List;

/**
 * An engine for finding the maximum-weight matching in a complete bipartite graph. Suppose we have two sets <i>S</i>
 * and <i>T</i>, both of size <i>n</i>. For each <i>i</i> in <i>S</i> and <i>j</i> in <i>T</i>, we have a weight
 * <i>w<sub>ij</sub></i>. A perfect matching <i>X</i> is a subset of <i>S</i> x <i>T</i> such that each <i>i</i> in
 * <i>S</i> occurs in exactly one element of <i>X</i>, and each <i>j</i> in <i>T</i> occurs in exactly one element of
 * <i>X</i>. Thus, <i>X</i> can be thought of as a one-to-one function from <i>S</i> to <i>T</i>. The weight of <i>X</i>
 * is the sum, over (<i>i</i>, <i>j</i>) in <i>X</i>, of <i>w<sub>ij</sub></i>. A BipartiteMatcher takes the number
 * <i>n</i> and the weights <i>w<sub>ij</sub></i>, and finds a perfect matching of maximum weight. It uses the Hungarian
 * algorithm of Kuhn (1955), as improved and presented by E. L. Lawler in his book <cite>Combinatorial Optimization:
 * Networks and Matroids</cite> (Holt, Rinehart and Winston, 1976, p. 205-206). The running time is
 * O(<i>n</i><sup>3</sup>). The weights can be any finite real numbers; Lawler's algorithm assumes positive weights, so
 * if necessary we add a constant <i>c</i> to all the weights before running the algorithm. This increases the weight of
 * every perfect matching by <i>nc</i>, which doesn't change which perfect matchings have maximum weight. If a weight is
 * set to Double.NEGATIVE_INFINITY, then the algorithm will behave as if that edge were not in the graph. If all the
 * edges incident on a given node have weight Double.NEGATIVE_INFINITY, then the final result will not be a perfect
 * matching, and an exception will be thrown.
 * 
 * @author Brian Milch
 */
public class BipartiteMatcher {

    /**
     * Creates a BipartiteMatcher without specifying the graph size. Calling any other method before calling reset will
     * yield an IllegalStateException.
     */
    public BipartiteMatcher() {
        this.n = -1;
    }

    /**
     * Creates a BipartiteMatcher and prepares it to run on an n x n graph. All the weights are initially set to {@code
     * 0}.
     */
    public BipartiteMatcher(int n) {
        reset(n);
    }

    /**
     * Resets the BipartiteMatcher to run on an n x n graph. The weights are all reset to {@code 0}.
     */
    public void reset(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Negative num nodes: " + n);
        }
        this.n = n;

        this.weights = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                this.weights[i][j] = 0;
            }
        }
        this.minWeight = 0;
        this.maxWeight = Double.NEGATIVE_INFINITY;

        this.sMatches = new int[n];
        this.tMatches = new int[n];
        this.sLabels = new int[n];
        this.tLabels = new int[n];
        this.u = new double[n];
        this.v = new double[n];
        this.pi = new double[n];
    }

    /**
     * Sets the weight w<sub>ij</sub> to the given value w.
     * 
     * @throws IllegalArgumentException
     *             if i or j is outside the range [0, n).
     */
    public void setWeight(int i, int j, double w) {
        if (this.n == -1) {
            throw new IllegalStateException("Graph size not specified");
        }
        if ((i < 0) || (i >= this.n)) {
            throw new IllegalArgumentException("i-value out of range: " + i);
        }
        if ((j < 0) || (j >= this.n)) {
            throw new IllegalArgumentException("j-value out of range: " + j);
        }
        if (Double.isNaN(w)) {
            throw new IllegalArgumentException("Illegal weight: " + w);
        }

        this.weights[i][j] = w;
        if ((w > Double.NEGATIVE_INFINITY) && (w < this.minWeight)) {
            this.minWeight = w;
        }
        if (w > this.maxWeight) {
            this.maxWeight = w;
        }
    }

    /**
     * Returns a maximum-weight perfect matching relative to the weights specified with setWeight. The matching is
     * represented as an array arr of length n, where arr[i] = j if (i,j) is in the matching.
     */
    public int[] getMatching() {
        if (this.n == -1) {
            throw new IllegalStateException("Graph size not specified");
        }
        if (this.n == 0) {
            return new int[0];
        }
        ensurePositiveWeights();

        // Step 0: Initialization
        this.eligibleS.clear();
        this.eligibleT.clear();
        for (int i = 0; i < this.n; i++) {
            this.sMatches[i] = -1;
            this.tMatches[i] = -1;

            this.u[i] = this.maxWeight; // ambiguous on p. 205 of Lawler, but see p. 202
            this.v[i] = 0;
            this.pi[i] = Double.POSITIVE_INFINITY;

            // this is really first run of Step 1.0
            this.sLabels[i] = EMPTY_LABEL;
            this.eligibleS.add(new Integer(i));

            this.tLabels[i] = NO_LABEL;
        }

        while (true) {
            // Augment the matching until we can't augment any more given the
            // current settings of the dual variables.
            while (true) {
                // Steps 1.1-1.4: Find an augmenting path
                int lastNode = findAugmentingPath();
                if (lastNode == -1) {
                    break; // no augmenting path
                }

                // Step 2: Augmentation
                flipPath(lastNode);
                for (int i = 0; i < this.n; i++) {
                    this.pi[i] = Double.POSITIVE_INFINITY;
                    this.sLabels[i] = NO_LABEL;
                    this.tLabels[i] = NO_LABEL;
                }

                // This is Step 1.0
                this.eligibleS.clear();
                for (int i = 0; i < this.n; i++) {
                    if (this.sMatches[i] == -1) {
                        this.sLabels[i] = EMPTY_LABEL;
                        this.eligibleS.add(new Integer(i));
                    }
                }

                this.eligibleT.clear();
            }

            // Step 3: Change the dual variables

            // delta1 = min_i u[i]
            double delta1 = Double.POSITIVE_INFINITY;
            for (int i = 0; i < this.n; i++) {
                if (this.u[i] < delta1) {
                    delta1 = this.u[i];
                }
            }

            // delta2 = min_{j : pi[j] > 0} pi[j]
            double delta2 = Double.POSITIVE_INFINITY;
            for (int j = 0; j < this.n; j++) {
                if ((this.pi[j] >= TOL) && (this.pi[j] < delta2)) {
                    delta2 = this.pi[j];
                }
            }

            if (delta1 < delta2) {
                // In order to make another pi[j] equal 0, we'd need to
                // make some u[i] negative.
                break; // we have a maximum-weight matching
            }

            changeDualVars(delta2);
        }

        int[] matching = new int[this.n];
        for (int i = 0; i < this.n; i++) {
            matching[i] = this.sMatches[i];
        }
        return matching;
    }

    /**
     * Tries to find an augmenting path containing only edges (i,j) for which u[i] + v[j] = weights[i][j]. If it
     * succeeds, returns the index of the last node in the path. Otherwise, returns -1. In any case, updates the labels
     * and pi values.
     */
    int findAugmentingPath() {
        while ((!this.eligibleS.isEmpty()) || (!this.eligibleT.isEmpty())) {
            if (!this.eligibleS.isEmpty()) {
                int i = (this.eligibleS.get(this.eligibleS.size() - 1)).intValue();
                this.eligibleS.remove(this.eligibleS.size() - 1);
                for (int j = 0; j < this.n; j++) {
                    // If pi[j] has already been decreased essentially
                    // to zero, then j is already labeled, and we
                    // can't decrease pi[j] any more. Omitting the
                    // pi[j] >= TOL check could lead us to relabel j
                    // unnecessarily, since the diff we compute on the
                    // next line may end up being less than pi[j] due
                    // to floating point imprecision.
                    if ((this.tMatches[j] != i) && (this.pi[j] >= TOL)) {
                        double diff = this.u[i] + this.v[j] - this.weights[i][j];
                        if (diff < this.pi[j]) {
                            this.tLabels[j] = i;
                            this.pi[j] = diff;
                            if (this.pi[j] < TOL) {
                                this.eligibleT.add(new Integer(j));
                            }
                        }
                    }
                }
            } else {
                int j = (this.eligibleT.get(this.eligibleT.size() - 1)).intValue();
                this.eligibleT.remove(this.eligibleT.size() - 1);
                if (this.tMatches[j] == -1) {
                    return j; // we've found an augmenting path
                }

                int i = this.tMatches[j];
                this.sLabels[i] = j;
                this.eligibleS.add(new Integer(i)); // ok to add twice
            }
        }

        return -1;
    }

    /**
     * Given an augmenting path ending at lastNode, "flips" the path. This means that an edge on the path is in the
     * matching after the flip if and only if it was not in the matching before the flip. An augmenting path connects
     * two unmatched nodes, so the result is still a matching.
     */
    void flipPath(int lastNode) {
        while (lastNode != EMPTY_LABEL) {
            int parent = this.tLabels[lastNode];

            // Add (parent, lastNode) to matching. We don't need to
            // explicitly remove any edges from the matching because:
            // * We know at this point that there is no i such that
            // sMatches[i] = lastNode.
            // * Although there might be some j such that tMatches[j] =
            // parent, that j must be sLabels[parent], and will change
            // tMatches[j] in the next time through this loop.
            this.sMatches[parent] = lastNode;
            this.tMatches[lastNode] = parent;

            lastNode = this.sLabels[parent];
        }
    }

    void changeDualVars(double delta) {
        for (int i = 0; i < this.n; i++) {
            if (this.sLabels[i] != NO_LABEL) {
                this.u[i] -= delta;
            }
        }

        for (int j = 0; j < this.n; j++) {
            if (this.pi[j] < TOL) {
                this.v[j] += delta;
            } else if (this.tLabels[j] != NO_LABEL) {
                this.pi[j] -= delta;
                if (this.pi[j] < TOL) {
                    this.eligibleT.add(new Integer(j));
                }
            }
        }
    }

    /**
     * Ensures that all weights are either Double.NEGATIVE_INFINITY, or strictly greater than zero.
     */
    private void ensurePositiveWeights() {
        // minWeight is the minimum non-infinite weight
        if (this.minWeight < TOL) {
            for (int i = 0; i < this.n; i++) {
                for (int j = 0; j < this.n; j++) {
                    this.weights[i][j] = this.weights[i][j] - this.minWeight + 1;
                }
            }

            this.maxWeight = this.maxWeight - this.minWeight + 1;
            this.minWeight = 1;
        }
    }

    /**
     * Tolerance for comparisons to zero, to account for floating-point imprecision. We consider a positive number to be
     * essentially zero if it is strictly less than TOL.
     */
    private static final double TOL = 1e-10;

    int n;

    double[][] weights;
    double minWeight;
    double maxWeight;

    // If (i, j) is in the mapping, then sMatches[i] = j and tMatches[j] = i.
    // If i is unmatched, then sMatches[i] = -1 (and likewise for tMatches).
    int[] sMatches;
    int[] tMatches;

    static final int NO_LABEL = -1;
    static final int EMPTY_LABEL = -2;

    int[] sLabels;
    int[] tLabels;

    double[] u;
    double[] v;

    double[] pi;

    List<Integer> eligibleS = new ArrayList<Integer>();
    List<Integer> eligibleT = new ArrayList<Integer>();
}
