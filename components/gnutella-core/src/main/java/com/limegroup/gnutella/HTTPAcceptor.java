package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.HttpIOSession;
import org.limewire.http.HttpServiceEventListener;
import org.limewire.http.HttpServiceHandler;
import org.limewire.http.LimeResponseConnControl;

import com.limegroup.gnutella.http.HTTPConnectionData;
import com.limegroup.gnutella.http.HttpContextParams;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Processes HTTP requests which are forwarded to {@link HttpRequestHandler}
 * objects that can be registered for a URL pattern.
 * <p>
 * The acceptor uses HttpCore and LimeWire's HTTP component for connection
 * handling. It needs to be start by invoking
 * {@link #start(ConnectionDispatcher)} in order to accept connection.
 */
public class HTTPAcceptor {

    private static final Log LOG = LogFactory.getLog(HTTPAcceptor.class);
    
    private static final String[] SUPPORTED_METHODS = new String[] { "GET", "HEAD", };

    private HttpIOReactor reactor;

    private HttpParams params;

    private HttpRequestHandlerRegistry registry;

    private ConnectionEventListener connectionListener;

    private List<HTTPAcceptorListener> acceptorListeners = Collections
            .synchronizedList(new ArrayList<HTTPAcceptorListener>());

    private BasicHttpProcessor processor;

    private DefaultHttpResponseFactory responseFactory;

    private HttpRequestHandler notFoundHandler;

    public HTTPAcceptor() {
        initializeReactor();
        inititalizeDefaultHandlers();
    }

    private void initializeReactor() {
        this.params = new BasicHttpParams();
        this.params.setIntParameter(HttpConnectionParams.SO_TIMEOUT,
                Constants.TIMEOUT);
        this.params.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
                Constants.TIMEOUT);
        // size of the per connection buffers used for headers and by the
        // decoder/encoder
        this.params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                8 * 1024);
        this.params.setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true);
        this.params.setParameter(HttpProtocolParams.ORIGIN_SERVER,
                LimeWireUtils.getHttpServer());

        this.registry = new HttpRequestHandlerRegistry();
        this.connectionListener = new ConnectionEventListener();

        // intercepts HTTP requests and responses
        processor = new BasicHttpProcessor();
        
        processor.addInterceptor(new RequestStatisticTracker());

        processor.addInterceptor(new ResponseDate());
        processor.addInterceptor(new ResponseServer());
        processor.addInterceptor(new ResponseContent());
        processor.addInterceptor(new LimeResponseConnControl());
        processor.addInterceptor(new HeaderStatisticTracker());

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

    private void inititalizeDefaultHandlers() {
        // unsupported requests
        notFoundHandler = new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                UploadStat.FILE_NOT_FOUND.incrementStat();

                response.setReasonPhrase("Feature Not Active");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        };
        registerHandler("/browser-control", notFoundHandler);
        registerHandler("/gnutella/file-view*", notFoundHandler);
        registerHandler("/gnutella/res/*", notFoundHandler);

        // return 400 for unmatched requests
        registerHandler("*", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                UploadStat.MALFORMED_REQUEST.incrementStat();

                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            }
        });
    }

    /**
     * Handles an incoming HTTP push request.
     */
    public void acceptConnection(Socket socket, HTTPConnectionData data) {
        DefaultNHttpServerConnection conn = reactor.acceptConnection(null, socket);
        HttpContextParams.setConnectionData(conn.getContext(), data);
    }

    /**
     * Adds a listener for acceptor events.
     */
    public void addAcceptorListener(HTTPAcceptorListener listener) {
        acceptorListeners.add(listener);
    }

    /**
     * Returns a handler that responds with a HTTP 404 error.
     */
    public HttpRequestHandler getNotFoundHandler() {
        return notFoundHandler;
    }
    
    /* Simulates the processing of request for testing. */
    protected HttpResponse process(HttpRequest request) throws IOException,
            HttpException {
        HttpExecutionContext context = new HttpExecutionContext(null);
        HttpResponse response = responseFactory.newHttpResponse(request
                .getRequestLine().getHttpVersion(), HttpStatus.SC_OK, context);
        response.getParams().setDefaults(this.params);

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
     * @see #addAcceptorListener(HTTPAcceptorListener)
     */
    public void removeAcceptorListener(HTTPAcceptorListener listener) {
        acceptorListeners.remove(listener);
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
    public synchronized void registerHandler(final String pattern,
            final HttpRequestHandler handler) {
        this.registry.register(pattern, handler);
    }

    /**
     * Unregisters the handlers for <code>pattern</code>.
     *  
     * @see #registerHandler(String, HttpRequestHandler)
     */
    public synchronized void unregisterHandler(final String pattern) {
        this.registry.unregister(pattern);
    }
    
    /**
     * Registers the acceptor at <code>dispatcher</code> for incoming
     * connections.
     */
    public void start(ConnectionDispatcher dispatcher) {
        dispatcher.addConnectionAcceptor(
            new ConnectionAcceptor() {
                public void acceptConnection(String word, Socket socket) {
                    reactor.acceptConnection(word + " ", socket);
                }
            }, SUPPORTED_METHODS, false, false);
    }

    /**
     * Unregisters the acceptor at <code>dispatcher</code>.
     * 
     * @see #start(ConnectionDispatcher)
     */
    public void stop(ConnectionDispatcher dispatcher) {
        dispatcher.removeConnectionAcceptor(SUPPORTED_METHODS);
    }

    /**
     * Forwards events from the underlying protocol layer to acceptor event listeners.
     */
    private class ConnectionEventListener implements HttpServiceEventListener {

        public void connectionOpen(NHttpConnection conn) {
            HTTPAcceptorListener[] listeners = HTTPAcceptor.this.acceptorListeners
                    .toArray(new HTTPAcceptorListener[0]);
            for (HTTPAcceptorListener listener : listeners) {
                listener.connectionOpen(conn);
            }
        }

        public void connectionClosed(NHttpConnection conn) {
            HTTPAcceptorListener[] listeners = HTTPAcceptor.this.acceptorListeners
                    .toArray(new HTTPAcceptorListener[0]);
            for (HTTPAcceptorListener listener : listeners) {
                listener.connectionClosed(conn);
            }
        }

        public void connectionTimeout(NHttpConnection conn) {
            // should never happen since LimeWire will close the socket on
            // timeouts which will trigger a connectionClosed() event
            throw new RuntimeException();
        }

        public void fatalIOException(IOException e, NHttpConnection conn) {
            LOG.debug("HTTP connection error", e);
            
            if (HttpContextParams.isPush(conn.getContext())) {
                if (HttpContextParams.isFirewalled(conn.getContext())) {
                    UploadStat.FW_FW_FAILURE.incrementStat();
                }
                UploadStat.PUSH_FAILED.incrementStat();
            }
            
            HTTPAcceptorListener[] listeners = HTTPAcceptor.this.acceptorListeners
                    .toArray(new HTTPAcceptorListener[0]);
            for (HTTPAcceptorListener listener : listeners) {
                listener.connectionClosed(conn);
            }
        }

        public void fatalProtocolException(HttpException e, NHttpConnection conn) {
            LOG.debug("HTTP protocol error", e);
            HTTPAcceptorListener[] listeners = HTTPAcceptor.this.acceptorListeners
                    .toArray(new HTTPAcceptorListener[0]);
            for (HTTPAcceptorListener listener : listeners) {
                listener.connectionClosed(conn);
            }
        }

        public void requestReceived(NHttpConnection conn) {
            if (LOG.isDebugEnabled())
                LOG.debug("Processing request: " + conn.getHttpRequest().getRequestLine());

            HTTPAcceptorListener[] listeners = HTTPAcceptor.this.acceptorListeners
            .toArray(new HTTPAcceptorListener[0]);
            for (HTTPAcceptorListener listener : listeners) {
                listener.requestReceived(conn, conn.getHttpRequest());
            }
        }

        public void responseSent(NHttpConnection conn) {
            HttpIOSession session = HttpContextParams.getIOSession(conn
                    .getContext());
            session
                    .setSocketTimeout(SharingSettings.PERSISTENT_HTTP_CONNECTION_TIMEOUT
                            .getValue());
            session.setThrottle(null);

            HTTPAcceptorListener[] listeners = HTTPAcceptor.this.acceptorListeners
                    .toArray(new HTTPAcceptorListener[0]);
            for (HTTPAcceptorListener listener : listeners) {
                listener.responseSent(conn, conn.getHttpResponse());
            }
        }

    }

    /**
     * Updates statistics when a request is received.
     */
    private class RequestStatisticTracker implements HttpRequestInterceptor {

        public void process(HttpRequest request, HttpContext context)
                throws HttpException, IOException {
            String method = request.getRequestLine().getMethod();
            if (HttpContextParams.isSubsequentRequest(context)) {
                if ("GET".equals(method))
                    UploadStat.SUBSEQUENT_GET.incrementStat();
                else if ("HEAD".equals(method))
                    UploadStat.SUBSEQUENT_HEAD.incrementStat();
                else
                    UploadStat.SUBSEQUENT_UNKNOWN.incrementStat();
                HttpContextParams.setSubsequentRequest(context, true);
            } else {
                if (HttpContextParams.isPush(context)) {
                    if ("GET".equals(method))
                        UploadStat.PUSHED_GET.incrementStat();
                    else if ("HEAD".equals(method))
                        UploadStat.PUSHED_HEAD.incrementStat();
                    else
                        UploadStat.PUSHED_UNKNOWN.incrementStat();
                } else {
                    if ("GET".equals(method))
                        HTTPStat.GET_REQUESTS.incrementStat();
                    else if ("HEAD".equals(method))
                        HTTPStat.HEAD_REQUESTS.incrementStat();
                    else 
                        HTTPStat.UNKNOWN_REQUESTS.incrementStat();
                }
            }
        }
        
    }

    /**
     * Tracks the bandwidth used when sending a response.
     */
    private class HeaderStatisticTracker implements HttpResponseInterceptor {

        /*
         * XXX iterating over all headers is rather inefficient since the size of
         * the headers is known in DefaultNHttpServerConnection.submitResponse() but
         * can't be easily made accessible
         */
        public void process(HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            for (Iterator it = response.headerIterator(); it.hasNext();) {
                Header header = (Header) it.next();
                BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header
                        .getName().length()
                        + 2 + header.getValue().length());
            }
        }

    }

}
