package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.nio.http.HttpCoreUtils;
import org.limewire.nio.http.HttpIOReactor;

import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.uploader.HTTPSession;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.LimeWireUtils;

public class NIOUploadManager implements UploadManager, ConnectionAcceptor {

    private HttpIOReactor reactor;

    private HttpParams params;

    private HttpRequestHandlerRegistry registry;

    private ReactorEventListener listener;

    private UploadSlotManager uploadSlotManager;

    public NIOUploadManager(UploadSlotManager uploadSlotManager) {
        this.uploadSlotManager = uploadSlotManager;

        initializeReactor();
        inititalizeHandlers();

        RouterService.getConnectionDispatcher().addConnectionAcceptor(reactor,
                new String[] { "GET", "HEAD", }, false, false);
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
        this.params.setParameter(HttpProtocolParams.USER_AGENT, LimeWireUtils
                .getHttpServer());

        this.registry = new HttpRequestHandlerRegistry();
        this.listener = new ReactorEventListener();

        // intercepts http requests and responses
        BasicHttpProcessor processor = new BasicHttpProcessor();
        processor.addInterceptor(new ResponseDate());
        processor.addInterceptor(new ResponseServer());
        processor.addInterceptor(new ResponseContent());
        processor.addInterceptor(new ResponseConnControl());

        BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(
                processor, new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(), params);
        serviceHandler.setEventListener(listener);
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

    private void inititalizeHandlers() {
        // browse
        registerHandler("/", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                if (!HttpCoreUtils.hasHeader(request, "Accept",
                        Constants.QUERYREPLY_MIME_TYPE)) {
                    response.setStatusCode(HttpStatus.SC_NOT_ACCEPTABLE);
                } else {
                    response.setEntity(new BrowseRepsonseEntity());
                    response.setStatusCode(HttpStatus.SC_OK);
                }
            }
        });

        // what is this?
        registerHandler("/browser-control", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setReasonPhrase("Feature Not Active");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        });

        // push-proxy requests
        HttpRequestHandler pushProxyHandler = new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
            }
        };
        registerHandler("/gnutella/push-proxy", pushProxyHandler);
        registerHandler("/gnet/push-proxy", pushProxyHandler);

        // uploads
        registerHandler("/get*", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
            }
        });
    }

    /**
     * Push uploads from firewalled clients.
     */
    public void acceptUpload(HTTPRequestMethod get, Socket socket, boolean lan) {
        // TODO Auto-generated method stub

    }

    /**
     * Incoming HTTP requests.
     */
    public void acceptConnection(String word, Socket socket) {
        reactor.acceptConnection(word, socket);
    }

    public float getLastMeasuredBandwidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getNumQueuedUploads() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getPositionInQueue(HTTPSession session) {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getUploadSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean hadSuccesfulUpload() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasActiveInternetTransfers() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isConnectedTo(InetAddress addr) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isServiceable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean killUploadsForFileDesc(FileDesc fd) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean mayBeServiceable() {
        // TODO Auto-generated method stub
        return false;
    }

    public int measuredUploadSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int uploadsInProgress() {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getAverageBandwidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        // TODO Auto-generated method stub
        return 0;
    }

    public void measureBandwidth() {
        // TODO Auto-generated method stub

    }

    public void registerHandler(final String pattern,
            final HttpRequestHandler handler) {
        this.registry.register(pattern, handler);
    }

    private class ReactorEventListener implements EventListener {

        public void connectionClosed(InetAddress address) {
        }

        public void connectionOpen(InetAddress address) {
        }

        public void connectionTimeout(InetAddress address) {
        }

        public void fatalIOException(IOException e) {
            throw new RuntimeException(e);
        }

        public void fatalProtocolException(HttpException e) {
            throw new RuntimeException(e);
        }

    }
}
