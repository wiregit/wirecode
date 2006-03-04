package com.limegroup.gnutella.handshaking;

import java.net.Socket;
import java.util.List;

import com.limegroup.gnutella.io.NIOMultiplexor;

public class AsyncIncomingHandshaker implements Handshaker {

    private HandshakeSupport support;
    private AsyncHandshaker shaker;
    private Socket socket;

    public AsyncIncomingHandshaker(HandshakeResponder responder,
                                   Socket socket,
                                   HandshakeObserver observer) {
        this.socket = socket;
        this.support = new HandshakeSupport(socket.getInetAddress().getHostAddress());
        List states = HandshakeState.getIncomingHandshakeStates(support, responder);
        shaker = new AsyncHandshaker(this, observer, states);
    }

    public void shake() {
        ((NIOMultiplexor)socket).setReadObserver(shaker);
        ((NIOMultiplexor)socket).setWriteObserver(shaker);
    }

    public HandshakeResponse getWrittenHeaders() {
        return support.getWrittenHandshakeResponse();
    }

    public HandshakeResponse getReadHeaders() {
        return support.getReadHandshakeResponse();
    }

}
