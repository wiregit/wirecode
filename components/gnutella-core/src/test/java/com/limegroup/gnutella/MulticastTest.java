package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import junit.framework.*;
import java.io.*;
import java.net.*;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.search.*;

import com.sun.java.util.collections.*;

public class MulticastTest extends BaseTestCase {

    private static ActivityCallback CALLBACK;
        
    private static MetaFileManager FMAN;
    
    private static MulticastMessageRouter MESSAGE_ROUTER;
        
    private static RouterService ROUTER_SERVICE;
        
	private static final String MP3_NAME =
        "com/limegroup/gnutella/mp3/mpg2layII_1504h_16k_frame56_24000hz_joint_CRCOrigID3v1&2_test27.mp3";
        
	private static final Map guidMap = new HashMap();

    public MulticastTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MulticastTest.class);
    }

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    private static void setSettings() {
        SettingsManager settings=SettingsManager.instance();
        settings.setBannedIps(new String[] {"*.*.*.*"});
        settings.setAllowedIps(new String[] {"127.*.*.*"});
        settings.setPort(PORT);
        settings.setExtensions("mp3;");
        File mp3 = CommonUtils.getResourceFile(MP3_NAME);
        assertTrue(mp3.exists());
        CommonUtils.copy(mp3, new File(_sharedDir, "metadata.mp3"));
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(1);
		ConnectionSettings.NUM_CONNECTIONS.setValue(3);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);

        ConnectionSettings.MULTICAST_PORT.setValue(9021);
        ConnectionSettings.ALLOW_MULTICAST_LOOPBACK.setValue(true);
	}

    public static void globalSetUp() throws Exception {
        CALLBACK = new ActivityCallbackStub();
        FMAN = new MetaFileManager();
        MESSAGE_ROUTER = new MulticastMessageRouter(CALLBACK, FMAN);
        ROUTER_SERVICE = new RouterService(
            CALLBACK, MESSAGE_ROUTER, FMAN);
    
        setSettings();
                
        ROUTER_SERVICE.start();
		ROUTER_SERVICE.clearHostCatcher();
		ROUTER_SERVICE.connect();
        
        // MUST SLEEP TO LET THE FILE MANAGER INITIALIZE
        sleep(2000);
    }

    public void setUp() throws Exception {
        setSettings();
        
        MESSAGE_ROUTER.multicasted.clear();
        MESSAGE_ROUTER.unicasted.clear();
        
        assertEquals("unexpected number of shared files", 1,
            FMAN.getNumFiles() );    }
    
    public static void globalTearDown() throws Exception {
        ROUTER_SERVICE.disconnect();
	}
    
    private static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException e) {}
	}
    
    /**
     * Tests that a multicast message is sent by utilizing the 'loopback'
     * feature of multicast.  Notably, we receive the message even if we
     * sent it.
     */
    public void testQueryIsSentAndReceived() {
        byte[] guid = RouterService.newQueryGUID();
        RouterService.query(guid, "sam");
        
        // sleep to let the search go.
        sleep(300);
        
        assertEquals( "unexpected number of multicast messages", 1, 
            MESSAGE_ROUTER.multicasted.size() );

        Message m = (Message)MESSAGE_ROUTER.multicasted.get(0);
        assertInstanceof( QueryRequest.class, m );
        QueryRequest qr = (QueryRequest)m;
        assertEquals("unexpected query", "sam", qr.getQuery() );
        assertTrue( "should be multicast", qr.isMulticast() );
        assertTrue( "should not be firewalled", !qr.isFirewalledSource() );
        
        // note it was hopped once.
        assertEquals("wrong ttl", 0, qr.getTTL());
        assertEquals("wrong hops", 1, qr.getHops());
    }
    
    /**
     * Tests that replies to multicast queries contain the MCAST GGEP header
     * and other appropriate info.
     */
    public void testQueryReplies() throws Exception {
        byte[] guid = RouterService.newQueryGUID();
        RouterService.query(guid, "metadata"); // search for the name
        
        // sleep to let the search process.
        sleep(300);
        
        assertEquals( "unexpected number of multicast messages", 1, 
            MESSAGE_ROUTER.multicasted.size() );
            
        assertEquals( "unexpected number of replies", 1,
            MESSAGE_ROUTER.unicasted.size() );

        Message m = (Message)MESSAGE_ROUTER.unicasted.get(0);
        assertInstanceof( QueryReply.class, m);
        QueryReply qr = (QueryReply)m;
        assertTrue("should have MCAST header", qr.isReplyToMulticastQuery());
        assertTrue("should not need push", !qr.getNeedsPush());
        assertTrue("should not be busy", !qr.getIsBusy());
        assertEquals("should only have 1 result", 1, qr.getResultCount());
        assertTrue("should be measured speed", qr.getIsMeasuredSpeed());
        
        byte[] xml = qr.getXMLBytes();
        assertNotNull("didn't have xml", xml);
        assertGreaterThan("xml too small", 10, xml.length);
        
        // remember it was hopped once
        assertEquals("wrong ttl", 0, qr.getTTL() );
        assertEquals("wrong hops", 1, qr.getHops() );
        
        // wipe out the address so the first addr == my addr check isn't used
        wipeAddress(qr);
        assertEquals("wrong qos", 4, qr.calculateQualityOfService(false));
        assertEquals("wrong qos", 4, qr.calculateQualityOfService(true));
	}
    
    private static void setGUID(Message m, GUID g) {
        try {
            PrivilegedAccessor.invokeMethod( m, "setGUID", g );
        } catch(Exception e) {
            ErrorService.error(e);
        }
    }
    
    private static void wipeAddress(QueryReply qr) throws Exception {
        PrivilegedAccessor.setValue(qr, "_address", new byte[4]);
	}
    
    private static class MulticastMessageRouter extends StandardMessageRouter {

        MulticastMessageRouter(ActivityCallback ac, FileManager fm) {
            super(ac, fm);
        }

        List multicasted = new LinkedList();
        List unicasted = new LinkedList();
    
        public void handleMulticastMessage(Message msg, DatagramPacket dp) {
            multicasted.add(msg);
            // change the guid so we can pretend we've never seen it before
            setGUID( msg, new GUID(RouterService.newQueryGUID()) );
            super.handleMulticastMessage(msg, dp);
        }
        
        public void handleUDPMessage(Message msg, DatagramPacket dp) {
            unicasted.add(msg);
            super.handleUDPMessage(msg, dp);
        }
	}
}
