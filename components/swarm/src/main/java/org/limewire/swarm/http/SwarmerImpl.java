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
import org.limewire.http.reactor.HttpIOSession;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.http.handler.SwarmHttpExecutionHandler;

public class SwarmerImpl implements Swarmer {

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final ConnectingIOReactor ioReactor;

    private final IOEventDispatch eventDispatch;

    private final AsyncNHttpClientHandler clientHandler;

    private final SwarmHttpExecutionHandler executionHandler;

    private final SwarmSourceEventListener globalSourceEventListener;

    private final SynchronizedHttpProcessor httpProcessor;

    public SwarmerImpl(SwarmHttpExecutionHandler executionHandler,
            ConnectionReuseStrategy connectionReuseStrategy, ConnectingIOReactor ioReactor,
            HttpParams params, SwarmSourceEventListener sourceEventListener) {

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
//        httpProcessor.addInterceptor(new HttpRequestInterceptor(){
//
//            public void process(HttpRequest arg0, HttpContext arg1) throws HttpException,
//                    IOException {
//               arg0.
//                
//            }
//            
//        });
 
        final ConnectionReuseStrategy finalConnectionReuseStrategy = connectionReuseStrategy;
        clientHandler = new AsyncNHttpClientHandler(httpProcessor, new SwarmExecutionHandler(),
                new ConnectionReuseStrategy() {

                    public boolean keepAlive(HttpResponse arg0, HttpContext arg1) {
                        
                        return finalConnectionReuseStrategy.keepAlive(arg0, arg1);
                    }
            
        }, params);

        eventDispatch = new DefaultClientIOEventDispatch(clientHandler, params);
    }

    public void addSource(final SwarmSource source) {
        addSource(source, null);
    }

    public void addSource(final SwarmSource source, SwarmSourceEventListener sourceEventListener) {
//        if (!started.get())
//            throw new IllegalStateException("Cannot add source before starting");

        if (LOG.isDebugEnabled())
            LOG.debug("Adding source: " + source);

        final SwarmSourceEventListener listener = getListener(sourceEventListener);
        SessionRequestCallback sessionRequestCallback = createSessionRequestCallback(source,
                listener);

        ioReactor.connect(source.getAddress(), null, new SourceInfo(source, listener),
                sessionRequestCallback);
    }

    private SessionRequestCallback createSessionRequestCallback(final SwarmSource source,
            final SwarmSourceEventListener listener) {
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

    private SwarmSourceEventListener getListener(SwarmSourceEventListener sourceEventListener) {
        final SwarmSourceEventListener listener;
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

    public boolean isActive() {
        return started.get();
    }

    private static class SourceInfo {
        private final SwarmSource source;

        private final SwarmSourceEventListener sourceEventListener;

        SourceInfo(SwarmSource source, SwarmSourceEventListener sourceEventListener) {
            this.source = source;
            this.sourceEventListener = sourceEventListener;
        }

        public SwarmSourceEventListener getEventListener() {
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
            SwarmSourceEventListener listener = (SwarmSourceEventListener) context.getAttribute(LISTENER);
            SwarmSource source = (SwarmSource) context
                    .getAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE);
            listener.connectionClosed(SwarmerImpl.this, source);

            executionHandler.finalizeContext(context);
        }

        public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
            if (isActive()) {
                SwarmSourceEventListener listener = (SwarmSourceEventListener) context.getAttribute(LISTENER);
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
                HttpIOSession ioSession = (HttpIOSession) context.getAttribute(LimeConnectingIOReactor.IO_SESSION_KEY);
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

    private static final SwarmSourceEventListener NULL_LISTENER = new SwarmSourceEventListener() {
        public void connected(Swarmer swarmer, SwarmSource source) {
        }

        public void connectFailed(Swarmer swarmer, SwarmSource source) {
        }

        public void connectionClosed(Swarmer swarmer, SwarmSource source) {
        }

        public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {
            //We can't try submitting a request again too soon after getting the response.
            //kinda hacky for now, will find a better place for this
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    public boolean finished() {
        // TODO Auto-generated method stub
        return false;
    }

    public void register(Class clazz, SwarmSourceHandler sourceHandler) {
        // TODO Auto-generated method stub
        
    }

}
