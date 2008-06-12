package org.limewire.swarm.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ContentListener;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.file.FileCoordinator;

class SwarmerImpl implements Swarmer, HttpSwarmHandler<SwarmerImpl.SourceInfo> {
    
    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);
    
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    private final ConnectingIOReactor ioReactor;
    private final IOEventDispatch eventDispatch;
    private final AsyncNHttpClientHandler clientHandler;
    private final SwarmNHttpRequestExecutionHandlerImpl<SourceInfo> executionHandler;
    private final FileCoordinator fileCoordinator;
    private final SourceEventListener globalSourceEventListener;
    
    SwarmerImpl(
            FileCoordinator fileCoordinator,
            ConnectingIOReactor ioReactor,
            HttpParams params,
            SourceEventListener sourceEventListener) {
        
        this.fileCoordinator = fileCoordinator;
        this.ioReactor = ioReactor;
        if(sourceEventListener == null)
            this.globalSourceEventListener = NULL_LISTENER;
        else
            this.globalSourceEventListener = sourceEventListener;
        executionHandler = new SwarmNHttpRequestExecutionHandlerImpl<SourceInfo>();
        
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        httpproc.addInterceptor(new RequestConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        
        clientHandler = new AsyncNHttpClientHandler(
                httpproc,
                executionHandler,
                new DefaultConnectionReuseStrategy(),
                params);
        eventDispatch = new DefaultClientIOEventDispatch(clientHandler, params);
    }
    
    public void addSource(final SwarmSource source, SourceEventListener sourceEventListener) {
        if(!started.get())
            throw new IllegalStateException("Cannot add source before starting");
        
        if(LOG.isDebugEnabled())
            LOG.debug("Adding source: " + source);
        
        final SourceEventListener listener;
        if(sourceEventListener == null)
            listener = globalSourceEventListener;
        else
            listener = new DualSourceEventListener(sourceEventListener, globalSourceEventListener);
        
        ioReactor.connect(source.getAddress(), null, executionHandler.createAttachment(this, new SourceInfo(source, listener)), new SessionRequestCallback() {
            public void cancelled(SessionRequest request) {
                listener.connectFailed(SwarmerImpl.this, source);
            };
            public void completed(SessionRequest request) {
                listener.connected(SwarmerImpl.this, source);
            };
            public void failed(SessionRequest request) {
                listener.connectFailed(SwarmerImpl.this, source);
            };
            public void timeout(SessionRequest request) {
                listener.connectFailed(SwarmerImpl.this, source);
            };
        });        
    }
    
    public void start() {
        try {
            started.set(true);
            ioReactor.execute(eventDispatch);
        } catch(IOException iox) {
            LOG.warn("Unable to execute event dispatch");
        }
    }
    
    public HttpRequest getHttpRequest(SourceInfo sourceInfo) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Getting HTTP Request for: " + sourceInfo);
        
        if(!isActive()) {
            throw new IOException("swarm not active");
        }
        
        // TODO: Do nothing if queued?
        
        // TODO: Try THEX.
        
        HttpRequest request = createFileRequest(sourceInfo);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Sending request: " + request.getRequestLine() + ", headers: " + Arrays.asList(request.getAllHeaders()));
        
        return request;
    }
        
    private HttpRequest createFileRequest(SourceInfo sourceInfo) throws IOException {
        Range range;
        
        if(sourceInfo.getSource().isRangeRequestSupported()) {
            range = fileCoordinator.leasePortion(sourceInfo.getAvailableRanges());
        } else {
            range = fileCoordinator.lease();
        }
        
        if(range == null) {
            throw new IOException("No range available to lease");
        }
        
        HttpRequest request = new BasicHttpRequest("GET", sourceInfo.getSource().getUri());
        request.addHeader(new BasicHeader("Range", "bytes=" + range.getLow() + "-" + (range.getHigh())));
        sourceInfo.setContentListener(new FileContentListener(fileCoordinator, range));
        return request;
    }

    private boolean isActive() {
        return true;
    }

    public ContentListener getContentListener(HttpResponse response, SourceInfo sourceInfo) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Retrieving new content listener for: " + sourceInfo);
        
        ResponseContentListener contentListener = sourceInfo.getContentListener();
        if(contentListener != null) {
            contentListener.initialize(response);
            sourceInfo.setContentListener(null);
            return contentListener;
        }
        
        throw new IllegalStateException("No listener created!");
    }
    
    public void cleanup(SourceInfo sourceInfo) {
        if(LOG.isDebugEnabled())
            LOG.debug("Cleaning up: " + sourceInfo);
        
        ContentListener contentListener = sourceInfo.getContentListener();
        if(contentListener != null)
            contentListener.finished();
        sourceInfo.setContentListener(null);
        
        sourceInfo.getEventListener().connectionClosed(this, sourceInfo.getSource());
    }
    
    public void connectionEstablished(SourceInfo sourceInfo) {
        // Do nothing -- use the SessionRequestCallback's notification to notify the 
        // source we're connected.
    }
    
    public void responseProcessed(HttpResponse response, SourceInfo sourceInfo) {
        sourceInfo.getEventListener().responseProcessed(this, sourceInfo.getSource(), response.getStatusLine().getStatusCode());
    }
    
    private static class SourceInfo {
        private final SwarmSource source;
        private final SourceEventListener sourceEventListener;
        private ResponseContentListener contentListener;
        
        SourceInfo(SwarmSource source, SourceEventListener sourceEventListener) {
            this.source = source;
            this.sourceEventListener = sourceEventListener;
        }
        
        public ResponseContentListener getContentListener() {
            return contentListener;
        }

        public void setContentListener(ResponseContentListener responseContentListener) {
            this.contentListener = responseContentListener;
        }

        public IntervalSet getAvailableRanges() {
            return null;
        }
        
        public SourceEventListener getEventListener() {
            return sourceEventListener;
        }
        
        public SwarmSource getSource() {
            return source;
        }

        @Override
        public String toString() {
            return "Attachment for: " + source;
        }
    }
    
    private static final SourceEventListener NULL_LISTENER = new SourceEventListener() {
        public void connected(Swarmer swarmer, SwarmSource source) {}
        public void connectFailed(Swarmer swarmer, SwarmSource source) {}
        public void connectionClosed(Swarmer swarmer, SwarmSource source) {}
        public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {}
    };

}
