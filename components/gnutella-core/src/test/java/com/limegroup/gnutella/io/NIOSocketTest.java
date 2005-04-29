package com.limegroup.gnutella.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;

/**
 * Tests that NIOSocket delegates events correctly.
 */
public final class NIOSocketTest extends BaseTestCase {
    
    private static final int PORT = 9999;

	public NIOSocketTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(NIOSocketTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	// tests to make sure that calling setReadObserver
	// will gobble any data that wasn't gobbled in blocking mode.
	public void testSetReadObserver() throws Exception {
	    byte[] data = new byte[127];
	    for(byte i = 0; i < data.length; i++)
	        data[i] = i;
	    
	    Listener listener = new Listener(PORT);
	    NIOSocket socket = new NIOSocket("127.0.0.1", PORT);
	    Stream stream = listener.getStream();
	    stream.write(data);
	    
	    InputStream in = socket.getInputStream();
	    byte[] readIn = new byte[57];
	    in.read(readIn);
	    for(int i = 0; i < readIn.length; i++)
	        assertEquals(i, readIn[i]);
	        
        Thread.sleep(1000);
	    
	    // Make sure that the NIOInputStream did gobble up the data.
	    // This is checking the internal implementation in NIOSocket, so
	    // should the implementation ever change, the test may need to be updated.
	    // (This portion is only checking that the blocking mode did read it up.)
	    NIOInputStream nioInput = (NIOInputStream)PrivilegedAccessor.getValue(socket, "reader");
	    ByteBuffer buffered = (ByteBuffer)PrivilegedAccessor.getValue(nioInput, "buffer");
	    assertEquals(buffered.toString(), data.length - readIn.length, buffered.position());
	    // End internal implementation test.
	    
	    
	    Tester reader = new Tester();
	    socket.setReadObserver(reader);
	    Thread.sleep(1000); // let the NIO thread pump since setReadObserver is invokedLater
	    ByteBuffer remaining = reader.getRead();
	    assertEquals(remaining.toString(), data.length - readIn.length, remaining.remaining());
	    for(int i = readIn.length; i < data.length; i++)
	        assertEquals(i, remaining.get());
	        
        assertSame(socket.getChannel(), reader.getReadChannel());
    }
    
    public void testSetReadObserverGoesThroughChains() throws Exception {
        NIOSocket socket = new NIOSocket();
        Object channel = socket.getChannel();
        
        RCROAdapter entry = new RCROAdapter();
        socket.setReadObserver(entry);
        Thread.sleep(1000);
        assertSame(channel, entry.getReadChannel());
        
        RCRAdapter chain1 = new RCRAdapter();
        entry.setReadChannel(chain1);
        socket.setReadObserver(entry);
        Thread.sleep(1000);
        assertSame(channel, chain1.getReadChannel());
        assertSame(chain1, entry.getReadChannel());
        
        RCRAdapter chain2 = new RCRAdapter();
        chain1.setReadChannel(chain2);
        socket.setReadObserver(entry);
        Thread.sleep(1000);
        assertSame(channel, chain2.getReadChannel());
        assertSame(chain2, chain1.getReadChannel());
        assertSame(chain1, entry.getReadChannel());
    }
        
        
        
    
    private static class Listener {
        
        private ServerSocket server;
        private Socket accepted;
        
        Listener(int port) throws Exception {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
            server.setSoTimeout(30 * 1000);
            new ManagedThread(new Runnable() {
                public void run() {
                    try {
                        accepted = server.accept();
                    } catch(IOException iox) {
                        fail(iox);
                    }
                    try {
                        server.close();
                    } catch(IOException ignored) {}
                }
            }).start();
        }
        
        Stream getStream() {
            return new Stream(accepted);
        }
    }
    
    private static class Stream {
        private Socket socket;
        
        Stream(Socket socket) {
            this.socket = socket;
        }
        
        void write(final byte[] data) {
            new ManagedThread(new Runnable() {
                public void run() {
                    try {
                        socket.getOutputStream().write(data);
                        socket.getOutputStream().flush();
                    } catch(IOException iox) {
                        fail(iox);
                    }
                }
            }).start();
        }
    }
    
    private static class Tester implements ChannelReadObserver {
        
        private ReadableByteChannel source;
        private ByteBuffer readData = ByteBuffer.allocate(128 * 1024);
        
        // ChannelReader methods.
        public ReadableByteChannel getReadChannel() { return source; }
        public void setReadChannel(ReadableByteChannel channel) { source = channel; }
        
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
    
    private static class RCRAdapter implements ChannelReader, ReadableByteChannel {
        protected ReadableByteChannel source;
        public ReadableByteChannel getReadChannel() { return source; }
        public void setReadChannel(ReadableByteChannel channel) { source = channel; }
        public int read(ByteBuffer b) throws IOException { return source.read(b); }
        public void close() throws IOException { source.close(); }
        public boolean isOpen() { return source.isOpen(); }
    }
    
    private static class RCROAdapter extends RCRAdapter implements ChannelReadObserver {
        public void handleRead() throws IOException { source.read(ByteBuffer.allocate(1)); }
        public void handleIOException(IOException iox) {}
        public void shutdown() {}
    }
       
}