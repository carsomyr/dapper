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

package dapper.event;

import static dapper.event.ControlEvent.ControlEventType.RESET;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.event.Source;
import shared.parallel.Handle;
import shared.util.Control;
import dapper.DapperException;

/**
 * A subclass of {@link ControlEvent} for resetting the server/client to an inactive state.
 * 
 * @author Roy Liu
 */
public class ResetEvent extends ControlEvent implements Handle<Object> {

    /**
     * An empty array of {@link StackTraceElement}s.
     */
    final protected static StackTraceElement[] EmptyStackTraceElements = new StackTraceElement[] {};

    final DapperException exception;

    Object tag;

    /**
     * Default constructor.
     */
    public ResetEvent(String message, Throwable t, Source<ControlEvent, SourceType> source) {
        super(RESET, source);

        Control.checkTrue(t != null, //
                "Please provide the cause of this event");

        this.exception = new DapperException(message, t);

        this.tag = null;
    }

    /**
     * Alternate constructor.
     */
    public ResetEvent(Node node, Source<ControlEvent, SourceType> source) {
        super(RESET, source);

        NodeList l1 = node.getChildNodes();

        String message = l1.item(0).getTextContent();
        String causeClassName = l1.item(1).getTextContent();
        String causeMessage = l1.item(2).getTextContent();

        Throwable cause = null;

        // Attempt to load the appropriate Throwable class.
        try {

            Class<?> clazz = Class.forName(causeClassName, true, //
                    Thread.currentThread().getContextClassLoader());

            if (Throwable.class.isAssignableFrom(clazz)) {
                cause = (Throwable) clazz.getConstructor(String.class).newInstance(causeMessage);
            }

        } catch (Exception e) {

            // Loading failed; pass through.
        }

        if (cause == null) {
            cause = new RuntimeException(causeMessage);
        }

        NodeList l2 = l1.item(3).getChildNodes();

        ArrayList<StackTraceElement> tmpList = new ArrayList<StackTraceElement>();

        for (int i = 0, n = l2.getLength(); i < n; i++) {

            NodeList l3 = l2.item(i).getChildNodes();

            tmpList.add(new StackTraceElement( //
                    l3.item(0).getTextContent(), //
                    l3.item(1).getTextContent(), //
                    l3.item(2).getTextContent(), //
                    Integer.parseInt(l3.item(3).getTextContent())));
        }

        cause.setStackTrace(tmpList.toArray(EmptyStackTraceElements));

        this.exception = new DapperException(message, cause);
    }

    /**
     * Gets the cause of the reset.
     */
    public DapperException getException() {
        return this.exception;
    }

    @Override
    protected void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.createElement("message")) //
                .setTextContent(String.valueOf(this.exception.getMessage()));

        Throwable cause = this.exception.getCause();

        contentNode.appendChild(doc.createElement("cause_class_name")) //
                .setTextContent(String.valueOf(cause.getClass().getName()));
        contentNode.appendChild(doc.createElement("cause_message")) //
                .setTextContent(String.valueOf(cause.getMessage()));

        Node n1 = contentNode.appendChild(doc.createElement("cause_elements"));

        for (StackTraceElement element : cause.getStackTrace()) {

            Node n2 = n1.appendChild(doc.createElement("cause_element"));

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
