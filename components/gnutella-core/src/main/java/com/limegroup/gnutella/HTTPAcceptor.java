package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.HttpResponseListener;
import org.limewire.http.HttpServiceHandler;
import org.limewire.http.LimeResponseConnControl;
import org.limewire.http.ServerConnectionEventListener;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Redirects HTTP requests to handlers.
 */
public class HTTPAcceptor implements ConnectionAcceptor {

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

        responseFactory = new DefaultHttpResponseFactory();

        HttpServiceHandler serviceHandler = new HttpServiceHandler(processor,
                responseFactory, new DefaultConnectionReuseStrategy(), params);
        serviceHandler.setConnectionListener(connectionListener);
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
        reactor.acceptConnection(word, socket);
    }

    public void addResponseListener(HttpResponseListener listener) {
        responseListeners.add(listener);
    }

    /** For testing. */
    public HttpResponse process(HttpRequest request) throws IOException,
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

    public void removeResponseListener(HttpResponseListener listener) {
        responseListeners.remove(listener);
    }

    public void registerHandler(final String pattern,
            final HttpRequestHandler handler) {
        this.registry.register(pattern, handler);
    }

    private class ConnectionEventListener implements
            ServerConnectionEventListener {

        public void connectionClosed(NHttpServerConnection conn) {
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.connectionClosed(conn);
            }
        }

        public void connectionOpen(NHttpServerConnection conn) {
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.connectionOpened(conn);
            }
        }

        public void connectionTimeout(NHttpServerConnection conn) {
            // should never happen since LimeWire will close the socket on
            // timeouts
            // which will trigger a connectionClosed() event
            throw new RuntimeException();
        }

        public void fatalIOException(NHttpServerConnection conn, IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        public void fatalProtocolException(NHttpServerConnection conn,
                HttpException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        public void responseBodySent(NHttpServerConnection conn,
                HttpResponse response) {
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.responseSent(conn, response);
            }
        }

        public void responseHeadersSent(NHttpServerConnection conn,
                HttpResponse response) {
            HttpResponseListener[] listeners = HTTPAcceptor.this.responseListeners
                    .toArray(new HttpResponseListener[0]);
            for (HttpResponseListener listener : listeners) {
                listener.responseSent(conn, response);
            }
        }

    }

}
