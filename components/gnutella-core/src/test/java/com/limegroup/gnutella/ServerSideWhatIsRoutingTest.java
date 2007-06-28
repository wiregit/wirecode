package com.limegroup.gnutella;

import java.io.File;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Iterator;

import junit.framework.Test;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 *  Tests that an Ultrapeer correctly handles connect back redirect messages.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test only covers Ultrapeer behavior - leaves don't participate in
 *  server side connect back stuff.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public final class ServerSideWhatIsRoutingTest extends LimeTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static final int PORT = 6667;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static Connection LEAF;

    /**
     * Ultrapeer connection.
     */
    private static Connection ULTRAPEER_1;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    /**
     * Just a TCP connection to use for testing.
     */
    private static ServerSocket TCP_ACCESS;

    /**
     * The port for TCP_ACCESS
     */ 
    private static final int TCP_ACCESS_PORT = 10776;

    /**
	 * Second Ultrapeer connection
     */
    private static Connection ULTRAPEER_2;

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public ServerSideWhatIsRoutingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideWhatIsRoutingTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private static void buildConnections() throws Exception {
        ULTRAPEER_1 = new Connection("localhost", PORT);
        UDP_ACCESS = new DatagramSocket();
        TCP_ACCESS = new ServerSocket(TCP_ACCESS_PORT);
        ULTRAPEER_2 = new Connection("localhost", PORT);
    }

    public static void setSettings() {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
        ConnectionSettings.PORT.setValue(PORT);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir        
        FileUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        FileUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(4);
		ConnectionSettings.NUM_CONNECTIONS.setValue(3);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
    }

	public static void globalSetUp() throws Exception {
        setSettings();

        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());

		ROUTER_SERVICE.start();
		RouterService.clearHostCatcher();
        RouterService.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());
	}

    
    public void setUp() {
        setSettings();
    }


	public static void globalTearDown() throws Exception {
        RouterService.disconnect();
		sleep();
        if ((LEAF != null) && LEAF.isOpen())
            LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
		sleep();
        UDP_ACCESS.close();
        TCP_ACCESS.close();
	}

	private static void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}

	/**
	 * Drains all messages 
	 */
 	private static void drainAll() throws Exception {
 		if(ULTRAPEER_1.isOpen()) {
 			drain(ULTRAPEER_1);
 		}
 		if(ULTRAPEER_2.isOpen()) {
 			drain(ULTRAPEER_2);
 		}
 		if((LEAF != null) && LEAF.isOpen()) {
 			drain(LEAF);
 		}
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private static void connect() throws Exception {
		buildConnections();
        //1. first Ultrapeer connection 
        ULTRAPEER_2.initialize(new UltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize(new UltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        
        // for Ultrapeer 1
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER_1.send((RouteTableMessage)iter.next());
			ULTRAPEER_1.flush();
        }

		assertTrue("ULTRAPEER_2 should be connected", ULTRAPEER_2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", ULTRAPEER_1.isOpen());

	    LEAF = new Connection("localhost", PORT);
        //3. routed leaf, with route table for "test"
        LEAF.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
        qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }
		assertTrue("LEAF should be connected", LEAF.isOpen());

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		//sleep();
		drainAll();
		sleep();
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    public void testDoesNotRouteToLeafWhatIsNewQuery() throws Exception {
        drainAll();

        // send the query
        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false, 
                             FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_1.send(whatIsNewQuery);
        ULTRAPEER_1.flush();

        // give time to process
        Thread.sleep(1000);

        // the Leaf should NOT get this query
        QueryRequest rQuery = (QueryRequest) getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNull(rQuery);
    }

    public void testDoesRouteToLeafWhatIsNewQuery() throws Exception {
        drainAll();

        // send the CapabilitiesVM
        LEAF.send(CapabilitiesVM.instance());
        LEAF.flush();

        // send the query
        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false, 
                             FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_1.send(whatIsNewQuery);
        ULTRAPEER_1.flush();

        // give time to process
        Thread.sleep(1000);

        // the Leaf should get this query
        QueryRequest rQuery = (QueryRequest) getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));

        // send the LAST HOP query
        whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)1, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false,
                             FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_2.send(whatIsNewQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(1000);

        // the Leaf should get this query
        rQuery = (QueryRequest) getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));


    }


    public void testDoesNotRouteToUltrapeerWhatIsNewQuery() throws Exception {
        drainAll();

        // send the query
        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false,
                             FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_2.send(whatIsNewQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(3000);

        // the UP should NOT get this query
        QueryRequest rQuery = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNull(rQuery);
    }

    public void testDoesRouteToUltrapeerWhatIsNewQuery() throws Exception {
        drainAll();

        // send the CapabilitiesVM
        ULTRAPEER_1.send(CapabilitiesVM.instance());
        ULTRAPEER_1.flush();

        // send the query
        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false,
                             FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_2.send(whatIsNewQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(1000);

        // the UP should get this query
        QueryRequest rQuery = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));
    }

    // Ultrapeer 1 should get the query, Ultrapeer 2 should not (because UP 1
    // sent the capabilites VM)
    public void testLeafQueryRoutesCorrectly() throws Exception {
        drainAll();

        // send the query
        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false,
                             FeatureSearchData.WHAT_IS_NEW);
        LEAF.send(whatIsNewQuery);
        LEAF.flush();

        // give time to process
        Thread.sleep(5000);

        // UP 1 should get this query
        QueryRequest rQuery = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));
        // UP 2 should NOT get this query
        rQuery = (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER_2,
                                                              QueryRequest.class);
        assertNull(rQuery);
    }


    public void testUnsupportedQueryForwardedCorrectly() throws Exception {
        drainAll();

        // send the query
        QueryRequest unknownFeatureQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)3, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false,
                             FeatureSearchData.FEATURE_SEARCH_MAX_SELECTOR+1);
        ULTRAPEER_2.send(unknownFeatureQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(4000);

        // the Leaf should NOT get this query
        QueryRequest rQuery = (QueryRequest) getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNull(rQuery);

        // Ultrapeer 1 should get it though
        rQuery = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(unknownFeatureQuery.getGUID()));
    }


    public void testLastHopUnsupportedQueryForwardedCorrectly() 
        throws Exception {
        drainAll();

        // send the query
        QueryRequest unknownFeatureQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, false, Network.UNKNOWN, false,
                             FeatureSearchData.FEATURE_SEARCH_MAX_SELECTOR+1);
        ULTRAPEER_2.send(unknownFeatureQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(4000);

        // the Leaf should NOT get this query
        QueryRequest rQuery = (QueryRequest) getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNull(rQuery);

        // Ultrapeer 1 should get it though
        rQuery = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(unknownFeatureQuery.getGUID()));
    }

    // ------------------------------------------------------

}
