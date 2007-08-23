package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.BasicHttpAcceptor;
import org.limewire.http.HttpAcceptorListener;
import org.limewire.http.HttpIOSession;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.NIODispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HTTPConnectionData;
import com.limegroup.gnutella.http.HttpContextParams;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Processes HTTP requests for Gnutella uploads.
 */
@Singleton
public class HTTPAcceptor extends BasicHttpAcceptor {

    private static final Log LOG = LogFactory.getLog(HTTPAcceptor.class);

    private static final String[] SUPPORTED_METHODS = new String[] { "GET",
            "HEAD", };

    private final HttpRequestHandler notFoundHandler;

    @Inject
    public HTTPAcceptor(Provider<ConnectionDispatcher> connectionDispatcher) {
        super(connectionDispatcher, false, createDefaultParams(LimeWireUtils.getHttpServer(),
                Constants.TIMEOUT), SUPPORTED_METHODS);

        this.notFoundHandler = new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                UploadStat.FILE_NOT_FOUND.incrementStat();

                response.setReasonPhrase("Feature Not Active");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        };
        
        addAcceptorListener(new ConnectionEventListener());
        addRequestInterceptor(new RequestStatisticTracker());
        addResponseInterceptor(new HeaderStatisticTracker());

        inititalizeDefaultHandlers();
    }

    private void inititalizeDefaultHandlers() {
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
     * Handles an incoming HTTP push request. This needs to be called from the
     * NIO thread.
     */
    public void acceptConnection(Socket socket, HTTPConnectionData data) {
        assert NIODispatcher.instance().isDispatchThread();

        if (getReactor() == null) {
            LOG.warn("Received upload request before reactor was initialized");
            return;
        }
        
        DefaultNHttpServerConnection conn = getReactor().acceptConnection(null,
                socket);
        if (conn != null)
            HttpContextParams.setConnectionData(conn.getContext(), data);
    }

    /**
     * Returns a handler that responds with a HTTP 404 error.
     */
    public HttpRequestHandler getNotFoundHandler() {
        return notFoundHandler;
    }

    /**
     * Forwards events from the underlying protocol layer to acceptor event
     * listeners.
     */
    private class ConnectionEventListener implements HttpAcceptorListener {

        public void connectionOpen(NHttpConnection conn) {
        }

        public void connectionClosed(NHttpConnection conn) {
        }

        public void connectionTimeout(NHttpConnection conn) {
        }

        public void fatalIOException(IOException e, NHttpConnection conn) {
            if (HttpContextParams.isPush(conn.getContext())) {
                if (HttpContextParams.isFirewalled(conn.getContext())) {
                    UploadStat.FW_FW_FAILURE.incrementStat();
                }
                UploadStat.PUSH_FAILED.incrementStat();
            }
        }

        public void fatalProtocolException(HttpException e, NHttpConnection conn) {
        }

        public void requestReceived(NHttpConnection conn, HttpRequest request) {
        }

        public void responseSent(NHttpConnection conn, HttpResponse response) {
            HttpIOSession session = HttpContextParams.getIOSession(conn
                    .getContext());
            session
                    .setSocketTimeout(SharingSettings.PERSISTENT_HTTP_CONNECTION_TIMEOUT
                            .getValue());
            session.setThrottle(null);
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
         * XXX iterating over all headers is rather inefficient since the size
         * of the headers is known in
         * DefaultNHttpServerConnection.submitResponse() but can't be easily
         * made accessible
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
