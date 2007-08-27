package org.limewire.http;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsLinker;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.NIODispatcher;
import org.limewire.service.ErrorService;

import com.google.inject.Provider;

/**
 * Processes HTTP requests which are forwarded to {@link HttpRequestHandler}
 * objects that can be registered for a URL pattern.
 * <p>
 * The acceptor uses HttpCore and LimeWire's HTTP component for connection
 * handling. <code>BasicHttpAcceptor</code> needs to be started by invoking
 * {@link #start(ConnectionDispatcher)} in order to accept connection.
 */
public class BasicHttpAcceptor {

    private static final Log LOG = LogFactory.getLog(BasicHttpAcceptor.class);

    public static final String[] DEFAULT_METHODS = new String[] { "GET",
            "HEAD", "POST", };

    private final boolean localOnly; 
    
    private final String[] supportedMethods;

    private final HttpRequestHandlerRegistry registry;

    private final SynchronizedHttpProcessor processor;

    private final List<HttpAcceptorListener> acceptorListeners = new CopyOnWriteArrayList<HttpAcceptorListener>();

    private final HttpParams params; 
    
    private HttpIOReactor reactor;

    private ConnectionEventListener connectionListener;

    private DefaultHttpResponseFactory responseFactory;

    private AtomicBoolean started = new AtomicBoolean();

    private Provider<ConnectionDispatcher> dispatcher;

    public BasicHttpAcceptor(Provider<ConnectionDispatcher> dispatcher, boolean localOnly, HttpParams params, String... supportedMethods) {
        this.dispatcher = dispatcher;
        this.localOnly = localOnly;
        this.params = params;
        this.supportedMethods = supportedMethods;
        
        this.registry = new SynchronizedHttpRequestHandlerRegistry();
        this.processor = new SynchronizedHttpProcessor();
        
        initializeDefaultInterceptor();
    }
    
    private void initializeDefaultInterceptor() {
        // intercepts HTTP requests and responses
        addResponseInterceptor(new ResponseDate());
        addResponseInterceptor(new ResponseServer());
        addResponseInterceptor(new ResponseContent());
        addResponseInterceptor(new LimeResponseConnControl());
    }

