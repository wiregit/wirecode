package com.limegroup.gnutella;


import junit.framework.*;
import java.io.*;
import java.net.*;
import java.util.Properties;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

public class ManagedConnectionTest extends com.limegroup.gnutella.util.BaseTestCase {  
    public static final int PORT=6666;


    private static final RouterService ROUTER_SERVICE =
        new RouterService(new ActivityCallbackStub());

    public ManagedConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ManagedConnectionTest.class);
    }    

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static void globalSetUp() throws Exception {
        launchBackend();
    }

    public void setUp() throws Exception {
        if(ROUTER_SERVICE.isStarted()) return;
        sleep(4000);
        //SettingsManager.instance().setPort(6444);
        setStandardSettings();
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
		ConnectionSettings.KEEP_ALIVE.setValue(1);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);

        // we start a router service so that all classes have correct
        // access to the others -- ConnectionManager having a valid
        // HostCatcher in particular
        ROUTER_SERVICE.start();
        RouterService.clearHostCatcher();
		ROUTER_SERVICE.connect();
    }

	public void tearDown() {
		
	}
 
    private static void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }

    
    /**
     * Tests the method for checking whether or not a connection is stable.
     */
    public void testIsStable() throws Exception {
        Connection conn = new ManagedConnection("localhost", Backend.PORT);
        conn.initialize();
        
        assertTrue("should not yet be considered stable", !conn.isStable());

        Thread.sleep(6000);
        assertTrue("connection should be considered stable", conn.isStable());
    }

	/**
	 * Test to make sure that GGEP extensions are correctly returned in pongs.
	 */
    public void testForwardsGGEP() throws Exception {
		ManagedConnection out = 
            new ManagedConnection("localhost", Backend.PORT);
        out.initialize();

        assertTrue("connection is open", out.isOpen());
        assertTrue("connection should support GGEP", out.supportsGGEP());
		// receive initial ping
        //drain(out);
		out.receive();
		out.receive();
		//out.receive();`

        //assertTrue("connection should support GGEP", out.supportsGGEP());
		out.send(new PingRequest((byte)3));
		
		Message m = out.receive();
		assertInstanceof("should be a pong", PingReply.class, m);
		PingReply pr = (PingReply)m;

        assertTrue("pong should have GGEP", pr.hasGGEPExtension());
		assertTrue("should not support unicast", !pr.supportsUnicast());

		assertTrue("incorrect daily uptime!", pr.getDailyUptime() > 0);
		assertEquals("unexpected vendor", "LIME", pr.getVendor());
		assertTrue("pong should have GGEP", pr.hasGGEPExtension());
		out.close();
    }

	/**
	 * Tests the method for checking whether or not the connection is a high-
	 * degree connection that maintains high numbers of intra-Ultrapeer 
	 * connections.
	 */
	public void testIsHighDegreeConnection() throws IOException {
		ManagedConnection mc = 
            new ManagedConnection("localhost", Backend.PORT);
		mc.initialize();
		assertTrue("connection should be high degree", 
				   mc.isHighDegreeConnection());
	}

    public void testHorizonStatistics() {
        ManagedConnection mc= new ManagedConnection("", 0);
        //For testing.  You may need to ensure that HORIZON_UPDATE_TIME is
        //non-final to compile.
        mc.HORIZON_UPDATE_TIME=1*200;   

        PingReply pr1 = PingReply.create(
            GUID.makeGuid(), (byte)3, 6346,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
            1, 10, false, 0, false);

        PingReply pr2= PingReply.create(
            GUID.makeGuid(), (byte)3, 6347,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
            2, 20, false, 0, false);
        PingReply pr3= PingReply.create(
            GUID.makeGuid(), (byte)3, 6346,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)2},
            3, 30, false, 0, false);

        assertEquals("unexpected number of files", 0, mc.getNumFiles());
        assertEquals("unexpected number of hosts", 0, mc.getNumHosts());
        assertEquals("unexted total file size", 0, mc.getTotalFileSize());

        mc.updateHorizonStats(pr1);
        mc.updateHorizonStats(pr1);  //check duplicates
        assertEquals("unexpected number of files", 1, mc.getNumFiles());
        assertEquals("unexpected number of hosts", 1, mc.getNumHosts());
        assertEquals("unexpected total filesize", 10, mc.getTotalFileSize());

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }
            
        mc.refreshHorizonStats();    
        mc.updateHorizonStats(pr1);  //should be ignored for now
        mc.updateHorizonStats(pr2);
        mc.updateHorizonStats(pr3);
        assertEquals("unexpected number of files", 1, mc.getNumFiles());
        assertEquals("unexpected number of hosts", 1, mc.getNumHosts());
        assertEquals("unexpected total filesize", 10, mc.getTotalFileSize());
        mc.refreshHorizonStats();    //should be ignored
        assertEquals("unexpected number of files", 1, mc.getNumFiles());
        assertEquals("unexpected number of hosts", 1, mc.getNumHosts());
        assertEquals("unexpected total filesize", 10, mc.getTotalFileSize());

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }            

        mc.refreshHorizonStats();    //update stats
        assertEquals("unexedted number of files", 1+2+3, mc.getNumFiles());
        assertEquals("unexpected number of hosts", 3, mc.getNumHosts());
        assertEquals("unexpedted total filesize", 10+20+30, mc.getTotalFileSize());

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }       

        mc.refreshHorizonStats();
        assertEquals("unexpected number of files", 0,mc.getNumFiles());
        assertEquals("unexpected number of hosts", 0,mc.getNumHosts());
        assertEquals("unexpected total file size", 0,mc.getTotalFileSize());                
    }
    	
	
	/**
	 * Tests to make sure that LimeWire correctly strips any GGEP extensions
	 * in pongs if it thinks the connection on the other end does not 
	 * support GGEP.
	 */
	public void testStripsGGEP() throws Exception {
        ManagedConnection out = 
			ManagedConnection.createTestConnection("localhost", 
												   Backend.PORT,
												   new NoGGEPProperties(),
												   new EmptyResponder());
		out.initialize();

		// receive initial ping
        //drain(out);
		out.receive();
		out.receive();
        //out.receive();

        //Connection in = acceptor.accept();
		//assertNotNull("connection should not be null", in);
        assertTrue("Connection should support GGEP", out.supportsGGEP());
		out.send(new PingRequest((byte)2));
		
		Message m = out.receive();
		assertInstanceof("should be a pong", PingReply.class, m);
		PingReply pr = (PingReply)m;
        //try {
        pr.getDailyUptime();
        
        assertEquals("Payload wasn't stripped", -1, pr.getDailyUptime());
        //fail("Payload wasn't stripped");
        //} catch (BadPacketException e) { }			

        //try {
        //  pr.getDailyUptime();
        //  fail("GGEP payload wasn't stripped");
        //} catch (BadPacketException e) { }			
        // try {
        assertFalse("should not have contained GGEP block", 
                    pr.supportsUnicast());

		assertTrue("pong should not have GGEP", !pr.hasGGEPExtension());

        out.close();
    }
    
	/**
	 * Tests to make sure that connections are closed correctly from the
	 * client side.
	 */
    public void testClientSideClose() throws Exception {
        ManagedConnection out=null;
        Connection in=null;
        //com.limegroup.gnutella.MiniAcceptor acceptor=null;                
        //When receive() or sendQueued() gets IOException, it calls
        //ConnectionManager.remove().  This in turn calls
        //ManagedConnection.close().  Our stub does this.
        ConnectionManager manager=new ConnectionManagerStub(true);

        //1. Locally closed
        //acceptor=new com.limegroup.gnutella.MiniAcceptor(null, PORT);
		out = new ManagedConnection("localhost", Backend.PORT);
        out.initialize();            

        //in=acceptor.accept(); 
		assertTrue("connection should be open", out.isOpen());
		assertTrue("runner should not be dead", !out.runnerDied());
        out.close();
        sleep(100);
        assertTrue("connection should not be open", !out.isOpen());
        assertTrue("runner should have died", out.runnerDied());

        try { Thread.sleep(1000); } catch (InterruptedException e) { }
        //in.close(); //needed to ensure connect below works
	}

	/**
	 * Tests to make sure that connections are closed correctly from the
	 * server side.
	 */
    public void testServerSideClose() throws Exception {
		MiniAcceptor acceptor = new MiniAcceptor(PORT);
		
		//2. Remote close: discovered on read
		ManagedConnection out = new ManagedConnection("localhost", PORT);
		out.initialize();            
        Connection in = acceptor.accept(); 
        assertTrue("connection should be open", out.isOpen());
        assertTrue("runner should not be dead", !out.runnerDied());


        in.close();
        try {
			out.receive();
			fail("should not have received message");
        } catch (BadPacketException e) {
			fail("should not have received bad packet", e);
        } catch (IOException e) { }            

        sleep(100);
        assertTrue("connection should not be open", !out.isOpen());
        assertTrue("runner should be dead", out.runnerDied());

        //3. Remote close: discovered on write.  Because of TCP's half-close
        //semantics, we need TWO writes to discover this.  (See unit tests
        //for Connection.)
        acceptor = new com.limegroup.gnutella.MiniAcceptor(PORT);
        out = new ManagedConnection("localhost", PORT);
        out.initialize();            
        in = acceptor.accept(); 
        assertTrue("connection should be open", out.isOpen());
        assertTrue("runner should not be dead", !out.runnerDied());
        in.close();
        Message m = new PingRequest((byte)3);
        m.hop();
        out.send(m);        
        out.send(new PingRequest((byte)3));
        sleep(100);

        assertTrue("connection should not be open", !out.isOpen());
        assertTrue("runner should be dead", out.runnerDied());
		sleep(2000);
    }

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private static boolean drain(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                Message m = c.receive(2000);
                ret=true;
            } catch (InterruptedIOException e) {
                return ret;
            } catch (BadPacketException e) {
            }
        }
    }

    class GGEPResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) throws IOException {
            Properties props=new Properties();
            props.put("GGEP", "0.6"); 
            return HandshakeResponse.createResponse(props);
        }
    }

    class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) throws IOException {
            Properties props = new Properties();
            return HandshakeResponse.createResponse(props);
        }
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
}
