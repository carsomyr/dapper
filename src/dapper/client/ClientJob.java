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

package dapper.client;

import static dapper.Constants.REQUEST_TIMEOUT_MILLIS;
import static dapper.codelet.Resource.ResourceType.OUTPUT_HANDLE;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import shared.event.Source;
import shared.metaclass.RegistryClassLoader;
import shared.metaclass.ResourceRegistry;
import shared.net.SynchronousManagedConnection;
import shared.parallel.Handle;
import shared.util.Control;
import shared.util.CoreThread;
import shared.util.RequestFuture;
import dapper.AsynchronousBase;
import dapper.codelet.Codelet;
import dapper.codelet.CodeletUtilities;
import dapper.codelet.DataService;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;
import dapper.codelet.StreamResource;
import dapper.event.ControlEvent;
import dapper.event.DataEvent;
import dapper.event.ExecuteAckEvent;
import dapper.event.ResetEvent;
import dapper.event.ResourceEvent;
import dapper.event.SourceType;
import dapper.server.flow.EmbeddingCodelet;
import dapper.server.flow.FlowNode;

/**
 * A client job thread class.
 * 
 * @author Roy Liu
 */
public class ClientJob extends CoreThread implements Closeable, DataService {

    final ResourceEvent event;
    final AsynchronousBase base;

    final Map<String, StreamResource<?>> remaining;
    final Set<StreamResource<?>> connectResources;
    final Source<ControlEvent, SourceType> callback;

    Map<String, RequestFuture<byte[]>> pending;

    /**
     * Default constructor.
     */
    public ClientJob(ResourceEvent event, AsynchronousBase base, Source<ControlEvent, SourceType> callback) {
        super("Job Thread");

        this.event = event;
        this.base = base;

        this.pending = new HashMap<String, RequestFuture<byte[]>>();
        this.remaining = new HashMap<String, StreamResource<?>>();
        this.connectResources = new HashSet<StreamResource<?>>();

        Set<Resource> allResources = new HashSet<Resource>();
        allResources.addAll(this.event.getIn());
        allResources.addAll(this.event.getOut());

        for (Resource resource : allResources) {

            switch (resource.getType()) {

            case INPUT_STREAM:
            case OUTPUT_STREAM:

                StreamResource<?> connectResource = (StreamResource<?>) resource;

                this.remaining.put(connectResource.getIdentifier(), connectResource);

                // This stream requires connecting to.
                if (connectResource.getAddress() != null) {
                    this.connectResources.add(connectResource);
                }

                break;
            }
        }

        this.callback = callback;
    }

    /**
     * Registers an input/output stream.
     */
    @SuppressWarnings("unchecked")
    protected void registerStream(String identifier, SynchronousManagedConnection connection) {

        StreamResource<?> res = this.remaining.remove(identifier);

        if (res != null) {

            switch (res.getType()) {

            case INPUT_STREAM:
                ((Handle<InputStream>) res).set(connection.getInputStream());
                res.setAddress(connection.getRemoteAddress());
                break;

            case OUTPUT_STREAM:
                ((Handle<OutputStream>) res).set(connection.getOutputStream());
                res.setAddress(connection.getRemoteAddress());
                break;

            // Huh? How could this even happen?
            default:
                Control.close(connection);
                break;
            }

        } else {

            // If not found, then the connection must be erroneous.
            Control.close(connection);
        }
    }

    /**
     * Gets the set of {@link Resource}s that require connecting to.
     */
    public Set<StreamResource<?>> getConnectResources() {
        return this.connectResources;
    }

    /**
     * Gets whether this job is ready to execute.
     */
    public boolean isReady() {
        return this.remaining.isEmpty();
    }

    /**
     * Registers data requested from the server.
     */
    public void registerData(String pathname, byte[] data) {

        synchronized (this) {

            RequestFuture<byte[]> rf = this.pending.remove(pathname);

            if (rf != null) {
                rf.set(data);
            }
        }
    }

    /**
     * Closes all underlying {@link InputStream}s and {@link OutputStream}s.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void close() {

        Set<Resource> allResources = new HashSet<Resource>();
        allResources.addAll(this.event.getIn());
        allResources.addAll(this.event.getOut());

        for (Resource resource : allResources) {

            // Close any streams we encounter.
            switch (resource.getType()) {

            case INPUT_STREAM:
            case OUTPUT_STREAM:
                Control.close(((StreamResource<? extends Closeable>) resource).get());
                break;
            }
        }

        synchronized (this) {

            for (RequestFuture<byte[]> future : this.pending.values()) {
                future.setException(new RuntimeException("The client job has been stopped"));
            }

            this.pending = null;
        }
    }

    @Override
    protected void runUnchecked() throws Exception {

        RegistryClassLoader rcl = new RegistryClassLoader();
        rcl.addRegistry(new ResourceRegistry() {

            @Override
            public URL getResource(String pathname) {
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Enumeration<URL> getResources(String pathname) {
                return Collections.enumeration(Collections.EMPTY_LIST);
            }

            @Override
            public InputStream getResourceAsStream(String pathname) {

                byte[] data = getData(String.format("cp:%s", pathname));
                return (data != null) ? new ByteArrayInputStream(data) : null;
            }
        });

        Codelet codelet = (Codelet) rcl.loadClass(this.event.getClassName()).newInstance();

        List<Resource> outResources = this.event.getOut();

        CodeletUtilities.setDataService(this);

        try {

            codelet.run( //
                    Collections.unmodifiableList(this.event.getIn()), //
                    Collections.unmodifiableList(outResources), //
                    this.event.getParameters());

        } finally {

            CodeletUtilities.setDataService(null);
        }

        Node embeddingParameters = (codelet instanceof EmbeddingCodelet) ? ((EmbeddingCodelet) codelet)
                .getEmbeddingParameters() : null;
        embeddingParameters = (embeddingParameters == null) ? FlowNode.EmptyParameters : embeddingParameters;

        Control.checkTrue(embeddingParameters.getNodeName().equals("parameters"), //
                "Invalid parameters node");

        Document doc = Control.createDocument();
        Node edgeParameters = doc.createElement("edge_parameters");

        for (Resource outResource : outResources) {

            Node edgeParameterNode = edgeParameters.appendChild(doc.createElement("edge_parameter"));

            if (outResource.getType() == OUTPUT_HANDLE) {
                ((OutputHandleResource) outResource).getContents(edgeParameterNode);
            }
        }

        ExecuteAckEvent executeAckEvent = new ExecuteAckEvent(embeddingParameters, edgeParameters, this.callback);
        executeAckEvent.set(this);

        this.callback.onLocal(executeAckEvent);
    }

    @Override
    protected void runCatch(Throwable t) {

        ResetEvent resetEvent = new ResetEvent("Execution encountered an unexpected exception", t, this.callback);
        resetEvent.set(this);

        this.callback.onLocal(resetEvent);
    }

    @Override
    public byte[] getData(String pathname) {

        RequestFuture<byte[]> rf = new RequestFuture<byte[]>();

        synchronized (this) {

            Control.checkTrue(this.pending != null && !this.pending.containsKey(pathname), //
                    "Request conditions violated");

            this.pending.put(pathname, rf);
        }

        this.callback.onLocal(new DataEvent(pathname, new byte[] {}, ClientJob.this.callback));

        try {

            return rf.get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        } catch (Exception e) {

            return null;
        }
    }
}