    public static HttpParams createDefaultParams(String userAgent, int timeout) {
        BasicHttpParams params = new BasicHttpParams();
        params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, timeout);
        params.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, timeout);
        // size of the per connection buffers used for headers and by the
        // decoder/encoder
        params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                8 * 1024);
        params.setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true);
        params.setParameter(HttpProtocolParams.ORIGIN_SERVER, userAgent);
        params.setIntParameter(HttpConnectionParams.MAX_LINE_LENGTH, 4096);
        params.setIntParameter(HttpConnectionParams.MAX_HEADER_COUNT, 50);
        params.setParameter(HttpProtocolParams.HTTP_ELEMENT_CHARSET,
                HTTP.ISO_8859_1);

        return params;
    }
    
    /**
     * Note: Needs to be called from the NIODispatcher thread.
     */
    private void initializeReactor() {
        assert NIODispatcher.instance().isDispatchThread();
        
        this.connectionListener = new ConnectionEventListener();

        responseFactory = new DefaultHttpResponseFactory();

        HttpServiceHandler serviceHandler = new HttpServiceHandler(processor,
                responseFactory, new DefaultConnectionReuseStrategy(), params);
        serviceHandler.setEventListener(connectionListener);
        serviceHandler.setHandlerResolver(this.registry);

        this.reactor = new HttpIOReactor(params);
        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, reactor.getHttpParams());
        try {
            this.reactor.execute(ioEventDispatch);
        } catch (IOException e) {
            // can not happen
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Adds a listener for acceptor events.
     */
    public void addAcceptorListener(HttpAcceptorListener listener) {
        acceptorListeners.add(listener);
    }
    
    /**
     * Adds an interceptor for incoming requests. 
     * 
     * @see HttpProcessor
     */
    public void addRequestInterceptor(HttpRequestInterceptor interceptor) {
        processor.addInterceptor(interceptor);
    }
    
    /**
     * Adds an interceptor for outgoing responses. 
     * 
     * @see HttpProcessor
     */
    public void addResponseInterceptor(HttpResponseInterceptor interceptor) {
        processor.addInterceptor(interceptor);
    }    

    /**
     * Returns the reactor.
     * 
     * <p>Note: Needs to be called from the NIODispatcher thread.
     * 
     * @return null, if the acceptor has not been started, yet.
     */
    protected HttpIOReactor getReactor() {
        assert NIODispatcher.instance().isDispatchThread();
        
        return reactor;
    }
    
    /* Simulates the processing of request for testing. */
    public HttpResponse process(HttpRequest request) throws IOException,
            HttpException {
        HttpExecutionContext context = new HttpExecutionContext(null);
        HttpResponse response = responseFactory.newHttpResponse(request
                .getRequestLine().getHttpVersion(), HttpStatus.SC_OK, context);
        HttpParamsLinker.link(response, this.params);

        // HttpContextParams.setLocal(context, true);
        context.setAttribute(HttpExecutionContext.HTTP_REQUEST, request);
        context.setAttribute(HttpExecutionContext.HTTP_RESPONSE, response);

        processor.process(request, context);

        HttpRequestHandler handler = null;
        if (this.registry != null) {
            String requestURI = request.getRequestLine().getUri();
            handler = this.registry.lookup(requestURI);
        }
        if (handler != null) {
            handler.handle(request, response, context);
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }

        return response;
    }

    /**
     * Removes <code>listener</code> from the list of acceptor listeners.
     * 
     * @see #addAcceptorListener(HttpAcceptorListener)
     */
    public void removeAcceptorListener(HttpAcceptorListener listener) {
        acceptorListeners.remove(listener);
    }

    /**
     * Removes an interceptor for incoming requests. 
     * 
     * @see #addRequestInterceptor(HttpRequestInterceptor)
     */
    public void removeRequestInterceptor(HttpRequestInterceptor interceptor) {
        processor.removeInterceptor(interceptor);
    }
    
    /**
     * Adds an interceptor for outgoing responses. 
     * 
     * @see #addResponseInterceptor(HttpResponseInterceptor)
     */
    public void removeResponseInterceptor(HttpResponseInterceptor interceptor) {
        processor.removeInterceptor(interceptor);
    }

    /**
     * Registers a request handler for a request pattern. See
     * {@link HttpRequestHandlerRegistry} for a description of valid patterns.
     * <p>
     * If a request matches multiple handlers, the handler with the longer
     * pattern is preferred.
     * <p>
     * Only a single handler may be registered per pattern.
     * 
     * @param pattern the URI pattern to handle requests for
     * @param handler the handler that processes the request
     */
    public void registerHandler(final String pattern,
            final HttpRequestHandler handler) {
        registry.register(pattern, handler);
    }

    /**
     * Unregisters the handlers for <code>pattern</code>.
     * 
     * @see #registerHandler(String, HttpRequestHandler)
     */
    public void unregisterHandler(final String pattern) {
        registry.unregister(pattern);
    }

    /**
     * Registers the acceptor at <code>dispatcher</code> for incoming
     * connections.
     */
    public void start() {
        if (started.getAndSet(true)) {
            throw new IllegalStateException();
        }
        
        final AtomicBoolean inited = new AtomicBoolean(false);
        try {
            Future<?> result = NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    initializeReactor();
                    inited.set(true);
                }
            });
            
            // wait for reactor to finish initialization
            result.get();
        } catch (InterruptedException e) {
            if (inited.get())
                LOG.warn("Interrupted while waiting for reactor initialization", e);
            else
                ErrorService.error(e); // this is a problem.
        } catch (ExecutionException e) {
            ErrorService.error(e);
        }

        dispatcher.get().addConnectionAcceptor(new ConnectionAcceptor() {
            public void acceptConnection(String word, Socket socket) {
                reactor.acceptConnection(word + " ", socket);
            }
        }, localOnly, false, supportedMethods);
    }

    /**
     * Unregisters the acceptor at <code>dispatcher</code>.
     * 
     * @see #start(ConnectionDispatcher)
     */
    public void stop() {
        if (!started.getAndSet(false)) {
            throw new IllegalStateException();
        }
        
        dispatcher.get().removeConnectionAcceptor(supportedMethods);
        dispatcher = null;
    }

    /**
     * Forwards events from the underlying protocol layer to acceptor event
     * listeners.
     */
    private class ConnectionEventListener implements HttpServiceEventListener {

        public void connectionOpen(NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionOpen(conn);
            }
        }

        public void connectionClosed(NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionClosed(conn);
            }
        }

        public void connectionTimeout(NHttpConnection conn) {
            // should never happen since LimeWire will close the socket on
            // timeouts which will trigger a connectionClosed() event
            throw new RuntimeException();
        }

        public void fatalIOException(IOException e, NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            LOG.debug("HTTP connection error", e);
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionClosed(conn);
            }
        }

        public void fatalProtocolException(HttpException e, NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            LOG.debug("HTTP protocol error", e);
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionClosed(conn);
            }
        }

        public void requestReceived(NHttpConnection conn, HttpRequest request) {
            assert NIODispatcher.instance().isDispatchThread();
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing request: " + request.getRequestLine());
            }
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.requestReceived(conn, request);
            }
        }

        public void responseSent(NHttpConnection conn, HttpResponse response) {
            assert NIODispatcher.instance().isDispatchThread();
            
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.responseSent(conn, response);
            }
        }

    }

}
