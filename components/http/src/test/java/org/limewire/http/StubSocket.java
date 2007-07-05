package org.limewire.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;

public class StubSocket extends AbstractNBSocket {

    private StubChannel channel;

    public StubSocket() {
        channel = new StubChannel();
    }
    
    @Override
    public SocketChannel getChannel() {
        return channel;
    }
    
    @Override
    public SocketAddress getRemoteSocketAddress() {
        return new InetSocketAddress(getInetAddress(), getPort());
    }

    @Override
    protected InterestReadableByteChannel getBaseReadChannel() {
        return null;
    }

    @Override
    protected InterestWritableByteChannel getBaseWriteChannel() {
        return null;
    }

    @Override
    protected void shutdownImpl() {
    }
    
}
