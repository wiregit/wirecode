package org.limewire.rudp;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;


public class UDPSelectorProvider extends SelectorProvider {
    /** A factory so the outside world can change the default UDPSelectorProvider. */
    private static volatile UDPSelectorProviderFactory providerFactory = null;
    
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

    public UDPMultiplexor openSelector() {
        UDPMultiplexor plexor = new UDPMultiplexor(this, context);
        return plexor;
    }

    public ServerSocketChannel openServerSocketChannel() throws IOException {
        throw new IOException("not supported");
    }

    public UDPSocketChannel openSocketChannel() {
        return new UDPSocketChannel(this, context);
    }
    
    public Class<UDPSocketChannel> getUDPSocketChannelClass() {
        return UDPSocketChannel.class;
    }
    
    public RUDPContext getContext() {
        return context;
    }
    
    /** Retrieves the default provider. */
    public static UDPSelectorProvider defaultProvider() {
        if(providerFactory != null)
            return providerFactory.createProvider();
        else
            return new UDPSelectorProvider();
    }
    
    /** Sets the new default provider factory. */
    public static void setDefaultProviderFactory(UDPSelectorProviderFactory factory) {
        providerFactory = factory;
    }
    
    /** Gets the last provider factory. */
    public static UDPSelectorProviderFactory getDefaultProviderFactory() {
        return providerFactory;
    }

}
