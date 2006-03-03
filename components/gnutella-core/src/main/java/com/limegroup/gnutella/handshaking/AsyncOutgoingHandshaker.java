package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.io.NIOMultiplexor;

public class AsyncOutgoingHandshaker implements Handshaker {

    private Properties requestHeaders;
    private HandshakeResponder responder;
    private NIOMultiplexor multiplexor;
    private GnetConnectObserver observer;

    public AsyncOutgoingHandshaker(Properties requestHeaders, HandshakeResponder responder,
                            NIOMultiplexor multiplexor, GnetConnectObserver observer) {
        this.requestHeaders = requestHeaders;
        this.responder = responder;
        this.multiplexor = multiplexor;
        this.observer = observer;
    }

    public void shake() {
        // TODO Auto-generated method stub

    }

    public HandshakeResponse getWrittenHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    public HandshakeResponse getReadHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

}
