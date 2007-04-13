package org.limewire.nio.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import junit.framework.Test;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ManagedThread;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.ReadBufferChannel;
import org.limewire.nio.channel.WriteBufferChannel;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.util.BaseTestCase;

public class SSLReadWriteChannelTest extends BaseTestCase {
    
    public SSLReadWriteChannelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SSLReadWriteChannelTest.class);
    }
    
    public void testClientTLS() throws Exception {
        doTLSTest(false);
    }
    
    public void testServerTLS() throws Exception {
        doTLSTest(true);
    }
    
    //NOTE -- Many of the tests offload initializing, writing & reading to the NIODispatcher
    //        Thread.  This is because after the SSLReadWriteChannel finishes a task, if there
    //        was buffered read data it will trigger a read in that thread.  If there are other
    //        reads occurring in other threads, that can cause internal corruption in the
    //        SSLEngine.  So, all reading/writing/initializing must be done in a single thread.    
    
    public void testClientTLSFails() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        SSLReadWriteChannel channel = new SSLReadWriteChannel(context, executor);
        channel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
        
        ReadBufferChannel readSink = new ReadBufferChannel("NOT TLS DATA!".getBytes());
        WriteBufferChannel writeSink = new WriteBufferChannel(300);
        channel.setReadChannel(readSink);
        channel.setWriteChannel(writeSink);
        new WriteBufferChannel("OUTGOING".getBytes(), channel);
        
        // Trigger the beginning of the handshake.
        channel.handleWrite();
        
        ByteBuffer readTo = ByteBuffer.allocate(200);
        try {
            channel.read(readTo);
            fail("should have failed!");
        } catch(SSLException expected) {}
    }
    
    public void testServerTLSFails() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        SSLReadWriteChannel channel = new SSLReadWriteChannel(context, executor);
        channel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
        
        ReadBufferChannel readSink = new ReadBufferChannel("NOT TLS DATA!".getBytes());
        channel.setReadChannel(readSink);
        
        ByteBuffer readTo = ByteBuffer.allocate(200);
        try {
            channel.read(readTo);
            fail("should have failed!");
        } catch(SSLException expected) {}
    }
    
    public void testNonBlockingTLSExchange() throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel clientChannel = new SSLReadWriteChannel(context, executor);
        final SSLReadWriteChannel serverChannel = new SSLReadWriteChannel(context, executor);
        
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                clientChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, true, false);
                serverChannel.initialize(null, new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, false, false);
            }
        }).get();
        
        Pipe clientToServer = Pipe.open();
        Pipe serverToClient = Pipe.open();
        
        serverToClient.source().configureBlocking(false);
        serverToClient.sink().configureBlocking(false);
        clientToServer.source().configureBlocking(false);
        clientToServer.sink().configureBlocking(false);
        
        clientChannel.setReadChannel(new IRWrapper(serverToClient.source()));
        clientChannel.setWriteChannel(new IWWrapper(clientToServer.sink()));
        serverChannel.setReadChannel(new IRWrapper(clientToServer.source()));
        serverChannel.setWriteChannel(new IWWrapper(serverToClient.sink()));
        
        String serverOut = "I AM A SERVER\r\n, HELLO\r\n";
        String clientOut = "A CLIENT I AM\r\n, GOODBYE\r\n";
        new WriteBufferChannel(serverOut.getBytes(), serverChannel);
        new WriteBufferChannel(clientOut.getBytes(), clientChannel);
        
        final ByteBuffer clientRead = ByteBuffer.allocate(100);
        final ByteBuffer serverRead = ByteBuffer.allocate(100);
        for(int i = 0; i < 10; i++) {
            NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    try {
                        clientChannel.read(clientRead);
                        serverChannel.read(serverRead);
                        clientChannel.handleWrite();
                        serverChannel.handleWrite();
                    } catch(IOException iox) {
                        throw new RuntimeException(iox);
                    }
                }
            }).get();
            Thread.sleep(100);
        }
        
        assertEquals(serverOut, new String(clientRead.array(), 0, clientRead.position()));
        assertEquals(clientOut, new String(serverRead.array(), 0, serverRead.position()));
        
        serverToClient.source().close();
        serverToClient.sink().close();
        clientToServer.source().close();
        clientToServer.sink().close();
    }
    
    // TODO: Test underflows & overflows
    
    
    // The idea behind this is to connect one SSLSocket to a plain socket,
    // since that's the only way we can read what the built-in SSLSocket
    // is sending, and read all input to the SSLRWChannel as well as writing
    // any output it wants back through the socket.
    // The channel is either in server or client mode, with the blocking socket
    // in the opposite mode.
    private void doTLSTest(final boolean testServer) throws Exception {
        Executor executor = ExecutorsHelper.newProcessingQueue("TLSTest");
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        
        final SSLReadWriteChannel channel = new SSLReadWriteChannel(context, executor);
        
        SSLSocketFactory sslFactory = context.getSocketFactory();
        final SSLSocket sslSocket = (SSLSocket)sslFactory.createSocket();
        sslSocket.setUseClientMode(!testServer);
        sslSocket.setWantClientAuth(false);
        sslSocket.setNeedClientAuth(false);
        sslSocket.setEnabledCipherSuites(new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" } );
        
        ServerSocket server = new ServerSocket();
        server.bind(null);
        sslSocket.connect(server.getLocalSocketAddress());
        final Socket accepted = server.accept();
        
        // Wrap the channel around the accepted socket.
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                channel.initialize(accepted.getRemoteSocketAddress(), new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, testServer, false);
            }
        }).get(); // wait for this to complete.
        channel.setReadChannel(new IRWrapper(accepted.getInputStream()));
        channel.setWriteChannel(new IWWrapper(accepted.getOutputStream()));
       
        final String INCOMING = "THIS IS A TEST\r\n";
        final String OUTGOING = "OUTGOING DATA\r\n";
        
        // This will trigger an outgoing write.
        new WriteBufferChannel(OUTGOING.getBytes(), channel);
        
        final byte[] b = new byte[100];        
        Thread t = new ManagedThread(new Runnable() {
            public void run() {
                try {                    
                    sslSocket.getInputStream().read(b);
                    sslSocket.getOutputStream().write(INCOMING.getBytes());
                } catch(IOException iox) {
                    throw new RuntimeException(iox);
                }
            }
        });
        t.start();
        
        final ByteBuffer channelRead = ByteBuffer.allocate(100);
        // Try short pumps of data until the handshake is complete.
        for(int i = 0; i < 10; i++) {
            NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    try {
                        channel.read(channelRead);
                        channel.handleWrite();
                    } catch(IOException iox) {
                        throw new RuntimeException(iox);
                    }
                }
            }).get();
            Thread.sleep(100);
        }

        t.join(10000);
        
        assertEquals(INCOMING, new String(channelRead.array(), 0, channelRead.position()));
        assertEquals(OUTGOING, new String(b, 0, OUTGOING.length()));
        
        sslSocket.close();
        server.close();
        accepted.close();
    }

    private static class IRWrapper implements InterestReadableByteChannel {
        private final ReadableByteChannel channel;
        private final InputStream in;
        
        IRWrapper(InputStream in) {
            this.in = in;
            this.channel = Channels.newChannel(in);
        }
        
        public IRWrapper(ReadableByteChannel channel) {
            this.channel = channel;
            this.in = null;
        }

        public void interestRead(boolean status) {
        }

        public int read(ByteBuffer dst) throws IOException {
            if(in == null || in.available() > 0) // prevents blocking
                return channel.read(dst);
            else
                return 0;
        }

        public void close() throws IOException {
            channel.close();            
        }

        public boolean isOpen() {
            return channel.isOpen();
        }
    }
    
    private static class IWWrapper implements InterestWritableByteChannel {
        private final WritableByteChannel channel;
        
        public IWWrapper(OutputStream out) {
            this.channel = Channels.newChannel(out);
        }
        
        public IWWrapper(WritableByteChannel channel) {
            this.channel = channel;
        }
        
        public void interestWrite(WriteObserver observer, boolean status) {
        }

        public int write(ByteBuffer src) throws IOException {
            return channel.write(src); // assumes this will never block
        }

        public void close() throws IOException {
            channel.close();
        }

        public boolean isOpen() {
            return channel.isOpen();
        }

        public boolean handleWrite() throws IOException {
            return true;
        }

        public void handleIOException(IOException iox) {
        }

        public void shutdown() {
        }
        
    }
    
}
