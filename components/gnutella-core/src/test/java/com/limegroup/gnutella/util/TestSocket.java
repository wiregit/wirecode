package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

/**
 * Helper class that creates a socket that returns a "dummy" socket and
 * channel -- useful for testing.  This will always return the local host
 * address as the InetAddress and will always return an empty channel that
 * is not really connected to anything else.  You can also supply your own
 * dummy channel for custom tests.
 */
public class TestSocket extends Socket {
   
    private final SocketChannel CHANNEL;
    
    public TestSocket() {
        CHANNEL = new TestChannel();
    }
    
    public TestSocket(SocketChannel channel) {
        CHANNEL = channel;
    }
    
    public SocketChannel getChannel()   {
        return CHANNEL;
    } 
    
    public InetAddress getInetAddress()   {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // this should not happen
            return null;
        }
    } 
}
