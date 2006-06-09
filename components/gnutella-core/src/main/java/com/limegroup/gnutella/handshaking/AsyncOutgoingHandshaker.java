package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Properties;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.io.IOStateMachine;
import com.limegroup.gnutella.io.IOStateObserver;
import com.limegroup.gnutella.io.NIOMultiplexor;

public class AsyncOutgoingHandshaker implements Handshaker, IOStateObserver {

    private final HandshakeSupport support;
    private final IOStateMachine shaker;
    private final Socket socket;
    private final HandshakeObserver observer;

    public AsyncOutgoingHandshaker(Properties requestHeaders, HandshakeResponder responder,
                                   Socket socket, HandshakeObserver observer) {
        this.socket = socket;
        this.support = new HandshakeSupport(socket.getInetAddress().getHostAddress());
        List states = HandshakeState.getOutgoingHandshakeStates(support, requestHeaders, responder);
        this.shaker = new IOStateMachine(this, states);
        this.observer = observer;
    }

    public void shake() throws SocketException {
        socket.setSoTimeout(Constants.TIMEOUT);
        ((NIOMultiplexor)socket).setReadObserver(shaker);
        ((NIOMultiplexor)socket).setWriteObserver(shaker);
    }

    public HandshakeResponse getWrittenHeaders() {
        return support.getWrittenHandshakeResponse();
    }

    public HandshakeResponse getReadHeaders() {
        return support.getReadHandshakeResponse();
    }
    
    public void handleStatesFinished() {
        observer.handleHandshakeFinished(this);
    }

    public void handleIOException(IOException iox) {
        if(iox instanceof NoGnutellaOkException) {
            NoGnutellaOkException ngok = (NoGnutellaOkException)iox;
            observer.handleNoGnutellaOk(ngok.getCode(), ngok.getMessage());
        } else {
            observer.handleBadHandshake();
        }
    }

    public void shutdown() {
        observer.shutdown();
    }    
}
