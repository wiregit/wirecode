package org.limewire.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.reactor.SessionRequestImpl;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.io.IOUtils;
import org.limewire.nio.AbstractNBSocket;

/**
 * An implementation of the {@link ConnectingIOReactor} interface that
 * establishes connections through LimeWire's NIO layer.
 */
public class HttpIOReactor implements ConnectingIOReactor {

    public static final String IO_SESSION_KEY = "org.limewire.iosession";

    static final Log LOG = LogFactory.getLog(HttpIOReactor.class);
    
    private HttpParams params;
    
    protected IOEventDispatch eventDispatch = null;

    // XXX copied from DefaultServerIOEventDispatch
    private static final String NHTTP_CONN = "NHTTP_CONN";
    
    public HttpIOReactor(final HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException();
        }
        
        this.params = params;
    }

    
    public void execute(IOEventDispatch eventDispatch) throws IOException {
        if (eventDispatch == null) {
            throw new IllegalArgumentException("Event dispatcher may not be null");
        }
        this.eventDispatch = eventDispatch;
    }

    public HttpParams getHttpParams() {
        return params;
    }
    
    public void shutdown() throws IOException {
    }

    public SessionRequest connect(SocketAddress remoteAddress,
            SocketAddress localAddress, Object attachment,
            SessionRequestCallback callback) {
        if (remoteAddress == null || (!(remoteAddress instanceof InetSocketAddress))) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        final SessionRequestImpl sessionRequest = new SessionRequestImpl(
                remoteAddress, localAddress, attachment, callback);
        connect(sessionRequest);
        return sessionRequest;
    }   

    // FIXME move Sockets class to NIO component
    protected void connect(final SessionRequestImpl sessionRequest) {
//        try {
//            Sockets.connect((InetSocketAddress) sessionRequest.getRemoteAddress(), 
//                    sessionRequest.getConnectTimeout(),
//                    new ConnectObserver() {
//                        public void handleConnect(Socket socket) throws IOException {                          
//                            prepareSocket(socket);
//                            DefaultNHttpServerConnection conn = connectSocket((NIOSocket) socket, sessionRequest.getAttachment(), "");
//                            sessionRequest.completed((IOSession) conn.getContext().getAttribute(IO_SESSION_KEY));
//                        }
//
//                        public void handleIOException(IOException e) {
//                            LOG.error("Unexpected exception", e);
//                            sessionRequest.failed(e);
//                        }
//
//                        public void shutdown() {
//                            sessionRequest.timedout();
//                        }
//
//                    });
//        } catch (IOException e) {
//            // should never happen since we are connecting in the background
//            LOG.error("Unexpected exception", e);
//            sessionRequest.failed(e);
//        }
        throw new UnsupportedOperationException();
    }

    protected void prepareSocket(final Socket socket) throws IOException {
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(this.params));
        int linger = HttpConnectionParams.getLinger(this.params);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
    }
    
    protected DefaultNHttpServerConnection connectSocket(AbstractNBSocket socket, Object attachment, String word) {
        final HttpIOSession session = new HttpIOSession(socket);        
        
        session.setAttribute(IOSession.ATTACHMENT_KEY, attachment);
        session.setSocketTimeout(HttpConnectionParams.getSoTimeout(this.params));
        
        HttpChannel channel = new HttpChannel(session, eventDispatch, word);
        session.setHttpChannel(channel);
        
        this.eventDispatch.connected(session);
        
        // need to enable access to the channel for throttling support
        DefaultNHttpServerConnection conn = (DefaultNHttpServerConnection) session.getAttribute(NHTTP_CONN);
        assert conn != null;
        conn.getContext().setAttribute(IO_SESSION_KEY, session);
        
        socket.setReadObserver(channel);
        socket.setWriteObserver(channel);
        
        return conn;
    }

    public DefaultNHttpServerConnection acceptConnection(String word, Socket socket) {
        try {
            prepareSocket(socket);
            return connectSocket((AbstractNBSocket) socket, null, word);
        } catch (IOException e) {
            IOUtils.close(socket);
            return null;
        }
    }

}
