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
import static dapper.Constants.REMOTE;

import java.io.File;
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
 * A demonstration of distributed merge sort.
 * 
 * @author Roy Liu
 */
@Program(arguments = {
//
        "depth", //
        "path", //
        "in", //
        "input_size", //
        "out" //
})
public class MergeSortTest implements FlowBuilder {

    /**
     * The line length.
     */
    final public static int LINE_LENGTH = 1024;

    final int inputSize, depth;
    final File pathDir, inFile, outFile;

    /**
     * Default constructor.
     */
    public MergeSortTest(String[] args) {

        this.depth = Integer.parseInt(args[0]);
        this.pathDir = new File(args[1]);
        this.inFile = new File(args[2]);
        this.inputSize = Integer.parseInt(args[3]);
        this.outFile = new File(args[4]);
    }

    public void build(Flow flow, //
            List<FlowEdge> inEdges, //
            List<FlowNode> outNodes) {

        FlowNode createSortFileNode = new FlowNode("ex.CreateSortFile") //
                .setDomainPattern(LOCAL) //
                .setParameters(String.format("<file>%s</file><lines>%d</lines>", //
                        this.inFile.getAbsolutePath(), //
                        this.inputSize));

        FlowNode uploadNode = new FlowNode("ex.Upload") //
                .setDomainPattern(LOCAL) //
                .setParameters(this.inFile.getAbsolutePath());

        FlowNode splitNode = new FlowNode("ex.Split") //
                .setDomainPattern(REMOTE) //
                .setParameters(this.pathDir.getAbsolutePath());

        FlowNode sortNode = new FlowNode("ex.Sort") //
                .setDomainPattern(REMOTE);

        FlowNode mergeNode = new FlowNode("ex.Merge") //
                .setDomainPattern(REMOTE);

        FlowNode downloadNode = new FlowNode("ex.Download") //
                .setDomainPattern(LOCAL) //
                .setParameters(this.outFile.getAbsolutePath());

        FlowNode cleanupNode = new FlowNode("ex.Cleanup") //
                .setDomainPattern(REMOTE);

        FlowNode[] nodes = new FlowNode[(1 << this.depth) - 1];

        flow.add(createSortFileNode);
        flow.add(uploadNode, new DummyEdge(createSortFileNode, uploadNode));
        flow.add(splitNode, new StreamEdge(uploadNode, splitNode).setInverted(true));

        for (int i = 0, n = 1 << (this.depth - 1); i < n; i++) {

            nodes[i] = sortNode.clone();

            flow.add(nodes[i], //
                    new HandleEdge(splitNode, nodes[i]).setName("to_sort"));
        }

        for (int i = this.depth - 2, count = 1 << (this.depth - 1), offset = 0; i >= 0; i--) {

            for (int j = count, m = count + (1 << i); j < m; j++, count++, offset++) {

                nodes[j] = mergeNode.clone();

                flow.add(nodes[j], //
                        new StreamEdge(nodes[((j - count) + offset) << 1], nodes[j]), //
                        new StreamEdge(nodes[(((j - count) + offset) << 1) + 1], nodes[j]));
            }
        }

        flow.add(downloadNode, new StreamEdge(nodes[(1 << this.depth) - 2], downloadNode));
        flow.add(cleanupNode, //
                new DummyEdge(downloadNode, cleanupNode), //
                new HandleEdge(splitNode, cleanupNode).setName("to_cleanup"));
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Merge Sort Test";
    }
}
