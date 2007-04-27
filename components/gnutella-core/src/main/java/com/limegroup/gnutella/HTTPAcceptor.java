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
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.HttpIOSession;
import org.limewire.http.HttpResponseListener;
import org.limewire.http.HttpServiceEventListener;
import org.limewire.http.HttpServiceHandler;
import org.limewire.http.LimeResponseConnControl;

import com.limegroup.gnutella.http.HttpContextParams;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Redirects HTTP requests to handlers.
 */
public class HTTPAcceptor implements ConnectionAcceptor {

    private static final Log LOG = LogFactory.getLog(HTTPAcceptor.class);

    private HttpIOReactor reactor;

    private HttpParams params;

    private HttpRequestHandlerRegistry registry;

    private ConnectionEventListener connectionListener;

    private List<HttpResponseListener> responseListeners = Collections
            .synchronizedList(new ArrayList<HttpResponseListener>());

    private BasicHttpProcessor processor;

    private DefaultHttpResponseFactory responseFactory;

    public HTTPAcceptor() {
        initializeReactor();

        RouterService.getConnectionDispatcher().addConnectionAcceptor(
                new ConnectionAcceptor() {
                    public void acceptConnection(String word, Socket socket) {
                        reactor.acceptConnection(word, socket);
                    }
                }, new String[] { "GET", "HEAD", }, false, false);
    }

    private void initializeReactor() {
        this.params = new BasicHttpParams();
        this.params.setIntParameter(HttpConnectionParams.SO_TIMEOUT,
                Constants.TIMEOUT);
        this.params.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
                Constants.TIMEOUT);
        // size of the buffers used for headers and by the
        // decoder/encoder
        this.params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                8 * 1024);
        this.params.setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true);
        this.params.setParameter(HttpProtocolParams.ORIGIN_SERVER,
                LimeWireUtils.getHttpServer());

        this.registry = new HttpRequestHandlerRegistry();
        this.connectionListener = new ConnectionEventListener();

        // intercepts http requests and responses
        processor = new BasicHttpProcessor();
        // processor.addInterceptor(new ResponseDate());
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

    /**
     * Incoming HTTP requests.
     */
    public void acceptConnection(String word, Socket socket) {
        if ("GET".equals(word))
            HTTPStat.GET_REQUESTS.incrementStat();
        else if ("HEAD".equals(word))
            HTTPStat.HEAD_REQUESTS.incrementStat();

        reactor.acceptConnection(word, socket);
    }

    /**
     * Adds a listener for acceptor events.
     */
    public void addResponseListener(HttpResponseListener listener) {
        responseListeners.add(listener);
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
     * @see #addResponseListener(HttpResponseListener)
     */
    public void removeResponseListener(HttpResponseListener listener) {
        responseListeners.remove(listener);
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
     * @see 
     */
    public synchronized void unregisterHandler(final String pattern) {
        this.registry.unregister(pattern);
    }
    
    /**
     * Forwards events from the underlying protocol layer to acceptor event listeners.
     */
    private class ConnectionEventListener implements HttpServiceEventListener {

        public void connectionOpen(NHttpConnection conn) {
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.connectionOpen(conn);
            }
        }

        public void connectionClosed(NHttpConnection conn) {
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
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
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.connectionClosed(conn);
            }
        }

        public void fatalProtocolException(HttpException e, NHttpConnection conn) {
            LOG.debug("HTTP protocol error", e);
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.connectionClosed(conn);
            }
        }

        public void responseSent(NHttpConnection conn) {
            HttpIOSession session = HttpContextParams.getIOSession(conn
                    .getContext());
            session
                    .setSocketTimeout(SharingSettings.PERSISTENT_HTTP_CONNECTION_TIMEOUT
                            .getValue());
            session.setThrottle(null);

            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.responseSent(conn, conn.getHttpResponse());
            }
        }

    }

    /*
     * XXX iterating over all headers is rather inefficient since the size of
     * the headers is known in DefaultNHttpServerConnection.submitResponse() but
     * can't be easily made accessible
     */
    private class HeaderStatisticTracker implements HttpResponseInterceptor {

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
