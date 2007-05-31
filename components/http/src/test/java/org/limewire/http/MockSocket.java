package org.limewire.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;

public class MockSocket extends AbstractNBSocket {

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