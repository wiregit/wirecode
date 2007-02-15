package org.limewire.nio.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.io.IOUtils;
import org.limewire.nio.NIOSocket;
import org.limewire.nio.observer.ConnectObserver;

import com.limegroup.gnutella.ConnectionAcceptor;
import com.limegroup.gnutella.util.Sockets;

public class HttpIOReactor implements ConnectingIOReactor, ConnectionAcceptor {

    static final Log LOG = LogFactory.getLog(HttpIOReactor.class);
    
    private HttpParams params;
    
    protected IOEventDispatch eventDispatch = null;


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

    public HttpSessionRequest createSession(SocketAddress remoteAddress,
            SocketAddress localAddress, final Object attachment) {
        if (remoteAddress == null || (!(remoteAddress instanceof InetSocketAddress))) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        final HttpSessionRequest sessionRequest = new HttpSessionRequest(
                remoteAddress, localAddress, attachment);
        return sessionRequest;
    }
    
    public SessionRequest connect(SocketAddress remoteAddress,
            SocketAddress localAddress, final Object attachment) {
        HttpSessionRequest sessionRequest = createSession(remoteAddress, localAddress, attachment);
        connect(sessionRequest);
        return sessionRequest;
    }
    
    public void connect(final HttpSessionRequest sessionRequest) {
        try {
            Sockets.connect((InetSocketAddress) sessionRequest.getRemoteAddress(), 
                    sessionRequest.getConnectTimeout(),
                    new ConnectObserver() {
                        public void handleConnect(Socket socket) throws IOException {                          
                            prepareSocket(socket);
                            HttpIOSession session = connectSocket((NIOSocket) socket, sessionRequest.getAttachment());
                            sessionRequest.connected(session);
                        }

                        public void handleIOException(IOException e) {
                            LOG.error("Unexpected exception", e);
                            sessionRequest.failed(e);
                        }

                        public void shutdown() {
                            sessionRequest.shutdown();
                        }

                    });
        } catch (IOException e) {
            // should never happen since we are connecting in the background
            LOG.error("Unexpected exception", e);
            sessionRequest.failed(e);
        }
    }

    protected void prepareSocket(final Socket socket) throws IOException {
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(this.params));
        socket.setSoTimeout(HttpConnectionParams.getSoTimeout(this.params));
        socket.setSoTimeout(0);
        int linger = HttpConnectionParams.getLinger(this.params);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
    }
    
    protected HttpIOSession connectSocket(final NIOSocket socket, Object attachment) throws IOException {
        final HttpIOSession session = new HttpIOSession(socket);        
        int timeout = 0;
        try {
            timeout = socket.getSoTimeout();
        } catch (IOException ex) {
            // Very unlikely to happen and is not fatal
            // as the protocol layer is expected to overwrite
            // this value anyways
        }
        
        session.setAttribute(IOSession.ATTACHMENT_KEY, attachment);
        session.setSocketTimeout(timeout);

        HttpChannel channel = new HttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        
        this.eventDispatch.connected(session);
        
        socket.setReadObserver(channel);
        socket.setWriteObserver(channel);
        
        return session;
    }


    public void acceptConnection(String word, Socket socket) {
        try {
            prepareSocket(socket);
            connectSocket((NIOSocket) socket, null);
        } catch (IOException e) {
            IOUtils.close(socket);
        }
    }
    
}
