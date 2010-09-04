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
    @Override
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
     * Creates a human-readable description of this {@link Codelet}.
     */
    @Override
    public String toString() {
        return "Broadcast";
    }
}
