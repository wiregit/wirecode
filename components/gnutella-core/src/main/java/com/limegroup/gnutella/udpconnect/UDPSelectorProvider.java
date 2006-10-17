package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import com.limegroup.gnutella.io.TransportListener;

public class UDPSelectorProvider extends SelectorProvider {

	private final TransportListener listener;
	
	public UDPSelectorProvider(TransportListener listener) {
		this.listener = listener;
	}

    public DatagramChannel openDatagramChannel() throws IOException {
        throw new IOException("not supported");
    }

    public Pipe openPipe() throws IOException {
        throw new IOException("not supported");
    }

    public AbstractSelector openSelector() {
        return new UDPMultiplexor(this, listener);
    }

    public ServerSocketChannel openServerSocketChannel() throws IOException {
        throw new IOException("not supported");
    }

    public SocketChannel openSocketChannel() {
        return new UDPSocketChannel(this, listener);
    }

}
