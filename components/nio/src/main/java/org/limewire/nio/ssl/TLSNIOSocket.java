package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.NIOSocket;
import org.limewire.nio.channel.BufferReader;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.util.BufferUtils;

/**
 * A version of NIOSocket that uses TLS for transfer encoding.
 * 
 * This is currently hardcoded to only support the cipher suite:
 *  - TLS_DH_anon_WITH_AES_128_CBC_SHA
 */
public class TLSNIOSocket extends NIOSocket {
    
    private volatile SSLReadWriteChannel tlsLayer;
    private volatile InterestReadableByteChannel baseReader;
    private volatile InterestWritableByteChannel baseWriter;

    public TLSNIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public TLSNIOSocket(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }

    public TLSNIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public TLSNIOSocket(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }

    public TLSNIOSocket() throws IOException {
        super();
    }
    
    TLSNIOSocket(Socket socket) {
        super(socket);
    }
    
    /**
     * Wraps an existing socket in a TLS-enabled socket.
     * Useful for STARTTLS or otherwise converting an existing connection
     * to a secure one.
     * 
     * This currently only works for creating server-side TLS sockets.
     */ 
    public static TLSNIOSocket wrap(Socket socket, ByteBuffer data) throws IOException {
        if(socket instanceof AbstractNBSocket) {
            TLSNIOSocket tlsSocket = new TLSNIOSocket(socket);
            if(data.hasRemaining()) {
                InterestReadableByteChannel oldReader = tlsSocket.tlsLayer.getReadChannel();
                tlsSocket.tlsLayer.setReadChannel(new BufferReader(data));
                tlsSocket.tlsLayer.read(BufferUtils.getEmptyBuffer());
                if(data.hasRemaining())
                    throw new IllegalStateException("unable to read all prebuffered data in one pass!");
                tlsSocket.tlsLayer.setReadChannel(oldReader);
            }
            return tlsSocket;
        } else {
            throw new IllegalArgumentException("cannot wrap non AbstractNBSocket");
        }
        
    }
    
    @Override
    public boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) {
        return super.connect(addr, timeout, new TLSConnectInitializer(addr, observer));
    }
    
    @Override
    protected InterestReadableByteChannel getBaseReadChannel() {
        if(baseReader == null) {
            tlsLayer.setReadChannel(super.getBaseReadChannel());
            baseReader = tlsLayer;
        }
        return baseReader;
    }

    @Override
    protected InterestWritableByteChannel getBaseWriteChannel() {
        if(baseWriter == null) {
            tlsLayer.setWriteChannel(super.getBaseWriteChannel());
            baseWriter = tlsLayer;
        }
        return baseWriter;
    }
    
    @Override
    protected void initIncomingSocket() {
        super.initIncomingSocket();
        tlsLayer = new SSLReadWriteChannel(SSLUtils.getTLSContext(), SSLUtils.getExecutor());
        tlsLayer.initialize(getRemoteSocketAddress(), new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
    }

    @Override
    protected void initOutgoingSocket() throws IOException {
        super.initOutgoingSocket();
        tlsLayer = new SSLReadWriteChannel(SSLUtils.getTLSContext(), SSLUtils.getExecutor());
    }
    
    /**
     * A delegating connector that forces the TLS Layer to be initialized
     * prior to informing the real ConnectObserver about the connection.
     */
    private class TLSConnectInitializer implements ConnectObserver {
        private final ConnectObserver delegate;
        private final SocketAddress addr;
        
        public TLSConnectInitializer(SocketAddress addr, ConnectObserver delegate) {
            this.delegate = delegate;
            this.addr = addr;
        }

        public void handleConnect(Socket socket) throws IOException {
            tlsLayer.initialize(addr, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
            delegate.handleConnect(socket);
        }

        public void handleIOException(IOException iox) {
            delegate.handleIOException(iox);
        }

        public void shutdown() {
            delegate.shutdown();
        }
        
    }

}
