package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;

import org.limewire.concurrent.AtomicLazyReference;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.BufferReader;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.util.BufferUtils;

/** Contains a collection of SSL-related utilites. */
public class SSLUtils {
    
    private SSLUtils() {}
        
    private static final Executor TLS_PROCESSOR = ExecutorsHelper.newProcessingQueue("TLSProcessor");
    private static final AtomicLazyReference<SSLContext> TLS_CONTEXT = new AtomicLazyReference<SSLContext>() {
        @Override
        protected SSLContext createObject() {
                try {
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, null, null);
                    // TODO: Set the SSLSessionContext cache size, or timeout?
                    return context;
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                } catch (KeyManagementException e) {
                    throw new IllegalStateException(e);
                }
        }        
    };
    
    /** Returns the shared Executor for processing tasks from the SSLEngine. */
    public static Executor getExecutor() {
        return TLS_PROCESSOR;
    }
    
    /** Returns the shared TLS context. */
    public static SSLContext getTLSContext() {
        return TLS_CONTEXT.get();
    }
    
    /** Returns true is the given socket is already using TLS. */
    public static boolean isTLSEnabled(Socket socket) {
        return socket instanceof TLSNIOSocket;
    }
    
    /** Returns true if we are capable of performing a startTLS operation on this socket. */
    public static boolean isStartTLSCapable(Socket socket) {
        return socket instanceof AbstractNBSocket;
    }
    
    /**
     * Wraps an existing socket in a TLS-enabled socket.
     * Any data within 'data' will be pushed into the TLS layer.
     * 
     * This currently only works for creating server-side TLS sockets.
     * 
     * You must ensure that isTLSCapable returns true for the socket,
     * otherwise an IllegalArgumentException is thrown.
     */ 
    public static TLSNIOSocket startTLS(Socket socket, ByteBuffer data) throws IOException {
        if(socket instanceof AbstractNBSocket) {
            TLSNIOSocket tlsSocket = new TLSNIOSocket(socket);
            // Tell the channel to read in the buffered data.
            if(data.hasRemaining()) {
                SSLReadWriteChannel sslChannel = tlsSocket.getSSLChannel();
                InterestReadableByteChannel oldReader = sslChannel.getReadChannel();
                sslChannel.setReadChannel(new BufferReader(data));
                sslChannel.read(BufferUtils.getEmptyBuffer());
                if(data.hasRemaining())
                    throw new IllegalStateException("unable to read all prebuffered data in one pass!");
                sslChannel.setReadChannel(oldReader);
            }
            return tlsSocket;
        } else {
            throw new IllegalArgumentException("cannot wrap non AbstractNBSocket");
        }
    }
    
    /**
     * Returns a tracker for the given socket.
     * If no SSL exchanges are performed on the socket, the returned
     * tracker will always report 0 bytes produced and consumed.
     */
    public static SSLBandwidthTracker getSSLBandwidthTracker(Socket socket) {
        if(socket instanceof TLSNIOSocket) {
           return new SSLChannelTracker(((TLSNIOSocket)socket).getSSLChannel());
        } else {
            return EmptyTracker.instance();
        }
    }
    
    public static class EmptyTracker implements SSLBandwidthTracker {
        private static final EmptyTracker instance = new EmptyTracker();
        public static final EmptyTracker instance() { return instance; }
        private EmptyTracker() {}
        public long getReadBytesConsumed() { return 0; }
        public long getReadBytesProduced() { return 0; }
        public long getWrittenBytesConsumed() { return 0; }
        public long getWrittenBytesProduced() { return 0; }
    }
    
    private static class SSLChannelTracker implements SSLBandwidthTracker {
        private final SSLReadWriteChannel channel;
        
        SSLChannelTracker(SSLReadWriteChannel channel) {
            this.channel = channel;
        }

        public long getReadBytesConsumed() {
            return channel.getReadBytesConsumed();
        }

        public long getReadBytesProduced() {
            return channel.getReadBytesProduced();
        }

        public long getWrittenBytesConsumed() {
            return channel.getWrittenBytesConsumed();
        }

        public long getWrittenBytesProduced() {
            return channel.getWrittenBytesProduced();
        }
    }
}
