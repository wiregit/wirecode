package com.limegroup.gnutella;

import java.io.File;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.EmptyResponder;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Random;

/**
 *  Tests that an Ultrapeer correctly handle Probe queries.  Essentially tests
 *  the following methods of MessageRouter: handleQueryRequestPossibleDuplicate
 *  and handleQueryRequest
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 */
public final class ServerSideLeafGuidedQueriesTest extends BaseTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static final int PORT = 6667;

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 300;

	/**
	 * The default TTL to use for request messages.
	 */
	private final static byte TTL = 7;

	/**
	 * The "soft max" TTL used by LimeWire's message routing -- hops + ttl 
	 * greater than this value have their TTLs automatically reduced
	 */
	private static final byte SOFT_MAX = 4;

	/**
	 * The TTL of the initial "probe" queries that the Ultrapeer uses to
	 * determine how widely distributed a file is.
	 */
	private static final byte PROBE_QUERY_TTL = 2;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static Connection LEAF;

    /**
     * The Ultrapeer connections.
     */
    private static Connection ULTRAPEERS[] = new Connection[29];

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public ServerSideLeafGuidedQueriesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideLeafGuidedQueriesTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private static void buildConnections() {
	    LEAF =
			new Connection("localhost", PORT, 
						   new LeafHeaders("localhost"),
						   new EmptyResponder()
						   );
        
        for (int i = 0; i < ULTRAPEERS.length; i++)
            ULTRAPEERS[i] = new Connection("localhost", PORT,
                                           new UltrapeerHeaders("localhost"),
                                           new EmptyResponder()
                                           );

    }

    public static void setSettings() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
        ConnectionSettings.PORT.setValue(PORT);
        SharingSettings.setDirectories(new File[0]);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(4);
		ConnectionSettings.NUM_CONNECTIONS.setValue(3);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
    }

	public static void globalSetUp() throws Exception {
        setSettings();

        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());

		ROUTER_SERVICE.start();
		ROUTER_SERVICE.clearHostCatcher();
		ROUTER_SERVICE.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());
	}

    
    public void setUp() throws Exception {
        setSettings();
        
        for (int i = 0; i < ULTRAPEERS.length; i++) {
            assertTrue("ULTRAPEER " + i + 
                       " should be connected", ULTRAPEERS[i].isOpen());
            assertTrue("should be up -> up",
                       ULTRAPEERS[i].isSupernodeSupernodeConnection());
        }
		assertTrue("LEAF should be connected", LEAF.isOpen());
		assertTrue("should be up -> leaf", LEAF.isClientSupernodeConnection());
		
		ConnectionManager cm = RouterService.getConnectionManager();
		assertEquals("wrong # ultrapeers",
		    ULTRAPEERS.length, cm.getNumUltrapeerConnections());
        assertEquals("wrong # leaf connections",
            1, cm.getNumInitializedClientConnections());
    }


	public static void globalTearDown() throws Exception {
		ROUTER_SERVICE.disconnect();
		sleep();
		LEAF.close();
        for (int i = 0; i < ULTRAPEERS.length; i++)
            ULTRAPEERS[i].close();
		sleep();
	}

	private static void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}

	/**
	 * Drains all messages 
	 */
 	private static void drainAll() throws Exception {
 	    drainAll(ULTRAPEERS, TIMEOUT);
 	    drain(LEAF, TIMEOUT);
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private static void connect() throws Exception {
		buildConnections();

        // init all UPs        
        for (int i = 0; i < ULTRAPEERS.length; i++)
            ULTRAPEERS[i].initialize();
        
        //3. routed leaf, with route table for "test"
        LEAF.initialize();
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }

        // for Ultrapeers
        for (int i = 0; i < ULTRAPEERS.length; i++) {
            qrt = new QueryRouteTable();
            qrt.add("leehsus");
            qrt.add("berkeley");
            for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
                ULTRAPEERS[i].send((RouteTableMessage)iter.next());
                ULTRAPEERS[i].flush();
            }
        }

    }

    // BEGIN TESTS
    // ------------------------------------------------------

    public void testConfirmSupport() throws Exception {
        Message m = getFirstMessageOfType(LEAF,
                        MessagesSupportedVendorMessage.class);
        assertNotNull(m);
        
        MessagesSupportedVendorMessage msvm =
            (MessagesSupportedVendorMessage)m;

        assertGreaterThan(0, msvm.supportsLeafGuidance());
    }


    public void testLeafIsDone() throws Exception {
        drainAll();

        // we want to make sure that the leaf correctly guides the queries to
        // stop.

        // send a query from the leaf
        QueryRequest query = QueryRequest.createQuery("berkeley");
        LEAF.send(query);
        LEAF.flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length); i++)
            if (nQuery == null)
                nQuery = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);

        assertTrue(nQuery != null);

        // now tell the main UP that you have got enough results
        QueryStatusResponse sResp = 
             new QueryStatusResponse(new GUID(query.getGUID()), 250);
        LEAF.send(sResp);
        LEAF.flush();

        // UPs should not get any more queries
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length); i++) {
            nQuery = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            assertNull(nQuery);
        }
        
    }


    public void testUPRoutedEnough() throws Exception {
        drainAll();

        // we want to make sure that the UP stops the query when enough results
        // have been routed

        // send a query from the leaf
        QueryRequest query = QueryRequest.createQuery("berkeley");
        LEAF.send(query);
        LEAF.flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length); i++) {
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
            // send 10 results from this Ultrapeer....
            for (int j = 0; (j < 10) && (local != null); j++)
                routeResultsToUltrapeer(nQuery.getGUID(), ULTRAPEERS[i]);
        }

        assertTrue(nQuery != null);

        // UPs should get more queries
        Thread.sleep(5000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length); i++) {
            QueryRequest local  = getFirstQueryRequest(ULTRAPEERS[i], 
                                                       TIMEOUT*2);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // now send enough results, we shouldn't get no more queries yo
        routeResultsToUltrapeers(query.getGUID(), 200);
        drainAll(); // do this to make sure no queries were sent while executing

        // we shouldn't get no more queries yo
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length); i++) {
            nQuery = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            assertNull(nQuery);
        }
    }


    public void testUPRoutedMax() throws Exception {
        drainAll();

        // we want to make sure that the UP stops the query when enough results
        // have been routed AND the leaf still wants more

        // send a query from the leaf
        QueryRequest query = QueryRequest.createQuery("berkeley");
        LEAF.send(query);
        LEAF.flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length) && (nQuery == null); i++) {
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
            // send 10 results from this Ultrapeer....
            for (int j = 0; (j < 10) && (local != null); j++)
                routeResultsToUltrapeer(nQuery.getGUID(), ULTRAPEERS[i]);
        }

        assertTrue(nQuery != null);

        // send a QueryStatus, but not one that will make the UP stop
        QueryStatusResponse sResp = 
             new QueryStatusResponse(new GUID(query.getGUID()), 25);
        LEAF.send(sResp);
        LEAF.flush();

        // UPs should get more queries
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length) && (nQuery == null); i++) {
            QueryRequest local  = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // now send enough results, we shouldn't get no more queries yo
        routeResultsToUltrapeers(query.getGUID(), 200);
        drainAll(); // do this to make sure no queries were sent while executing

        // we shouldn't get no more queries yo
        Thread.sleep(4000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length); i++) {
            nQuery = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            assertNull(nQuery);
        }
    }


    public void testLongLivedLeafGuidance() throws Exception {
        drainAll();

        // we want to make sure that the UP stops the query when enough results
        // have been routed AND the leaf still wants more

        // send a query from the leaf
        QueryRequest query = QueryRequest.createQuery("berkeley");
        LEAF.send(query);
        LEAF.flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length) && (nQuery == null); i++) {
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertTrue(nQuery != null);

        // send a QueryStatus, but not one that will make the UP stop
        QueryStatusResponse sResp = 
             new QueryStatusResponse(new GUID(query.getGUID()), 10);
        LEAF.send(sResp);
        LEAF.flush();

        // UPs should get more queries
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length) && (nQuery == null); i++) {
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // send a QueryStatus, but not one that will make the UP stop
        sResp = new QueryStatusResponse(new GUID(query.getGUID()), 30);
        LEAF.send(sResp);
        LEAF.flush();

        // UPs should get more queries
        Thread.sleep(4000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length) && (nQuery == null); i++) {
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // send a QueryStatus that will make UP stop
        sResp = new QueryStatusResponse(new GUID(query.getGUID()), 50);
        LEAF.send(sResp);
        LEAF.flush();
        drainAll(); // do this to make sure no queries were sent while executing

        // we shouldn't get no more queries yo
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEERS.length); i++) {
            nQuery = getFirstQueryRequest(ULTRAPEERS[i], TIMEOUT);
            assertNull(nQuery);
        }
    }


    private void routeResultsToUltrapeers(byte[] guid, int numResults) 
        throws Exception {
        Random rand = new Random();
        for (int i = 0; i < numResults; i++) {
            int index = rand.nextInt(ULTRAPEERS.length);
            routeResultsToUltrapeer(guid, ULTRAPEERS[index]);
        }
    }

    private void routeResultsToUltrapeer(byte[] guid, Connection source) 
        throws Exception {
        byte[] ip = new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
        byte[] clientGUID = GUID.makeGuid();
        Response[] resp = new Response[] {new Response(0, 10, "berkeley")};
        QueryReply reply = new QueryReply(guid, (byte)3, 6346, ip, 0, resp,
                                          clientGUID, false);
        source.send(reply);
        source.flush();
    }

    

}
