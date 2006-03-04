package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.WriteObserver;

class AsyncHandshaker implements ChannelReadObserver, ChannelWriter, InterestWriteChannel, InterestReadChannel {

    private Handshaker shaker;
    private HandshakeObserver handshakeObserver;
    private List states;

    AsyncHandshaker(Handshaker shaker, HandshakeObserver observer, List states) {
        this.shaker = shaker;
        this.handshakeObserver = observer;
        this.states = states;
    }    
    
    public void handleRead() throws IOException {
        // TODO Auto-generated method stub
        
    }

    public InterestWriteChannel getWriteChannel() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setWriteChannel(InterestWriteChannel newChannel) {
        // TODO Auto-generated method stub
        
    }

    public InterestReadChannel getReadChannel() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setReadChannel(InterestReadChannel newChannel) {
        // TODO Auto-generated method stub
        
    }

    public AsyncHandshaker() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void interest(WriteObserver observer, boolean status) {
        // TODO Auto-generated method stub

    }

    public int write(ByteBuffer src) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    public void close() throws IOException {
        // TODO Auto-generated method stub

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

    public void interest(boolean status) {
        // TODO Auto-generated method stub

    }

    public int read(ByteBuffer dst) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

}
