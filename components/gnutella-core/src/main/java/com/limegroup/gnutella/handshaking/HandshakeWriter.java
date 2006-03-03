package com.limegroup.gnutella.handshaking;

import java.io.IOException;

import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestWriteChannel;

class HandshakeWriter implements ChannelWriter {
    
    private GnetConnectObserver gnetObserver;
    private HandshakeResponder responder;
    private HandshakeReader reader;
    private int state;

    HandshakeWriter(HandshakeResponder responder, HandshakeReader reader, GnetConnectObserver gnetObserver) {
        this.responder = responder;
        this.reader = reader;
        this.gnetObserver = gnetObserver;
    }
    
    void setState(int state) {
        this.state = state;
    }
    
    HandshakeResponse getWrittenHandshakeResponse() {
        return null;
    }

    public void setWriteChannel(InterestWriteChannel newChannel) {
        // TODO Auto-generated method stub

    }

    public InterestWriteChannel getWriteChannel() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean handleWrite() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    public void handleIOException(IOException iox) {
        // TODO Auto-generated method stub

    }

    public void shutdown() {
        // TODO Auto-generated method stub

    }

}
