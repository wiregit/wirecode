package com.limegroup.gnutella;


import java.io.IOException;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;


/**
 * Tests basic connection properties.  All tests are done once with compression
 * and once without.
 */
public class ManagedConnectionTest extends BaseTestCase {  
    public static final int SERVER_PORT = 6666;


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
        setStandardSettings();
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        ConnectionSettings.NUM_CONNECTIONS.setValue(1);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);
        ConnectionSettings.SEND_QRP.setValue(false);

        launchBackend();
        sleep(4000);
        
        // we start a router service so that all classes have correct
        // access to the others -- ConnectionManager having a valid
        // HostCatcher in particular
        ROUTER_SERVICE.start();
        RouterService.clearHostCatcher();
        RouterService.connect();
    }

    public void setUp() throws Exception {
    }
    
    public static void globalTearDown() throws Exception {
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
        Connection conn = 
            new ManagedConnection("localhost", Backend.BACKEND_PORT);
        conn.initialize();
        
        assertTrue("should not yet be considered stable", !conn.isStable());

        Thread.sleep(6000);
        assertTrue("connection should be considered stable", conn.isStable());
        conn.close();
    }

	/**
	 * Test to make sure that GGEP extensions are correctly returned in pongs
	 * on a compressed connection.
	 */
    public void testForwardsGGEPCompressed() throws Exception {
		ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
		ConnectionSettings.ENCODE_DEFLATE.setValue(true);
		tForwardsGGEP();
    }
    
    /**
     * Tests to make sure that GGEP extensions are correctly returned in pongs
     * on a normal connection.
     */
    public void testForwardGGEPNotCompressed() throws Exception {
		ConnectionSettings.ACCEPT_DEFLATE.setValue(false);
		ConnectionSettings.ENCODE_DEFLATE.setValue(false);
		tForwardsGGEP();
    }
    
    private void tForwardsGGEP() throws Exception {
		ManagedConnection out = 
            new ManagedConnection("localhost", Backend.BACKEND_PORT);
        out.initialize();
        out.buildAndStartQueues();        

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
            new ManagedConnection("localhost", Backend.BACKEND_PORT);
		mc.initialize();
		assertTrue("connection should be high degree", 
				   mc.isHighDegreeConnection());
        mc.close();
	}

    public void testHorizonStatistics() {
        HorizonCounter hc = HorizonCounter.instance();
        ManagedConnection mc= new ManagedConnection("", 1);
        //For testing.  You may need to ensure that HORIZON_UPDATE_TIME is
        //non-final to compile.
        HorizonCounter.HORIZON_UPDATE_TIME = 1*200;   

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

        assertEquals("unexpected number of files", 0, hc.getNumFiles());
        assertEquals("unexpected number of hosts", 0, hc.getNumHosts());
        assertEquals("unexted total file size", 0, hc.getTotalFileSize());

        mc.updateHorizonStats(pr1);
        mc.updateHorizonStats(pr1);  //check duplicates
        assertEquals("unexpected number of files", 1, hc.getNumFiles());
        assertEquals("unexpected number of hosts", 1, hc.getNumHosts());
        assertEquals("unexpected total filesize", 10, hc.getTotalFileSize());

        try { Thread.sleep(HorizonCounter.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }
            
        hc.refresh();
        mc.updateHorizonStats(pr1);  //should be ignored for now
        mc.updateHorizonStats(pr2);
        mc.updateHorizonStats(pr3);
        assertEquals("unexpected number of files", 1, hc.getNumFiles());
        assertEquals("unexpected number of hosts", 1, hc.getNumHosts());
        assertEquals("unexpected total filesize", 10, hc.getTotalFileSize());
        hc.refresh();    //should be ignored
        assertEquals("unexpected number of files", 1, hc.getNumFiles());
        assertEquals("unexpected number of hosts", 1, hc.getNumHosts());
        assertEquals("unexpected total filesize", 10, hc.getTotalFileSize());

        try { Thread.sleep(HorizonCounter.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }            

        hc.refresh();    //update stats
        assertEquals("unexedted number of files", 1+2+3, hc.getNumFiles());
        assertEquals("unexpected number of hosts", 3, hc.getNumHosts());
        assertEquals("unexpedted total filesize", 10+20+30, 
            hc.getTotalFileSize());

        try { Thread.sleep(HorizonCounter.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }       

        hc.refresh();
        assertEquals("unexpected number of files", 0,hc.getNumFiles());
        assertEquals("unexpected number of hosts", 0,hc.getNumHosts());
        assertEquals("unexpected total file size", 0,hc.getTotalFileSize());
        
        mc.close();
    }
    	
	/**
	 * Tests to make sure that LimeWire correctly strips any GGEP extensions
	 * in pongs if it thinks the connection on the other end does not 
	 * support GGEP on an uncompressed connection.
	 */
	public void testStripsGGEPNotCompressed() throws Exception {
		ConnectionSettings.ACCEPT_DEFLATE.setValue(false);
		ConnectionSettings.ENCODE_DEFLATE.setValue(false);
		tStripsGGEP();
    }

	/**
	 * Tests to make sure that LimeWire correctly strips any GGEP extensions
	 * in pongs if it thinks the connection on the other end does not 
	 * support GGEP on a compressed connection.
	 */
	public void testStripsGGEPCompressed() throws Exception {
		ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
		ConnectionSettings.ENCODE_DEFLATE.setValue(true);
		tStripsGGEP();
    }
   	    
	private void tStripsGGEP() throws Exception {
        ManagedConnection out = 
			ManagedConnection.createTestConnection("localhost", 
												   Backend.BACKEND_PORT,
												   new NoGGEPProperties(),
												   new EmptyResponder());
        out.initialize();
        out.buildAndStartQueues();

        assertTrue("connection is open", out.isOpen());
		// receive all initial messages.
		drain(out);
        Thread.sleep(2000);
        drain(out);
		out.send(new PingRequest((byte)3));
		
		Message m = out.receive();
		assertInstanceof("should be a pong", PingReply.class, m);
		PingReply pr = (PingReply)m;

        assertTrue("pong should not have GGEP", !pr.hasGGEPExtension());
		assertTrue("should not have ggep block", !pr.supportsUnicast());
		assertEquals("incorrect daily uptime!", -1, pr.getDailyUptime());
		out.close();
    }
    
	/**
	 * Tests to make sure that connections are closed correctly from the
	 * client side.
	 */
    public void testClientSideClose() throws Exception {
        ManagedConnection out=null;

        //1. Locally closed
        //acceptor=new com.limegroup.gnutella.MiniAcceptor(null, PORT);
		out = new ManagedConnection("localhost", Backend.BACKEND_PORT);
        out.initialize();            
        out.buildAndStartQueues();

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
		MiniAcceptor acceptor = new MiniAcceptor(SERVER_PORT);
		
		//2. Remote close: discovered on read
		ManagedConnection out = new ManagedConnection("localhost", SERVER_PORT);
		out.initialize();            
		out.buildAndStartQueues();
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
        acceptor = new com.limegroup.gnutella.MiniAcceptor(SERVER_PORT);
        out = new ManagedConnection("localhost", SERVER_PORT);
        out.initialize();            
        out.buildAndStartQueues();
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
