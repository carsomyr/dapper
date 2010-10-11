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
    final protected static StackTraceElement[] emptyStackTraceElements = new StackTraceElement[] {};

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

        cause.setStackTrace(tmpList.toArray(emptyStackTraceElements));

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

    @Override
    public Object get() {
        return this.tag;
    }

    @Override
    public void set(Object tag) {
        this.tag = tag;
    }
}
