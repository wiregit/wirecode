package com.limegroup.gnutella;

import java.io.File;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.xml.MetaFileManager;

public class MulticastTest extends BaseTestCase {

    private static ActivityCallback CALLBACK;
        
    private static MetaFileManager FMAN;
    
    private static MulticastMessageRouter MESSAGE_ROUTER;
        
    private static RouterService ROUTER_SERVICE;
        
	private static final String MP3_NAME =
        "com/limegroup/gnutella/metadata/mpg2layII_1504h_16k_frame56_24000hz_joint_CRCOrigID3v1&2_test27.mp3";
        

    public MulticastTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MulticastTest.class);
    }

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    private static void setSettings() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        // Set the local host to not be banned so pushes can go through
        String ip = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {ip});
        ConnectionSettings.PORT.setValue(TEST_PORT);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("mp3;");
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
        MESSAGE_ROUTER = new MulticastMessageRouter();
        ROUTER_SERVICE = new RouterService(
            CALLBACK, MESSAGE_ROUTER, FMAN);
    
        setSettings();
                
        ROUTER_SERVICE.start();
		RouterService.clearHostCatcher();
		RouterService.connect();
        
        // MUST SLEEP TO LET THE FILE MANAGER INITIALIZE
        sleep(2000);
    }

    public void setUp() throws Exception {
        setSettings();
        
        MESSAGE_ROUTER.multicasted.clear();
        MESSAGE_ROUTER.unicasted.clear();
        
        assertEquals("unexpected number of shared files", 1,
            FMAN.getNumFiles() );
	}
    
    public static void globalTearDown() throws Exception {
        RouterService.disconnect();
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
    
    /**
     * Tests that a push sent from a multicast RFD is sent via multicast.
     * This does NOT test that ManagedDownloader will actively push
     * multicast requests, nor does it check that we can parse
     * the incoming GIV.  It also does not test to ensure that multicast
     * pushes are given priority over all other uploads.
     */
    public void testPushSentThroughMulticast() throws Exception {
        // first go through some boring stuff to get a correct QueryReply
        // that we can convert to an RFD.
        byte[] guid = RouterService.newQueryGUID();
        RouterService.query(guid, "metadata");
        sleep(300);
        assertEquals("should have sent query", 1,
            MESSAGE_ROUTER.multicasted.size());
        assertEquals("should have gotten reply", 1,
            MESSAGE_ROUTER.unicasted.size());

        Message m = (Message)MESSAGE_ROUTER.unicasted.get(0);
        assertInstanceof( QueryReply.class, m);
        QueryReply qr = (QueryReply)m;
        // Because we're acting as both the sender & receiver, our
        // routing tables are a little confused, so we must reset
        // the push route table to map the guid to ForMeReplyHandler
        // from a UDPReplyHandler
        reroutePush(qr.getClientGUID());
                
        // okay, now we have a QueryReply to convert to an RFD.
        List responses = qr.getResultsAsList();
        assertEquals("should only have 1 response", 1, responses.size());
        Response res = (Response)responses.get(0);
        RemoteFileDesc rfd = res.toRemoteFileDesc(qr.getHostData());
        
        assertTrue("rfd should be multicast", rfd.isReplyToMulticast());
        
        // clear the data to make it easier to look at again...
        MESSAGE_ROUTER.multicasted.clear();
        MESSAGE_ROUTER.unicasted.clear();        
        
        // Finally, we have the RFD we want to push.
        RouterService.getDownloadManager().sendPush(rfd);
        
        
        // sleep to make sure the push goes through.
        sleep(300);
        
        assertEquals("should have sent & received push", 1,
            MESSAGE_ROUTER.multicasted.size());
        // should be a push.
        m = (Message)MESSAGE_ROUTER.multicasted.get(0);
        assertInstanceof(PushRequest.class, m);
        PushRequest pr = (PushRequest)m;
        // note it was hopped.
        assertEquals("wrong ttl", 0, pr.getTTL());
        assertEquals("wrong hops", 1, pr.getHops());
        assertTrue("wrong client guid",
            Arrays.equals(rfd.getClientGUID(), pr.getClientGUID()));
        assertEquals("wrong index", rfd.getIndex(), pr.getIndex());
        
        assertEquals("should not have unicasted anything", 0,
            MESSAGE_ROUTER.unicasted.size());
	}
    
    /**
     * Tests to ensure multicast requests are sent via push
     * and will upload regardless of the slots left.
     */
    public void testPushesHaveUploadPriority() throws Exception {
        // Make it so a normal upload request would fail.
        UploadSettings.UPLOADS_PER_PERSON.setValue(0);        
    
        // first go through some boring stuff to get a correct QueryReply
        // that we can convert to an RFD.
        byte[] guid = RouterService.newQueryGUID();
        RouterService.query(guid, "metadata");
        sleep(300);
        assertEquals("should have sent query", 1,
            MESSAGE_ROUTER.multicasted.size());
        assertEquals("should have gotten reply", 1,
            MESSAGE_ROUTER.unicasted.size());

        Message m = (Message)MESSAGE_ROUTER.unicasted.get(0);
        assertInstanceof( QueryReply.class, m);
        QueryReply qr = (QueryReply)m;
        // Because we're acting as both the sender & receiver, our
        // routing tables are a little confused, so we must reset
        // the push route table to map the guid to ForMeReplyHandler
        // from a UDPReplyHandler
        reroutePush(qr.getClientGUID());
        
        // okay, now we have a QueryReply to convert to an RFD.
        List responses = qr.getResultsAsList();
        assertEquals("should only have 1 response", 1, responses.size());
        Response res = (Response)responses.get(0);
        RemoteFileDesc rfd = res.toRemoteFileDesc(qr.getHostData());
        
        // clear the data to make it easier to look at again...
        MESSAGE_ROUTER.multicasted.clear();
        MESSAGE_ROUTER.unicasted.clear();
        
        assertFalse("file should not be saved yet", 
            new File( _savedDir, "metadata.mp3").exists());
        assertTrue("file should be shared",
            new File(_sharedDir, "metadata.mp3").exists());
        
        RouterService.download(new RemoteFileDesc[] { rfd }, false, 
                               new GUID(guid));
        
        // sleep to make sure the download starts & push goes through.
        sleep(10000);
        
        assertEquals("should have sent & received push", 1,
            MESSAGE_ROUTER.multicasted.size());
        // should be a push.
        m = (Message)MESSAGE_ROUTER.multicasted.get(0);
        assertInstanceof(PushRequest.class, m);
        
        assertEquals("should not have unicasted anything", 0,
            MESSAGE_ROUTER.unicasted.size());
        
        assertTrue("file should have been downloaded & saved",
            new File(_savedDir, "metadata.mp3").exists());
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
    
    private static void reroutePush(byte[] guid) throws Exception {
        RouteTable rt = (RouteTable)PrivilegedAccessor.getValue(
            MESSAGE_ROUTER, "_pushRouteTable");
        rt.routeReply(guid, ForMeReplyHandler.instance());
    }
    
    private static class MulticastMessageRouter extends StandardMessageRouter {

        MulticastMessageRouter() {
            super();
        }

        List multicasted = new LinkedList();
        List unicasted = new LinkedList();
    
        public void handleMulticastMessage(Message msg, InetSocketAddress addr) {
            multicasted.add(msg);
            // change the guid so we can pretend we've never seen it before
            if( msg instanceof QueryRequest )
                setGUID( msg, new GUID(RouterService.newQueryGUID()) );
            super.handleMulticastMessage(msg, addr);
        }
        
        public void handleUDPMessage(Message msg, InetSocketAddress addr) {
            unicasted.add(msg);
            super.handleUDPMessage(msg, addr);
        }
	}
        
}
