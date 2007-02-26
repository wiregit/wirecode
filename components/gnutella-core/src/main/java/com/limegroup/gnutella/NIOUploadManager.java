package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.FileEntity;
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
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.HttpCoreUtils;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.HttpServiceHandler;
import org.limewire.http.ServerConnectionEventListener;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.uploader.BrowseRepsonseEntity;
import com.limegroup.gnutella.uploader.HTTPSession;
import com.limegroup.gnutella.uploader.NIOUploader;
import com.limegroup.gnutella.uploader.PushProxyRequestHandler;
import com.limegroup.gnutella.uploader.UploadSession;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.LimeWireUtils;

public class NIOUploadManager extends AbstractUploadManager implements
        UploadManager, ConnectionAcceptor {

    private final static String SESSION_KEY = "org.limewire.session";

    private HttpIOReactor reactor;

    private HttpParams params;

    private HttpRequestHandlerRegistry registry;

    private ConnectionEventListener listener;

    public NIOUploadManager(UploadSlotManager uploadSlotManager) {
        super(uploadSlotManager);

        initializeReactor();
        inititalizeHandlers();

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
        this.params.setParameter(HttpProtocolParams.USER_AGENT, LimeWireUtils
                .getHttpServer());

        this.registry = new HttpRequestHandlerRegistry();
        this.listener = new ConnectionEventListener();

        // intercepts http requests and responses
        BasicHttpProcessor processor = new BasicHttpProcessor();
        // processor.addInterceptor(new ResponseDate());
        processor.addInterceptor(new ResponseServer());
        processor.addInterceptor(new ResponseContent());
        processor.addInterceptor(new ResponseConnControl());

        HttpServiceHandler serviceHandler = new HttpServiceHandler(processor,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(), params);
        serviceHandler.setConnectionListener(listener);
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
                UploadStat.BROWSE_HOST.incrementStat();
                if (!HttpCoreUtils.hasHeader(request, "Accept",
                        Constants.QUERYREPLY_MIME_TYPE)) {
                    response.setStatusCode(HttpStatus.SC_NOT_ACCEPTABLE);
                } else {
                    response.setEntity(new BrowseRepsonseEntity());
                }
            }
        });

        // update
        registerHandler("/update.xml", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                UploadStat.UPDATE_FILE.incrementStat();
                UploadSession session = getSession(context);
                NIOUploader uploader = new NIOUploader("update.xml", session,
                        -1);

                File file = new File(CommonUtils.getUserSettingsDir(),
                        "update.xml");
                // TODO is the returned mime-type correct?
                response.setEntity(new FileEntity(file,
                        Constants.QUERYREPLY_MIME_TYPE));
            }
        });

        // push-proxy requests
        HttpRequestHandler pushProxyHandler = new PushProxyRequestHandler();
        registerHandler("/gnutella/push-proxy", pushProxyHandler);
        registerHandler("/gnet/push-proxy", pushProxyHandler);

        // uploads
        registerHandler("/get*", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
            }
        });

        // unsupported requests
        HttpRequestHandler notFoundHandler = new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setReasonPhrase("Feature Not Active");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        };
        registerHandler("/browser-control", notFoundHandler);
        registerHandler("/gnutella/file-view*", notFoundHandler);
        registerHandler("/gnutella/res/*", notFoundHandler);

        // return malformed request for everything else
        registerHandler("*", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
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

    public NIOUploader getUploader(HttpContext context, String filename,
            int index) {
        UploadSession session = getSession(context);
        NIOUploader uploader = (NIOUploader) session.getUploader();
        if (uploader != null) {

        } else {
            uploader = new NIOUploader(filename, session, index);
        }
        return uploader;
    }

    public HTTPSession createSession(HttpContext context, HttpRequest reqeust) {
        assert context.getAttribute(SESSION_KEY) == null;
        HTTPSession session = new HTTPSession(null, this);
        context.setAttribute(SESSION_KEY, session);
        return session;
    }

    public UploadSession getSession(HttpContext context) {
        UploadSession session = (UploadSession) context
                .getAttribute(SESSION_KEY);
        assert session != null;
        return session;
    }

    public void registerHandler(final String pattern,
            final HttpRequestHandler handler) {
        this.registry.register(pattern, handler);
    }

    private class ConnectionEventListener implements
            ServerConnectionEventListener {

        public void connectionClosed(NHttpServerConnection conn) {
            UploadSession session = getSession(conn.getContext());
            // TODO close session
        }

        public void connectionOpen(NHttpServerConnection conn) {
            createSession(conn.getContext(), conn.getHttpRequest());
        }

        public void connectionTimeout(NHttpServerConnection conn) {
            throw new RuntimeException();
        }

        public void fatalIOException(NHttpServerConnection conn, IOException e) {
            throw new RuntimeException(e);
        }

        public void fatalProtocolException(NHttpServerConnection conn,
                HttpException e) {
            throw new RuntimeException(e);
        }

    }

}
