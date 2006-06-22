package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelReader;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.StubConnectObserver;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.UDPServiceStub;
import com.limegroup.gnutella.util.BaseTestCase;

/**
 * Tests that NIOSocket delegates events correctly.
 */
public final class UDPConnectNIOTest extends BaseTestCase {
    
    private static final int PORT_1 = 6346;
    private static final int PORT_2 = 6348;

	public UDPConnectNIOTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UDPConnectNIOTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public static void globalSetUp() throws Exception {
        UDPConnectionProcessor.setUDPServiceForTesting(UDPServiceStub.instance());
        new RouterService(new ActivityCallbackStub());
        Acceptor ac = RouterService.getAcceptor();
        ac.setAddress(InetAddress.getByName("127.0.0.1"));
        ac.setExternalAddress(InetAddress.getByName("127.0.0.1"));
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false); 
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue("127.0.0.1");
    }

    public static void globalTearDown() throws Exception {
        // Cleanup the UDPServiceStub usage
        UDPConnectionProcessor.setUDPServiceForTesting(null);
    }

    public void setUp() throws Exception {
        Acceptor ac = RouterService.getAcceptor();
        ac.setAddress(InetAddress.getByName("127.0.0.1"));
        ac.setExternalAddress(InetAddress.getByName("127.0.0.1"));
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false); 
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue("127.0.0.1");

        // Add some simulated connections to the UDPServiceStub
        UDPServiceStub.stubInstance().addReceiver(PORT_1, PORT_2, 10, 0);
        UDPServiceStub.stubInstance().addReceiver(PORT_2, PORT_1, 10, 0);
    }
    
    public void tearDown() throws Exception {
        // Clear out the receiver parameters for the UDPServiceStub
        UDPServiceStub.stubInstance().clearReceivers();
    }      
    
    
    private StubConnectObserver setupConnection() throws Exception {
        UDPConnection conn  = new UDPConnection();
        StubConnectObserver stub = new StubConnectObserver();
        conn.connect(new InetSocketAddress("127.0.0.1", PORT_2), 5000, stub);
        return stub;
    }
    
    public void testDelayedGetInputStream() throws Exception {
        StubConnectObserver stub = setupConnection();
        
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", PORT_1);
        UDPConnection socket = new UDPConnection();
        socket.setSoTimeout(1000);
        socket.connect(addr);
        
        stub.waitForResponse(5000);
        Socket accepted = stub.getSocket();
        assertNotNull(accepted);

        byte[] rnd = new byte[100];
        new Random().nextBytes(rnd);
        accepted.getOutputStream().write(rnd); // this'll go immediately into the buffer
        
        ICROAdapter icro = new ICROAdapter();
        ByteBuffer read = icro.getReadBuffer();
        assertEquals(0, read.position());
        
        socket.setReadObserver(icro);
        Thread.sleep(500); // let NIODispatcher to its thang.
        
        assertEquals(100, read.position()); // data was transferred to the reader.
        for(int i = 0; i < 100; i++)
            assertEquals(rnd[i], read.get(i));
        
        InputStream stream = socket.getInputStream();
        byte[] readData = new byte[100];
        assertEquals(100, stream.read(readData));
        assertEquals(rnd, readData);
        
        assertEquals(0, read.position()); // moved to the InputStream
        
        new Random().nextBytes(rnd);
        accepted.getOutputStream().write(rnd); // write some more, make sure it goes to stream
        
        Thread.sleep(500);
        assertEquals(0, read.position());
        assertEquals(100, stream.read(readData));
        assertEquals(rnd, readData);
        
        socket.close();
    }
    
    public void testSetReadObserverGoesThroughChains() throws Exception {
        UDPConnection socket = new UDPConnection();
        
        RCROAdapter entry = new RCROAdapter();
        socket.setReadObserver(entry);
        Thread.sleep(1000);
        assertInstanceof(UDPSocketChannel.class, entry.getReadChannel());
        
        RCRAdapter chain1 = new RCRAdapter();
        entry.setReadChannel(chain1);
        socket.setReadObserver(entry);
        Thread.sleep(1000);
        assertInstanceof(UDPSocketChannel.class, chain1.getReadChannel());
        assertSame(chain1, entry.getReadChannel());
        
        RCRAdapter chain2 = new RCRAdapter();
        chain1.setReadChannel(chain2);
        socket.setReadObserver(entry);
        Thread.sleep(1000);
        assertInstanceof(UDPSocketChannel.class, chain2.getReadChannel());        
        assertSame(chain2, chain1.getReadChannel());
        assertSame(chain1, entry.getReadChannel());
    }

    
    public void testBlockingConnect() throws Exception {
        setupConnection();        
        UDPConnection socket = new UDPConnection();
        socket.connect(new InetSocketAddress("127.0.0.1", PORT_1));
        assertTrue(socket.isConnected());
        socket.close();
        Thread.sleep(500);
        assertFalse(socket.isConnected());
    }
    
    public void testBlockingConnectFailing() throws Exception {
        UDPConnection socket = new UDPConnection();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", 9999));
            fail("shouldn't have connected");
        } catch(ConnectException iox) {
            assertFalse(socket.isConnected());
        }
    }
    
    public void testBlockingConnectTimesOut() throws Exception {
        UDPConnection socket = new UDPConnection();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", 9999), 1000);
            fail("shouldn't have connected");
        } catch(SocketTimeoutException iox) {
            assertEquals("operation timed out (1000)", iox.getMessage());
        }
    }
    
    public void testNonBlockingConnect() throws Exception {
        setupConnection();
        UDPConnection socket = new UDPConnection();
        StubConnectObserver observer = new StubConnectObserver();
        socket.connect(new InetSocketAddress("127.0.0.1", PORT_1), 5000, observer);
        observer.waitForResponse(5500);
        assertTrue(socket.isConnected());
        assertSame(socket, observer.getSocket());
        assertFalse(observer.isShutdown());
        assertNull(observer.getIoException());
        socket.close();
        Thread.sleep(500);
        assertFalse(observer.isShutdown()); // doesn't get both connect & shutdown
        assertFalse(socket.isConnected());
    }
    
    public void testNonBlockingConnectFailing() throws Exception {
        UDPConnection socket = new UDPConnection();
        StubConnectObserver observer = new StubConnectObserver(); 
        

        // UDPConnectionProcessor has a default connect timeout of 20 seconds,
        // so we set our timeout at 40 seconds to ensure it doesn't fail because
        // of the timeout.
        socket.connect(new InetSocketAddress("127.0.0.1", PORT_1), 40000, observer);
        observer.waitForResponse(30000);
        
        assertTrue(observer.isShutdown());
        assertNull(observer.getSocket());
        assertNull(observer.getIoException()); // NIOSocket swallows the IOX.
        assertFalse(socket.isConnected());
    }
    
    public void testNonBlockingConnectTimesOut() throws Exception {
        UDPConnection socket = new UDPConnection();
        StubConnectObserver observer = new StubConnectObserver();
        socket.connect(new InetSocketAddress("127.0.0.1", PORT_1), 1000, observer);
        observer.waitForResponse(1500);
        assertTrue(observer.isShutdown());
        assertNull(observer.getSocket());
        assertNull(observer.getIoException()); // NIOSocket swallows the IOX.
        assertFalse(socket.isConnected());
    }
    
    public void testSoTimeoutUsedForNonBlockingRead() throws Exception {
        StubConnectObserver stub = setupConnection();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", PORT_1);
        UDPConnection socket = new UDPConnection();
        socket.setSoTimeout(1000);
        socket.connect(addr);

        stub.waitForResponse(1000);
        Socket accepted = stub.getSocket();
        assertNotNull(accepted);
        accepted.getOutputStream().write(new byte[100]); // this'll go immediately into the buffer
        socket.getInputStream().read(new byte[100]);
        Thread.sleep(2000);
        assertTrue(!socket.isClosed()); // didn't close 'cause we're using stream reading

        accepted.getOutputStream().write(new byte[1]); // give it some data just to make sure it has
        socket.setReadObserver(new ReadTester());
        Thread.sleep(2000);
        assertTrue(socket.isClosed()); // closed because we switched to NB reading w/ timeout set
    }
    
    private static class ReadTester implements ChannelReadObserver {
        
        private InterestReadChannel source;
        private ByteBuffer readData = ByteBuffer.allocate(128 * 1024);
        
        // ChannelReader methods.
        public InterestReadChannel getReadChannel() { return source; }
        public void setReadChannel(InterestReadChannel channel) { source = channel; }
        
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
    
    private static class ICROAdapter implements ChannelReadObserver, InterestReadChannel {
        private InterestReadChannel source;

        private ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        public ByteBuffer getReadBuffer() {
            return buffer;
        }

        public InterestReadChannel getReadChannel() {
            return source;
        }

        public void setReadChannel(InterestReadChannel channel) {
            source = channel;
        }

        public int read(ByteBuffer b) {
            return BufferUtils.transfer(buffer, b);
        }

        public void close() throws IOException {
            source.close();
        }

        public boolean isOpen() {
            return source.isOpen();
        }

        public void interest(boolean status) {
            source.interest(status);
        }

        public void handleRead() throws IOException {
            while (buffer.hasRemaining() && source.read(buffer) != 0);
        }

        public void handleIOException(IOException iox) {
        }

        public void shutdown() {
        }
    }
    
    private static class RCRAdapter implements ChannelReader, InterestReadChannel {
        protected InterestReadChannel source;
        public InterestReadChannel getReadChannel() { return source; }
        public void setReadChannel(InterestReadChannel channel) { source = channel; }
        public int read(ByteBuffer b) throws IOException { return source.read(b); }
        public void close() throws IOException { source.close(); }
        public boolean isOpen() { return source.isOpen(); }
        public void interest(boolean status) { source.interest(status); }
    }
    
    private static class RCROAdapter extends RCRAdapter implements ChannelReadObserver {
        public void handleRead() throws IOException { source.read(ByteBuffer.allocate(1)); }
        public void handleIOException(IOException iox) {}
        public void shutdown() {}
    }
       
}