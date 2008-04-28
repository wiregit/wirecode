package org.limewire.rudp;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Service-provider class for {@link UDPMultiplexor} selectors and 
 * {@link UDPSocketChannel} selectable channels.
 */
@Singleton
public class UDPSelectorProvider extends SelectorProvider {    
    private final RUDPContext context;
    
    public UDPSelectorProvider() {
        this(new DefaultRUDPContext());
    }
    
    @Inject
    public UDPSelectorProvider(RUDPContext context) {
		this.context = context;
	}

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        throw new IOException("not supported");
    }

    @Override
    public Pipe openPipe() throws IOException {
        throw new IOException("not supported");
    }

    @Override
    public UDPMultiplexor openSelector() {
        UDPMultiplexor plexor = new UDPMultiplexor(this, context);
        return plexor;
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        throw new IOException("not supported");
    }

    @Override
    public AbstractNBSocketChannel openSocketChannel() {
        return new UDPSocketChannel(this, context);
    }
    
    public Class<UDPSocketChannel> getUDPSocketChannelClass() {
        return UDPSocketChannel.class;
    }
    
    public RUDPContext getContext() {
        return context;
    }
}
