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

package ex;

import static org.dapper.Constants.LOCAL;
import static org.dapper.Constants.REMOTE;

import java.io.File;
import java.util.List;

import org.dapper.server.flow.DummyEdge;
import org.dapper.server.flow.Flow;
import org.dapper.server.flow.FlowBuilder;
import org.dapper.server.flow.FlowEdge;
import org.dapper.server.flow.FlowNode;
import org.dapper.server.flow.HandleEdge;
import org.dapper.server.flow.StreamEdge;
import org.dapper.ui.Program;

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

    @Override
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
     * Creates a human-readable description of this {@link FlowBuilder}.
     */
    @Override
    public String toString() {
        return "Merge Sort Test";
    }
}
