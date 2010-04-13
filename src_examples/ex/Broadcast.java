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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Node;

import shared.util.Control;
import dapper.codelet.Codelet;
import dapper.codelet.CodeletUtilities;
import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;
import dapper.codelet.StreamResource;

/**
 * A {@link Codelet} for broadcasting data from in-{@link Resource}s to all corresponding out-{@link Resource}s sharing
 * the same name.
 * 
 * @author Roy Liu
 */
public class Broadcast implements Codelet {

    @SuppressWarnings("unchecked")
    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws IOException {

        Map<String, List<Resource>> inResourcesMap = CodeletUtilities.groupByName(inResources);

        for (Entry<String, List<Resource>> entry : inResourcesMap.entrySet()) {

            String name = entry.getKey();
            List<Resource> resources = entry.getValue();

            for (Resource resource : resources) {

                switch (resource.getType()) {

                case INPUT_HANDLE:

                    InputHandleResource ihr = (InputHandleResource) resource;

                    for (OutputHandleResource ohr : CodeletUtilities.filter(outResources, //
                            name, OutputHandleResource.class)) {
                        ohr.put(ihr.get());
                    }

                    break;

                case INPUT_STREAM:

                    final StreamResource<InputStream> isr = (StreamResource<InputStream>) resource;

                    for (StreamResource<OutputStream> osr : CodeletUtilities.filter(outResources, //
                            name, StreamResource.class)) {

                        InputStream in = isr.get();
                        OutputStream out = osr.get();

                        Control.transfer(in, out);

                        Control.close(in);
                        Control.close(out);
                    }

                    break;

                default:
                    throw new RuntimeException("Edge type not recognized");
                }
            }
        }
    }

    /**
     * Default constructor.
     */
    public Broadcast() {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Broadcast";
    }
}
