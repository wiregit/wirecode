package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;

import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.InterestReadChannel;

class HandshakeReader implements ChannelReadObserver, ReadableByteChannel {

    private HandshakeWriter writer;
    private GnetConnectObserver gnetObserver;
    private InterestReadChannel channel;
    private boolean shutdown;
    private ByteBuffer buffer;
    private CharsetDecoder decoder;
    private CharBuffer charBuffer;
    private int state;
    
    
    HandshakeReader(GnetConnectObserver observer) {
        this.gnetObserver = observer;
    }
    
    void setHandshakeWriter(HandshakeWriter writer) {
        this.writer = writer;
    }
    
    void setState(int state) {
        this.state = state;
    }
    
    HandshakeResponse getReadHanshakeResponse() {
        return null;
    }

    public void handleRead() throws IOException {
        // TODO Auto-generated method stub

    }

    public void handleIOException(IOException iox) {
        // TODO Auto-generated method stub

    }

    public void shutdown() {
        // TODO Auto-generated method stub

    }

    public void setReadChannel(InterestReadChannel newChannel) {
        // TODO Auto-generated method stub

    }

    public InterestReadChannel getReadChannel() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public int read(ByteBuffer dst) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }
    

}
