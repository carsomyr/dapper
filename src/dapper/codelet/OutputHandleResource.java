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

package dapper.codelet;

import static dapper.codelet.Resource.ResourceType.OUTPUT_HANDLE;

import org.w3c.dom.Node;

import shared.array.ObjectArray;
import shared.util.Control;

/**
 * A subclass of {@link AbstractHandleResource} for output handles.
 * 
 * @author Roy Liu
 */
public class OutputHandleResource extends AbstractHandleResource<OutputHandleResource> {

    /**
     * Default constructor.
     */
    public OutputHandleResource(String name) {
        super(name, new ObjectArray<String>(String.class, 0, 2));
    }

    /**
     * Alternate constructor.
     */
    public OutputHandleResource(Node node) {
        super(node);
    }

    /**
     * Creates a handle entry from the given handle and the "" stem.
     */
    public void put(String handle) {
        put(handle, "");
    }

    /**
     * Creates a handle entry from the given handle and stem.
     */
    public void put(String handle, String stem) {

        if (this.nentries == this.handleArray.size(0)) {
            this.handleArray = this.handleArray.map(new ObjectArray<String>(String.class, //
                    (this.nentries << 1) + 1, 2), //
                    0, 0, this.nentries, //
                    0, 0, 2);
        }

        this.handleArray.set(handle, this.nentries, 0);
        this.handleArray.set(stem, this.nentries, 1);
        this.nentries++;
    }

    /**
     * Appends the given {@link ObjectArray} of handle entries to the current entries.
     */
    public void put(ObjectArray<String> newEntries) {

        Control.checkTrue(newEntries.ndims() == 2 && newEntries.size(1) == 2, //
                "Invalid dimensions");

        int nnewEntries = newEntries.size(0);

        if (this.nentries + nnewEntries > this.handleArray.size(0)) {
            this.handleArray = this.handleArray.map( //
                    new ObjectArray<String>(String.class, (this.nentries + nnewEntries) << 1, 2), //
                    0, 0, this.nentries, //
                    0, 0, 2);
        }

        newEntries.map( //
                this.handleArray, //
                0, this.nentries, nnewEntries, //
                0, 0, 2);

        this.nentries += nnewEntries;
    }

    public ResourceType getType() {
        return OUTPUT_HANDLE;
    }
}
