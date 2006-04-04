package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

public class UDPSelectorProvider extends SelectorProvider {

    private static final UDPSelectorProvider instance = new UDPSelectorProvider();
    
    public static UDPSelectorProvider instance() {
        return instance;
    }

    public DatagramChannel openDatagramChannel() throws IOException {
        throw new IOException("not supported");
    }

    public Pipe openPipe() throws IOException {
        throw new IOException("not supported");
    }

    public AbstractSelector openSelector() {
        return new UDPMultiplexor(this);
    }

    public ServerSocketChannel openServerSocketChannel() throws IOException {
        throw new IOException("not supported");
    }

    public SocketChannel openSocketChannel() {
        return new UDPSocketChannel(this);
    }

}
