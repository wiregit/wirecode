package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Helper class that implements all of the methods of SocketChannel but that is
 * not actually connected to the network.  Subclasses can easily override 
 * whichever methods they wish to provide custom functionality.
 */
public class TestChannel extends SocketChannel {
    
    public TestChannel()  {
        super(null);
    }
    
    // stub method
    public boolean finishConnect() throws IOException {
        return false;
    }
    
    // stub method
    public boolean isConnected() {
        return false;
    }
    
    // stub method
    public boolean isConnectionPending() {
        return false;
    }
    
    // stub method
    public Socket socket() {
        return null;
    }
    
    // stub method
    public boolean connect(SocketAddress arg0) throws IOException {
        return false;
    }
    
    // stub method
    public int read(ByteBuffer buf) throws IOException {
        
        return -1;
    }
    
    // stub method
    public int write(ByteBuffer arg0) throws IOException {
        return 0;
    }
    
    // stub method
    public long read(ByteBuffer[] arg0, int arg1, int arg2) 
        throws IOException {
        return 0;
    }
    
    // stub method
    public long write(ByteBuffer[] arg0, int arg1, int arg2) 
        throws IOException {
        return 0;
    }
    
    // stub method
    protected void implCloseSelectableChannel() throws IOException {       
    }
    
    // stub method
    protected void implConfigureBlocking(boolean arg0) throws IOException { 
    }
}
