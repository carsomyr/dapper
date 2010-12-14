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

import static org.dapper.codelet.Resource.ResourceType.INPUT_HANDLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.shared.array.ObjectArray;
import org.shared.util.Control;
import org.w3c.dom.Node;

/**
 * A subclass of {@link AbstractHandleResource} for input handles.
 * 
 * @author Roy Liu
 */
public class InputHandleResource extends AbstractHandleResource<InputHandleResource> {

    /**
     * Default constructor.
     */
    public InputHandleResource(String name, ObjectArray<String> handleArray) {
        super(name, handleArray);
    }

    /**
     * Alternate constructor.
     */
    public InputHandleResource(Node node) {
        super(node);
    }

    /**
     * Attempts to derive an {@link InputStream} from {@link #getFile()}.
     */
    @Override
    public InputStream getInputStream() {

        try {

            return new FileInputStream(getFile());

        } catch (FileNotFoundException e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Attempts to interpret the {@code 0}th handle as a file pathname.
     */
    public File getFile() {

        Control.checkTrue(this.nEntries > 0, //
                "Handles collection must be nonempty");

        return new File(this.handleArray.get(0, 0));
    }

    @Override
    public ResourceType getType() {
        return INPUT_HANDLE;
    }
}
