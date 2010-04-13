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

package ex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.w3c.dom.Node;

import shared.util.Control;
import dapper.codelet.Codelet;
import dapper.codelet.Resource;

/**
 * A {@link Codelet} that downloads its data from remote machines.
 * 
 * @author Roy Liu
 */
public class Download implements Codelet {

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        InputStream in = inResources.get(0).getInputStream();
        Control.transfer(in, new File(parameters.getTextContent()));
        Control.close(in);
    }

    /**
     * Default constructor.
     */
    public Download() {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Download";
    }
}
