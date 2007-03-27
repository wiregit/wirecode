package org.limewire.nio.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import junit.framework.Test;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.WriteBufferChannel;
import org.limewire.util.BaseTestCase;

public class TLSNIOSocketTest extends BaseTestCase {
    
    public TLSNIOSocketTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TLSNIOSocketTest.class);
    }
    
    public void testConnectAndReadWriteBlocking() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket)factory.createServerSocket(9999);
        server.setNeedClientAuth(false);
        server.setWantClientAuth(false);
        server.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});
        
        TLSNIOSocket socket = new TLSNIOSocket("127.0.0.1", 9999);        
        Socket accepted = server.accept();
        
        OutputStream clientOut = socket.getOutputStream();
        clientOut.write("TEST TEST\r\n".getBytes());
        clientOut.write("\r\n".getBytes());        
        byte[] serverB = new byte[1000];
        int serverRead = accepted.getInputStream().read(serverB);
        assertEquals(13, serverRead);
        assertEquals("TEST TEST\r\n\r\n", new String(serverB, 0, 13));
        
        accepted.getOutputStream().write("HELLO THIS IS A TEST!".getBytes());        
        InputStream clientIn = socket.getInputStream();
        byte[] clientB = new byte[2048];
        int clientRead = clientIn.read(clientB);
        assertEquals(21, clientRead);
        assertEquals("HELLO THIS IS A TEST!", new String(clientB, 0, 21));
        
        socket.close();
        accepted.close();
        server.close();
    }
    
    public void testConnectAndReadWriteNonBlocking() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket)factory.createServerSocket(9999);
        server.setNeedClientAuth(false);
        server.setWantClientAuth(false);
        server.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});
        
        TLSNIOSocket socket = new TLSNIOSocket("127.0.0.1", 9999);        
        Socket accepted = server.accept();
        
        WriteBufferChannel clientOut = new WriteBufferChannel();
        socket.setWriteObserver(clientOut);
        NIODispatcher.instance().invokeAndWait(new Runnable() {public void run() {}}); //wait for write to set 
        clientOut.setBuffer(ByteBuffer.wrap("TEST TEST\r\n\r\n".getBytes()));
        byte[] serverB = new byte[1000];
        int serverRead = accepted.getInputStream().read(serverB);
        assertEquals(13, serverRead);
        assertEquals("TEST TEST\r\n\r\n", new String(serverB, 0, 13));
        
        accepted.getOutputStream().write("HELLO THIS IS A TEST!".getBytes());
        
        ReadTester reader = new ReadTester();
        socket.setReadObserver(reader);
        Thread.sleep(500);
        ByteBuffer read = reader.getRead();
        assertEquals(21, read.limit());
        assertEquals("HELLO THIS IS A TEST!", new String(read.array(), 0, 21));
        
        socket.close();
        accepted.close();
        server.close();
    }
    
   private static class ReadTester implements ChannelReadObserver {        
        private InterestReadableByteChannel source;
        private ByteBuffer readData = ByteBuffer.allocate(128 * 1024);
        
        // ChannelReader methods.
        public InterestReadableByteChannel getReadChannel() { return source; }
        public void setReadChannel(InterestReadableByteChannel channel) { source = channel; }
        
        // IOErrorObserver methods.
        public void handleIOException(IOException x) { fail(x); }
        
        // ReadObserver methods.
        public void handleRead() throws IOException {
            source.read(readData);
            assertEquals(0, source.read(readData)); // must have finish on first read.
        }
        
        // Shutdownable methods.
        public void shutdown() {}
        
        public ByteBuffer getRead() { return (ByteBuffer)readData.flip(); }
    }
    
}
