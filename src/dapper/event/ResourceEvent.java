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
