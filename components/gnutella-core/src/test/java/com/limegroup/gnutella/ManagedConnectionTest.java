package com.limegroup.gnutella;


import java.io.IOException;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.UltrapeerHandshakeResponder;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;


/**
 * Tests basic connection properties.  All tests are done once with compression
 * and once without.
 */
public class ManagedConnectionTest extends ServerSideTestCase {

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
    

/*
    private void tSendFlush() 
		throws IOException, BadPacketException {
        PingRequest pr=null;
        long start=0;
        long elapsed=0;

        assertEquals("unexpected # sent messages", 0, out.getNumMessagesSent()); 
        assertEquals("unexpected # sent bytes", 0, out.getBytesSent());
        pr=new PingRequest((byte)3);
        out.send(pr);
        start=System.currentTimeMillis();        
        pr=(PingRequest)in.receive();
        elapsed=System.currentTimeMillis()-start;
        assertEquals("unexpected number of sent messages", 1, out.getNumMessagesSent());
        assertEquals( pr.getTotalLength(), in.getUncompressedBytesReceived() );
        assertEquals( pr.getTotalLength(), out.getUncompressedBytesSent() );
        assertLessThan("Unreasonably long send time", 500, elapsed);
        assertEquals("hopped something other than 0", 0, pr.getHops());
        assertEquals("unexpected ttl", 3, pr.getTTL());
    } */
    
    /**
     * Tests the method for checking whether or not a connection is stable.
     */
    public void testIsStable() throws Exception {
        Connection conn = createLeafConnection();
        assertTrue("should not yet be considered stable", !conn.isStable());
        Thread.sleep(6000);
        assertTrue("connection should be considered stable", conn.isStable());
        conn.close();
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

    public void testHorizonStatistics() throws Exception {
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

        Thread.sleep(HorizonCounter.HORIZON_UPDATE_TIME*2);
            
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

        Thread.sleep(HorizonCounter.HORIZON_UPDATE_TIME*2);

        hc.refresh();    //update stats
        assertEquals("unexedted number of files", 1+2+3, hc.getNumFiles());
        assertEquals("unexpected number of hosts", 3, hc.getNumHosts());
        assertEquals("unexpedted total filesize", 10+20+30, 
            hc.getTotalFileSize());

        Thread.sleep(HorizonCounter.HORIZON_UPDATE_TIME*2);

        hc.refresh();
        assertEquals("unexpected number of files", 0,hc.getNumFiles());
        assertEquals("unexpected number of hosts", 0,hc.getNumHosts());
        assertEquals("unexpected total file size", 0,hc.getTotalFileSize());
        
        mc.close();
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

    class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) throws IOException {
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
}
