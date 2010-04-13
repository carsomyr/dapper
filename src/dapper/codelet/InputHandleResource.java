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

import static dapper.codelet.Resource.ResourceType.INPUT_HANDLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.w3c.dom.Node;

import shared.array.ObjectArray;
import shared.util.Control;

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

        Control.checkTrue(this.nentries > 0, //
                "Handles collection must be non-empty");

        return new File(this.handleArray.get(0, 0));
    }

    public ResourceType getType() {
        return INPUT_HANDLE;
    }
}
