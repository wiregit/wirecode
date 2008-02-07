package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
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
import org.limewire.nio.NIODispatcher;
import org.limewire.statistic.Statistic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HTTPConnectionData;
import com.limegroup.gnutella.http.HttpContextParams;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics.StatisticType;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Processes HTTP requests for Gnutella uploads.
 */
@Singleton
public class HTTPAcceptor extends BasicHttpAcceptor {

    private static final Log LOG = LogFactory.getLog(HTTPAcceptor.class);

    private static final String[] SUPPORTED_METHODS = new String[] { "GET", "HEAD", };

    private final HttpRequestHandler notFoundHandler;

    @Inject
    public HTTPAcceptor(TcpBandwidthStatistics tcpBandwidthStatistics) {
        super(createDefaultParams(LimeWireUtils.getHttpServer(), Constants.TIMEOUT), SUPPORTED_METHODS);

        this.notFoundHandler = new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setReasonPhrase("Feature Not Active");
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        };
        
        addAcceptorListener(new ConnectionEventListener());
        if(tcpBandwidthStatistics != null)
            addResponseInterceptor(new HeaderStatisticTracker(tcpBandwidthStatistics));
        else
            LOG.warn("Not tracking TCP header bandwidth!");

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
        }

        public void fatalProtocolException(HttpException e, NHttpConnection conn) {
        }

        public void requestReceived(NHttpConnection conn, HttpRequest request) {
        }

        public void responseSent(NHttpConnection conn, HttpResponse response) {
            HttpIOSession session = HttpContextParams.getIOSession(conn.getContext());
            session.setSocketTimeout(SharingSettings.PERSISTENT_HTTP_CONNECTION_TIMEOUT.getValue());
            session.setThrottle(null);
        }

    }

    /**
     * Tracks the bandwidth used when sending a response.
     */
    private class HeaderStatisticTracker implements HttpResponseInterceptor {
        private final Statistic headerUpstream;
        
        HeaderStatisticTracker(TcpBandwidthStatistics tcpBandwidthStatistics) {
            this.headerUpstream = tcpBandwidthStatistics.getStatistic(StatisticType.HTTP_HEADER_UPSTREAM);
        }

        /*
         * XXX iterating over all headers is rather inefficient since the size
         * of the headers is known in
         * DefaultNHttpServerConnection.submitResponse() but can't be easily
         * made accessible
         */
        public void process(HttpResponse response, HttpContext context) throws HttpException,
                IOException {
            for (Iterator it = response.headerIterator(); it.hasNext();) {
                Header header = (Header) it.next();
                headerUpstream.addData(header.getName().length() + 2 + header.getValue().length());
            }
        }

    }

}
