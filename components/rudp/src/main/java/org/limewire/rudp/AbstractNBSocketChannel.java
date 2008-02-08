package org.limewire.rudp;

import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import org.limewire.nio.AbstractNBSocket;

public abstract class AbstractNBSocketChannel extends SocketChannel {
    
    public AbstractNBSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    public abstract AbstractNBSocket socket(); 

}
