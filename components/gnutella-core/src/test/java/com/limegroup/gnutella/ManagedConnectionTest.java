package com.limegroup.gnutella;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IOUtils;
import org.limewire.nio.NIOServerSocket;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.service.ErrorService;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.StubHandshakeResponder;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;


/**
 * Tests basic connection properties.  All tests are done once with compression
 * and once without.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public class ManagedConnectionTest extends ServerSideTestCase {
    
    private static final int LISTEN_PORT = 12350;

    public ManagedConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ManagedConnectionTest.class);
    }    

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Integer numUPs() {
        return new Integer(0);
    }

    public static Integer numLeaves() {
        return new Integer(0);
    }
	
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    public static void setUpQRPTables() {}

    /**
     * Tests the method for checking whether or not a connection is stable.
     */
    public void testIsStable() throws Exception {
        Connection conn = createLeafConnection();
        assertTrue("should not yet be considered stable", !conn.isStable());
        Thread.sleep(6000);
        assertTrue("connection should be considered stable", conn.isStable());
        conn.close();
        
        // Keep in mind that Connection is a loopback
        // connection! That means Acceptor creates a
        // second ManagedConnection instance for the
        // incoming connection. Give that instance a
        // bit time to die or the next test will fail
        // frequently.
        Thread.sleep(500);
    }
    
    public void testConnectionStatsRecorded() throws Exception {
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

        Connection in = createLeafConnection();
        drain(in);
        assertEquals(1, cm.getNumConnections());
        
        
        ManagedConnection out = (ManagedConnection)cm.getConnections().get(0);
        
        PingRequest pr=null;
        long start=0;
        long elapsed=0;

        // Record initial msgs.
        int initialNumSent = out.getNumMessagesSent();
        //long initialBytesSent = out.getUncompressedBytesSent();
        long initialBytesRecv = in.getUncompressedBytesReceived();
        
        pr=new PingRequest((byte)3);
        out.send(pr);
        
        start=System.currentTimeMillis();        
        pr=(PingRequest)in.receive();
        elapsed=System.currentTimeMillis()-start;
        assertEquals("unexpected number of sent messages", initialNumSent + 1, out.getNumMessagesSent());
        assertEquals( initialBytesRecv + pr.getTotalLength(), in.getUncompressedBytesReceived() );
        // due to delay in updating, this stat is off always.
        //assertEquals( initialBytesSent + pr.getTotalLength(), out.getUncompressedBytesSent() );
        assertLessThan("Unreasonably long send time", 500, elapsed);
        assertEquals("hopped something other than 0", 0, pr.getHops());
        assertEquals("unexpected ttl", 3, pr.getTTL());
        
        out.close();
        in.close();
    }
    
    public void testForwardsGGEP() throws Exception {
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

        Connection conn = createLeafConnection();
        drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        Connection out = (Connection)cm.getConnections().get(0);
        assertTrue("connection should support GGEP", out.supportsGGEP());

        out.send(PingReply.create(GUID.makeGuid(), (byte)1));
		Message m = getFirstInstanceOfMessageType(conn, PingReply.class);
		assertNotNull(m);
		assertInstanceof("should be a pong", PingReply.class, m);
		
		PingReply pr = (PingReply)m;

        assertTrue("pong should have GGEP", pr.hasGGEPExtension());
		assertTrue("should not support unicast", !pr.supportsUnicast());

		assertTrue("incorrect daily uptime!", pr.getDailyUptime() > 0);
		assertEquals("unexpected vendor", "LIME", pr.getVendor());
		assertTrue("pong should have GGEP", pr.hasGGEPExtension());
		out.close();
		conn.close();
    }

	/**
	 * Tests the method for checking whether or not the connection is a high-
	 * degree connection that maintains high numbers of intra-Ultrapeer 
	 * connections.
	 */
	public void testIsHighDegreeConnection() throws Exception {
        Connection conn = createLeafConnection();
		assertTrue("connection should be high degree",  conn.isHighDegreeConnection());
        conn.close();
	}

	public void testStripsGGEP() throws Exception {
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

	    Connection conn = createConnection(new NoGGEPProperties());
        drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        Connection out = (Connection)cm.getConnections().get(0);
        assertFalse("connection should supportn't GGEP", out.supportsGGEP());

        out.send(PingReply.create(GUID.makeGuid(), (byte)1));
		Message m = getFirstInstanceOfMessageType(conn, PingReply.class);
		assertNotNull(m);
		assertInstanceof("should be a pong", PingReply.class, m);
		
		PingReply pr = (PingReply)m;

        assertTrue("pong should not have GGEP", !pr.hasGGEPExtension());
		assertTrue("should not have ggep block", !pr.supportsUnicast());
		assertEquals("incorrect daily uptime!", -1, pr.getDailyUptime());
		out.close();
		conn.close();
    }


	// Tests to make sure that connections are closed correctly from the
    //	  client side.
    public void testClientSideClose() throws Exception {
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

        Connection out = createLeafConnection();
        drain(out);
        assertEquals(1, cm.getNumConnections());
        Connection in = (Connection)cm.getConnections().get(0);

        //in=acceptor.accept(); 
		assertTrue("connection should be open", out.isOpen());
        out.close();
        Thread.sleep(100);
        assertTrue("connection should not be open", !out.isOpen());
        Thread.sleep(100);
        assertTrue(!in.isOpen());
	}

	 // Tests to make sure that connections are closed correctly from the
	 // server side.
    public void testServerSideClose() throws Exception {
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

        Connection out = createLeafConnection();
        drain(out);
        assertEquals(1, cm.getNumConnections());
        Connection in = (Connection)cm.getConnections().get(0);
        assertTrue("connection should be open", out.isOpen());
        
        out.close();
        Message m = new PingRequest((byte)4);
        m.hop();
        in.send(m);   
        Thread.sleep(500);
        in.send(new PingRequest((byte)4));
        Thread.sleep(500);
        in.send(new PingRequest((byte)4));
        Thread.sleep(500);

        assertTrue("connection should not be open", !in.isOpen());
		Thread.sleep(2000);
    }
    
    public void testHashFiltering() throws Exception {
        URN sha1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        QueryRequest urnFile = QueryRequest.createQuery(sha1,"java");
        
        ManagedConnection mc = new ManagedConnection("", 1);
        // default should be no filtering
        assertFalse(mc.isSpam(urnFile));
        
        // now turn filtering on and rebuild filters
        FilterSettings.FILTER_HASH_QUERIES.setValue(true);
        mc = new ManagedConnection("", 1);
        
        assertTrue(mc.isSpam(urnFile));

        FilterSettings.FILTER_HASH_QUERIES.setValue(false);
    }
    
    public void testNonBlockingHandshakeSucceeds() throws Exception {
        ManagedConnection mc = new ManagedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            mc.initialize(observer);
            observer.waitForResponse(5000);
            assertTrue(observer.isConnect());
            assertFalse(observer.isBadHandshake());
            assertFalse(observer.isNoGOK());
            assertFalse(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }
    }
    
    public void testNonBlockingBadHandshake() throws Exception {
        ManagedConnection mc = new ManagedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            acceptor.getObserver().setBadHandshake(true);
            mc.initialize(observer);
            observer.waitForResponse(5000);
            assertFalse(observer.isConnect());
            assertTrue(observer.isBadHandshake());
            assertFalse(observer.isNoGOK());
            assertFalse(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }       
    }
    
    public void testNonBlockingNGOK() throws Exception {
        ManagedConnection mc = new ManagedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            acceptor.getObserver().setNoGOK(true);
            mc.initialize(observer);
            observer.waitForResponse(5000);
            assertFalse(observer.isConnect());
            assertFalse(observer.isBadHandshake());
            assertTrue(observer.isNoGOK());
            assertEquals(401, observer.getCode());
            assertFalse(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }           
    }
    
    public void testNonBlockingHandshakeTimeout() throws Exception {
        ManagedConnection mc = new ManagedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            acceptor.getObserver().setTimeout(true);
            mc.initialize(observer);
            observer.waitForResponse(10000);
            assertFalse(observer.isConnect());
            assertFalse(observer.isBadHandshake());
            assertFalse(observer.isNoGOK());
            assertTrue(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }            
    }

    class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) {
            Properties props = new Properties();
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }

    /**
     * Handshake properties indicating no support for GGEP.
     */
	private static class NoGGEPProperties extends UltrapeerHeaders {
		public NoGGEPProperties() {
			super("localhost");
			remove(HeaderNames.GGEP);
		}
	}
    
    private static class ConnectionAcceptor {
        private ServerSocket socket;
        private SimpleAcceptObserver observer;
        
        
        public void start() throws Exception {
            observer = new SimpleAcceptObserver();
            socket = new NIOServerSocket(observer);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(LISTEN_PORT));
        }
        
        public void shutdown() throws Exception {
            socket.close();
        }
        
        public SimpleAcceptObserver getObserver() {
            return observer;
        }
    }    
    
    private static class SimpleAcceptObserver implements AcceptObserver {
        private boolean noGOK = false;
        private boolean timeout = false;
        private boolean badHandshake = false;

        public void handleIOException(IOException iox) {
        }

        public void handleAccept(final Socket socket) throws IOException {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        if (badHandshake) {
                            socket.close();
                            return;
                        }
                        
                        if(timeout) {
                            socket.setSoTimeout(60000);
                            socket.getInputStream().read(new byte[2048]);
                            return;
                        }
                        
                        socket.setSoTimeout(3000);
                        InputStream in = socket.getInputStream();
                        String word = IOUtils.readWord(in, 9);
                        if (!word.equals("GNUTELLA"))
                            throw new IOException("Bad word: " + word);

                        if (noGOK) {
                            socket.getOutputStream().write("GNUTELLA/0.6 401 Failed\r\n\r\n".getBytes());
                            socket.getOutputStream().flush();
                            return;
                        }

                        final Connection con = new Connection(socket);
                        con.initialize(null, new StubHandshakeResponder(), 1000);
                    } catch (Exception e) {
                        ErrorService.error(e);
                    }
                }
            }, "conninit");
        }

        public void shutdown() {
        }

        public void setBadHandshake(boolean badHandshake) {
            this.badHandshake = badHandshake;
        }

        public void setNoGOK(boolean noGOK) {
            this.noGOK = noGOK;
        }
        
        public void setTimeout(boolean timeout) {
            this.timeout = timeout;
        }
        
        public void clear() {
            this.badHandshake = false;
            this.noGOK = false;
            this.timeout = false;
        }
    }
}
