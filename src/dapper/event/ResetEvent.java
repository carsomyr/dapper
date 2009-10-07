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

import static dapper.event.ControlEvent.ControlEventType.RESET;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.event.Source;
import shared.parallel.Handle;
import dapper.DapperException;

/**
 * A subclass of {@link ControlEvent} for resetting the server/client to an inactive state.
 * 
 * @author Roy Liu
 */
public class ResetEvent extends ControlEvent implements Handle<Object> {

    final DapperException error;

    Object tag;

    /**
     * Default constructor.
     */
    public ResetEvent(DapperException exception, Source<ControlEvent, SourceType> source) {
        super(RESET, source);

        this.error = exception;

        this.tag = null;
    }

    /**
     * Alternate constructor.
     */
    public ResetEvent(String message, StackTraceElement[] stackTrace, Source<ControlEvent, SourceType> source) {
        super(RESET, source);

        this.error = new DapperException(message);
        this.error.setStackTrace(stackTrace);

        this.tag = null;
    }

    /**
     * Alternate constructor.
     */
    public ResetEvent(Node node, Source<ControlEvent, SourceType> source) {
        super(RESET, source);

        NodeList l1 = node.getChildNodes();

        this.error = new DapperException(l1.item(0).getTextContent());

        NodeList l2 = l1.item(1).getChildNodes();

        ArrayList<StackTraceElement> tmpList = new ArrayList<StackTraceElement>();

        for (int i = 0, n = l2.getLength(); i < n; i++) {

            NodeList l3 = l2.item(i).getChildNodes();

            tmpList.add(new StackTraceElement( //
                    l3.item(0).getTextContent(), //
                    l3.item(1).getTextContent(), //
                    l3.item(2).getTextContent(), //
                    Integer.parseInt(l3.item(3).getTextContent())));
        }

        this.error.setStackTrace(tmpList.toArray(new StackTraceElement[] {}));
    }

    /**
     * Gets the cause of the reset.
     */
    public DapperException getError() {
        return this.error;
    }

    @Override
    protected void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.createElement("message")) //
                .setTextContent(String.valueOf(this.error.getMessage()));

        Node n1 = contentNode.appendChild(doc.createElement("elements"));

        for (StackTraceElement element : this.error.getStackTrace()) {

            Node n2 = n1.appendChild(doc.createElement("element"));

            n2.appendChild(doc.createElement("class_name")) //
                    .setTextContent(element.getClassName());
            n2.appendChild(doc.createElement("method_name")) //
                    .setTextContent(element.getMethodName());
            n2.appendChild(doc.createElement("file_name")) //
                    .setTextContent(element.getFileName());
            n2.appendChild(doc.createElement("line_number")) //
                    .setTextContent(Integer.toString(element.getLineNumber()));
        }
    }

    public Object get() {
        return this.tag;
    }

    public void set(Object tag) {
        this.tag = tag;
    }
}
