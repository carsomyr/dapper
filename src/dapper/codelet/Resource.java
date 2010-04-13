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

package dapper.codelet;

import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.event.EnumType;

/**
 * Defines a {@link Codelet} resource descriptor.
 * 
 * @apiviz.owns dapper.codelet.Resource.ResourceType
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
