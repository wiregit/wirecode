package org.limewire.nio.http;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.nio.BufferUtils;
import org.limewire.nio.NIOSocket;
import org.limewire.nio.channel.ChannelReader;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.WriteObserver;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.util.Sockets;

public class HttpIOReactor implements ConnectingIOReactor {

    static final Log LOG = LogFactory.getLog(HttpIOReactor.class);
    
    private HttpParams params;
    
    protected IOEventDispatch eventDispatch = null;


    public HttpIOReactor(final HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException();
        }
        
        this.params = params;
    }

    
    public void execute(IOEventDispatch eventDispatch) throws IOException {
        if (eventDispatch == null) {
            throw new IllegalArgumentException("Event dispatcher may not be null");
        }
        this.eventDispatch = eventDispatch;

        // start
    }

    public void shutdown() throws IOException {
        // TODO Auto-generated method stub
        
    }

    public SessionRequest connect(SocketAddress remoteAddress,
            SocketAddress localAddress, final Object attachment) {
        if (remoteAddress == null || (!(remoteAddress instanceof InetSocketAddress))) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        
        HttpSessionRequest sessionRequest = new HttpSessionRequest(
                remoteAddress, localAddress, attachment);
        try {
            Sockets.connect((InetSocketAddress) remoteAddress, Constants.TIMEOUT,
                    new ConnectObserver() {
                        public void handleConnect(Socket socket) throws IOException {
                            prepareSocket(socket);
                            connectSocket((NIOSocket) socket, attachment);
                        }

                        public void handleIOException(IOException e) {
                            LOG.error("Unexpected exception", e);
                        }

                        public void shutdown() {
                            
                        }

                    });
        } catch (IOException e) {
            // should never happen since we are connecting in the background
            throw new RuntimeException("Unexpected error", e);
        }
        
        return sessionRequest;
    }

    protected void prepareSocket(final Socket socket) throws IOException {
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(this.params));
        //socket.setSoTimeout(HttpConnectionParams.getSoTimeout(this.params));
        socket.setSoTimeout(0);
        int linger = HttpConnectionParams.getLinger(this.params);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
    }
    
    protected void connectSocket(final NIOSocket socket, Object attachment) throws IOException {
        final HttpIOSession session = new HttpIOSession(socket);        
        int timeout = 0;
        try {
            timeout = socket.getSoTimeout();
        } catch (IOException ex) {
            // Very unlikely to happen and is not fatal
            // as the protocol layer is expected to overwrite
            // this value anyways
        }
        
        session.setAttribute(IOSession.ATTACHMENT_KEY, attachment);
        session.setSocketTimeout(timeout);

        HttpChannel channel = new HttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        
        this.eventDispatch.connected(session);
        
        socket.setReadObserver(channel);
        socket.setWriteObserver(channel);
    }

    static class BufferedChannelWriter implements ChannelWriter {
        
        /**
         * The sink channel we write to & interest ourselves on.
         */
        protected InterestWritableByteChannel source;
        
        protected ByteBuffer buffer;
        
        protected boolean shutdown;

        private WriteObserver observer;
        
        public BufferedChannelWriter(WriteObserver observer, int bufferSize) {
            buffer = ByteBuffer.allocate(bufferSize);
            this.observer = observer;
        }
        
        /** The channel we're writing to. */
        public synchronized InterestWritableByteChannel getWriteChannel() {
            return source;
        }
        
        /** The channel we're writing to. */
        public synchronized void setWriteChannel(InterestWritableByteChannel channel) {
            this.source = channel;
            source.interest(observer, true);
        }
        
        /**
         * Adds <code>data</code> to the buffer and signals interest in writing to
         * the channel.
         * 
         * @throws IOException If the channel is already shutdown
         * @throws BufferOverflowException If there is insufficient space in the buffer 
         */
        public synchronized void put(byte[] data) throws IOException {
            if (shutdown) {
                throw new EOFException();
            }
            
            buffer.put(data);
            
            if(source != null)
                source.interest(observer, true);
        }
        
        
        /**
         * Writes as many messages as possible to the sink.
         */
        public synchronized boolean handleWrite() throws IOException {
            if(source == null)
                throw new IllegalStateException("writing with no source.");
                
            buffer.flip();
            while (buffer.hasRemaining() && source.write(buffer) > 0)
                ;
            
            boolean remaining = buffer.hasRemaining();
            if (remaining) {
                buffer.compact();
                return true;
            } else {
                buffer.clear();
                source.interest(observer, false);
                return false;
            }
        }

        public void handleIOException(IOException iox) {
        }
        
        public void shutdown() {
        }
        
        public void interest(boolean status) {
            if (source != null) {
                source.interest(this, status);
            }
        }

        public int put(ByteBuffer src) throws IOException {
            if (shutdown) {
                throw new EOFException();
            }
            
            buffer.put(src);
            
            if(source != null)
                source.interest(this, true);

            return src.position();
        }

    }

    static class BufferedChannelReader implements ChannelReader {

        protected ByteBuffer buffer;
        protected InterestReadableByteChannel source;
        protected boolean shutdown;
        
        public BufferedChannelReader(int bufferSize) {
            buffer = ByteBuffer.allocate(bufferSize);
        }

        public synchronized int read(ByteBuffer dst) {
            return BufferUtils.transfer(buffer, dst);
        }

        public InterestReadableByteChannel getReadChannel() {
            return source;
        }

        public void setReadChannel(InterestReadableByteChannel newChannel) {
            this.source = newChannel;
        }

        public void handleRead() throws IOException {
            int read = 0;
            while (buffer.hasRemaining() && (read = source.read(buffer)) > 0)
                ;
            if (read == -1) {
                // closed
            }
        }

        public void shutdown() {
        }

        public void interest(boolean status) {
            if (source != null) {
                source.interest(status);
            }
        }
        
    }
    
}
