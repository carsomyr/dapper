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
