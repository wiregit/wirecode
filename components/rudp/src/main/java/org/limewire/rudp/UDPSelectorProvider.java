package org.limewire.rudp;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;


public class UDPSelectorProvider extends SelectorProvider {

    private final RUDPContext context;
    
    public UDPSelectorProvider() {
        this(new DefaultRUDPContext());
    }
    
    public UDPSelectorProvider(RUDPContext context) {
		this.context = context;
	}

    public DatagramChannel openDatagramChannel() throws IOException {
        throw new IOException("not supported");
    }

    public Pipe openPipe() throws IOException {
        throw new IOException("not supported");
    }

    public AbstractSelector openSelector() {
        return new UDPMultiplexor(this, context);
    }

    public ServerSocketChannel openServerSocketChannel() throws IOException {
        throw new IOException("not supported");
    }

    public SocketChannel openSocketChannel() {
        return new UDPSocketChannel(this, context);
    }

}
