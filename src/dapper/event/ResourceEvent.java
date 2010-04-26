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

package dapper.event;

import static dapper.event.ControlEvent.ControlEventType.RESOURCE;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.event.Source;
import shared.util.Control;
import dapper.codelet.ParameterMetadata;
import dapper.codelet.Resource;
import dapper.codelet.Resource.ResourceType;

/**
 * A subclass of {@link ControlEvent} for carrying {@link Resource} information.
 * 
 * @author Roy Liu
 */
public class ResourceEvent extends ControlEvent implements ParameterMetadata {

    final List<Resource> in, out;
    final String className;
    final Node parameters;

    /**
     * Default constructor.
     */
    public ResourceEvent(List<Resource> in, List<Resource> out, //
            String className, Node parameters, Source<ControlEvent, SourceType> source) {
        super(RESOURCE, source);

        this.in = in;
        this.out = out;

        this.className = className;

        this.parameters = parameters;
    }

    public Node getParameters() {
        return this.parameters;
    }

    /**
     * Gets the class name.
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Gets the input {@link Resource}s.
     */
    public List<Resource> getIn() {
        return this.in;
    }

    /**
     * Gets the output {@link Resource}s.
     */
    public List<Resource> getOut() {
        return this.out;
    }

    /**
     * Alternate constructor.
     */
    public ResourceEvent(Node contentNode, Source<ControlEvent, SourceType> source) {
        super(RESOURCE, source);

        ArrayList<List<Resource>> res = new ArrayList<List<Resource>>();

        NodeList l1 = contentNode.getChildNodes();

        Node classNameNode = l1.item(0);
        Control.checkTrue(classNameNode.getNodeName().equals("class_name"));

        Node parametersNode = l1.item(1);
        Control.checkTrue(parametersNode.getNodeName().equals("parameters"));

        Node resourcesNode = l1.item(2);
        Control.checkTrue(resourcesNode.getNodeName().equals("resources"));

        this.className = classNameNode.getTextContent();

        this.parameters = parametersNode;

        NodeList l2 = resourcesNode.getChildNodes();

        for (int i = 0; i < 2; i++) {

            List<Resource> list = new ArrayList<Resource>();

            NodeList l3 = l2.item(i).getChildNodes();

            for (int j = 0, m = l3.getLength(); j < m; j++) {
                list.add(ResourceType.parse(l3.item(j)));
            }

            res.add(list);
        }

        this.in = res.get(0);
        this.out = res.get(1);
    }

    @Override
    protected void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        ArrayList<List<Resource>> res = new ArrayList<List<Resource>>();

        res.add(this.in);
        res.add(this.out);

        contentNode.appendChild(doc.createElement("class_name")) //
                .setTextContent(this.className);

        // Adopt the parameters node into the current document.
        contentNode.appendChild(doc.importNode(this.parameters, true));

        Node node1 = contentNode.appendChild(doc.createElement("resources"));

        String[] tags1 = new String[] { "resource_in", "resource_out" };

        for (int i = 0; i < 2; i++) {

            Node node2 = doc.createElement(tags1[i]);

            for (Resource resource : res.get(i)) {

                Node resourceNode = doc.createElement("resource");

                resourceNode.appendChild(doc.createElement("type")).setTextContent( //
                        resource.getType().toString());

                resource.getContents(resourceNode.appendChild(doc.createElement("content")));

                node2.appendChild(resourceNode);
            }

            node1.appendChild(node2);
        }
    }
}
