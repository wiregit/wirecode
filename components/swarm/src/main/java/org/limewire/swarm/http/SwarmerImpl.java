package org.limewire.swarm.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.limewire.http.protocol.SynchronizedHttpProcessor;
import org.limewire.swarm.http.handler.ExecutionHandler;

public class SwarmerImpl implements Swarmer {

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final ConnectingIOReactor ioReactor;

    private final IOEventDispatch eventDispatch;

    private final AsyncNHttpClientHandler clientHandler;

    private final ExecutionHandler executionHandler;

    private final SourceEventListener globalSourceEventListener;

    private final SynchronizedHttpProcessor httpProcessor;

    public SwarmerImpl(ExecutionHandler executionHandler,
            ConnectionReuseStrategy connectionReuseStrategy, ConnectingIOReactor ioReactor,
            HttpParams params, SourceEventListener sourceEventListener) {

        this.executionHandler = executionHandler;
        this.ioReactor = ioReactor;
        if (sourceEventListener == null)
            this.globalSourceEventListener = NULL_LISTENER;
        else
            this.globalSourceEventListener = sourceEventListener;

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

    public void addSource(final SwarmSource source) {
        addSource(source, null);
    }

    public void addSource(final SwarmSource source, SourceEventListener sourceEventListener) {
//        if (!started.get())
//            throw new IllegalStateException("Cannot add source before starting");

        if (LOG.isDebugEnabled())
            LOG.debug("Adding source: " + source);

        final SourceEventListener listener = getListener(sourceEventListener);
        SessionRequestCallback sessionRequestCallback = createSessionRequestCallback(source,
                listener);

        ioReactor.connect(source.getAddress(), null, new SourceInfo(source, listener),
                sessionRequestCallback);
    }

    private SessionRequestCallback createSessionRequestCallback(final SwarmSource source,
            final SourceEventListener listener) {
        return new SessionRequestCallback() {
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
        };
    }

    private SourceEventListener getListener(SourceEventListener sourceEventListener) {
        final SourceEventListener listener;
        if (sourceEventListener == null) {
            listener = globalSourceEventListener;
        } else {
            listener = new DualSourceEventListener(sourceEventListener, globalSourceEventListener);
        }
        return listener;
    }

    public void addHeaderInterceptor(HttpRequestInterceptor requestInterceptor) {
        httpProcessor.addInterceptor(requestInterceptor);
    }

    public void addHeaderInterceptor(HttpResponseInterceptor responseInterceptor) {
        httpProcessor.addInterceptor(responseInterceptor);
    }

    public void start() {
        try {
            started.set(true);
            ioReactor.execute(eventDispatch);
        } catch (IOException iox) {
            LOG.warn("Unable to execute event dispatch");
        }
    }

    public void shutdown() {
        try {
            started.set(false);
            ioReactor.shutdown();
        } catch (IOException iox) {
            LOG.warn("Unable to shutdown event dispatch");
        }

    }

    private boolean isActive() {
        return started.get();
    }

    private static class SourceInfo {
        private final SwarmSource source;

        private final SourceEventListener sourceEventListener;

        SourceInfo(SwarmSource source, SourceEventListener sourceEventListener) {
            this.source = source;
            this.sourceEventListener = sourceEventListener;
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

    private class SwarmExecutionHandler implements NHttpRequestExecutionHandler {
        private static final String LISTENER = "swarm.http.internal.eventlistener";

        public void initalizeContext(HttpContext context, Object attachment) {
            if (isActive()) {
                SourceInfo info = (SourceInfo) attachment;
                context.setAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE, info.getSource());
                context.setAttribute(LISTENER, info.getEventListener());
            } else {
                SwarmHttpUtils.closeConnectionFromContext(context);
            }
        }

        public void finalizeContext(HttpContext context) {
            SourceEventListener listener = (SourceEventListener) context.getAttribute(LISTENER);
            SwarmSource source = (SwarmSource) context
                    .getAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE);
            listener.connectionClosed(SwarmerImpl.this, source);

            executionHandler.finalizeContext(context);
        }

        public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
            if (isActive()) {
                SourceEventListener listener = (SourceEventListener) context.getAttribute(LISTENER);
                SwarmSource source = (SwarmSource) context
                        .getAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE);
                listener.responseProcessed(SwarmerImpl.this, source, response.getStatusLine()
                        .getStatusCode());

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

    private static final SourceEventListener NULL_LISTENER = new SourceEventListener() {
        public void connected(Swarmer swarmer, SwarmSource source) {
        }

        public void connectFailed(Swarmer swarmer, SwarmSource source) {
        }

        public void connectionClosed(Swarmer swarmer, SwarmSource source) {
        }

        public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {
        }
    };

    public boolean finished() {
        // TODO Auto-generated method stub
        return false;
    }

}
