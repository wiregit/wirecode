package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.io.NIOMultiplexor;

public class AsyncIncomingHandshaker implements Handshaker {

    private NIOMultiplexor multiplexor;
    private HandshakeReader reader;
    private HandshakeWriter writer;

    public AsyncIncomingHandshaker(HandshakeResponder responder,
                                   NIOMultiplexor multiplexor, GnetConnectObserver observer) {
        this.multiplexor = multiplexor;
        HandshakeReader reader = new HandshakeReader(observer);
        HandshakeWriter writer = new HandshakeWriter(responder, reader, observer);
        reader.setHandshakeWriter(writer);
        writer.setState(HandshakeState.startIncoming());
        reader.setState(HandshakeState.startIncoming());
    }

    public void shake() {
        multiplexor.setReadObserver(reader);
        multiplexor.setWriteObserver(writer);
    }

    public HandshakeResponse getWrittenHeaders() {
        return writer.getWrittenHandshakeResponse();
    }

    public HandshakeResponse getReadHeaders() {
        return reader.getReadHanshakeResponse();
    }

}
