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

package org.dapper.codelet;

import java.io.InputStream;
import java.io.OutputStream;

import org.shared.event.EnumType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Defines a {@link Codelet} resource descriptor.
 * 
 * @apiviz.owns org.dapper.codelet.Resource.ResourceType
 * @author Roy Liu
 */
public interface Resource extends EnumType<Resource.ResourceType>, Nameable {

    /**
     * An enumeration of {@link Resource} types.
     */
    public enum ResourceType {

        /**
         * Indicates an {@link InputHandleResource}.
         */
        INPUT_HANDLE {

            @Override
            public Resource createResource(Node node) {
                return new InputHandleResource(node);
            }
        }, //

        /**
         * Indicates an {@link OutputHandleResource}.
         */
        OUTPUT_HANDLE {

            @Override
            public Resource createResource(Node node) {
                return new OutputHandleResource(node);
            }
        }, //

        /**
         * Indicates an {@link InputStream}.
         */
        INPUT_STREAM {

            @Override
            public Resource createResource(Node node) {
                return new StreamResource<InputStream>(this, node);
            }
        }, //

        /**
         * Indicates an {@link OutputStream}.
         */
        OUTPUT_STREAM {

            @Override
            public Resource createResource(Node node) {
                return new StreamResource<OutputStream>(this, node);
            }
        };

        /**
         * Creates a {@link Resource} from the given DOM {@link Node}.
         */
        abstract public Resource createResource(Node node);

        /**
         * Parses a {@link Resource} from the given DOM {@link Node}.
         */
        final public static Resource parse(Node node) {

            NodeList list = node.getChildNodes();
            return valueOf(list.item(0).getTextContent()).createResource(list.item(1));
        }
    }

    /**
     * Gets the {@link InputStream}.
     */
    public InputStream getInputStream();

    /**
     * Gets the {@link OutputStream}.
     */
    public OutputStream getOutputStream();

    /**
     * Gets the contents as a DOM {@link Node}.
     */
    public void getContents(Node contentNode);
}
