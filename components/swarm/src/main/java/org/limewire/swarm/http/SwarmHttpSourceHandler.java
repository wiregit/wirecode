package org.limewire.swarm.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.limewire.http.protocol.SynchronizedHttpProcessor;
import org.limewire.swarm.DualSourceEventListener;
import org.limewire.swarm.ReconnectingSourceEventListener;
import org.limewire.swarm.SourceEventListener;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.http.handler.SwarmHttpExecutionHandler;
import org.limewire.swarm.http.handler.SwarmCoordinatorHttpExecutionHandler;

public class SwarmHttpSourceHandler implements SwarmSourceHandler {

    private static final Log LOG = LogFactory.getLog(SwarmHttpSourceHandler.class);

    private static final SourceEventListener DEFAULT_SOURCE_EVENT_LISTENER = new ReconnectingSourceEventListener();

    private final SourceEventListener globalSourceEventListener;

    private final SynchronizedHttpProcessor httpProcessor;

    private final ConnectingIOReactor ioReactor;

    private final IOEventDispatch eventDispatch;

    private final AsyncNHttpClientHandler clientHandler;

    private final SwarmHttpExecutionHandler executionHandler;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final SwarmCoordinator swarmCoordinator;

    public SwarmHttpSourceHandler(SwarmCoordinator swarmCoordinator, HttpParams params,
            ConnectingIOReactor ioReactor, ConnectionReuseStrategy connectionReuseStrategy,
            SourceEventListener globalSourceEventListener) {
        this.swarmCoordinator = swarmCoordinator;
        this.executionHandler = new SwarmCoordinatorHttpExecutionHandler(swarmCoordinator);
        this.ioReactor = ioReactor;

        if (globalSourceEventListener == null) {
            this.globalSourceEventListener = DEFAULT_SOURCE_EVENT_LISTENER;
        } else {
            this.globalSourceEventListener = globalSourceEventListener;
        }

        httpProcessor = new SynchronizedHttpProcessor();
        httpProcessor.addInterceptor(new RequestContent());
        httpProcessor.addInterceptor(new RequestTargetHost());
        httpProcessor.addInterceptor(new RequestConnControl());
        httpProcessor.addInterceptor(new RequestUserAgent());
        httpProcessor.addInterceptor(new RequestExpectContinue());

        clientHandler = new AsyncNHttpClientHandler(httpProcessor, new SwarmExecutionHandler(),
                connectionReuseStrategy, params);

        eventDispatch = new DefaultClientIOEventDispatch(clientHandler, params);
    }

    public void addSource(SwarmSource source, SourceEventListener sourceEventListener) {

        SourceEventListener listener = buildListener(sourceEventListener);
//        SessionRequestCallback sessionRequestCallback = new SwarmHttpSessionRequestCallback(this,
//                source, listener);
//
//        ioReactor.connect(source.getAddress(), null, new HttpSourceInfo(source, listener),
//                sessionRequestCallback);
    }

    private SourceEventListener buildListener(SourceEventListener sourceEventListener) {
        final SourceEventListener listener;
        if (sourceEventListener == null) {
            listener = globalSourceEventListener;
        } else {
            listener = new DualSourceEventListener(sourceEventListener, globalSourceEventListener);
        }
        return listener;
    }

    public void shutdown() throws IOException {
        started.set(false);
        ioReactor.shutdown();
    }

    public void start() throws IOException {
        started.set(true);
        ioReactor.execute(eventDispatch);
    }

    public boolean isActive() {
        return started.get();
    }

    private class SwarmExecutionHandler implements NHttpRequestExecutionHandler {
        private static final String LISTENER = "swarm.http.internal.eventlistener";

        public void initalizeContext(HttpContext context, Object attachment) {
            HttpSourceInfo info = (HttpSourceInfo) attachment;
            context.setAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE, info.getSource());
            context.setAttribute(LISTENER, info.getSourceEventListener());
        }

        public void finalizeContext(HttpContext context) {
            SourceEventListener listener = (SourceEventListener) context.getAttribute(LISTENER);
            SwarmSource source = (SwarmSource) context
                    .getAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE);
            listener.connectionClosed(SwarmHttpSourceHandler.this, source);

            executionHandler.finalizeContext(context);
        }

        public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
            if (isActive()) {
                SourceEventListener listener = (SourceEventListener) context.getAttribute(LISTENER);
                SwarmSource source = (SwarmSource) context
                        .getAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE);
                listener.responseProcessed(SwarmHttpSourceHandler.this, source,
                        new SwarmHttpStatus(response.getStatusLine()));

                executionHandler.handleResponse(response, context);
            } else {
                throw new IOException("Not active!");
            }
        }

        public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
                throws IOException {
            if (isActive()) {
                if (LOG.isTraceEnabled())
                    LOG.trace("Handling response: " + response.getStatusLine() + ", headers: "
                            + Arrays.asList(response.getAllHeaders()));
                return executionHandler.responseEntity(response, context);
            } else {
                throw new IOException("Not active!");
            }
        }

        public HttpRequest submitRequest(HttpContext context) {
            if (isActive()) {
                HttpRequest request = executionHandler.submitRequest(context);

                if (LOG.isTraceEnabled() && request != null)
                    LOG.trace("Submitting request: " + request.getRequestLine());
                return request;
            } else {
                SwarmHttpUtils.closeConnectionFromContext(context);
                return null;
            }
        }
    }

    public void addSource(SwarmSource source) {
        addSource(source, null);
    }

    public boolean isComplete() {
        return swarmCoordinator.isComplete();
    }

}
