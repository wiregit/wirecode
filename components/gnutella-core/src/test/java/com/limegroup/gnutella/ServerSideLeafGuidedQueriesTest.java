package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.routing.*;

import junit.framework.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;

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
    private static Connection ULTRAPEERS[] = new Connection[30];

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
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client to listen on port 6667, with
        //incoming slots and no connections.
        //To keep LimeWire from connecting to the outside network, we filter out
        //all addresses but localhost and 18.239.0.*.  The latter is used in
        //pongs for testing.  TODO: it would be nice to have a way to prevent
        //BootstrapServerManager from adding defaults and connecting.
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*", "18.239.0.*"});
        ConnectionSettings.PORT.setValue(PORT);
        SharingSettings.setDirectories(new File[0]);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(1);
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
        for (int i = 0; i < ULTRAPEERS.length; i++) {
            if (ULTRAPEERS[i].isOpen())
                drain(ULTRAPEERS[i]);
        }
 		if(LEAF.isOpen())
 			drain(LEAF);
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
        
        for (int i = 0; i < ULTRAPEERS.length; i++)
            assertTrue("ULTRAPEER " + i + 
                       " should be connected", ULTRAPEERS[i].isOpen());
		assertTrue("LEAF should be connected", LEAF.isOpen());

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
    }

    /** 
	 * Tries to receive any outstanding messages on c 
	 *
     * @return <tt>true</tt> if this got a message, otherwise <tt>false</tt>
	 */
    private static boolean drain(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                ret=true;
                //System.out.println("Draining "+m+" from "+c);
            } catch (InterruptedIOException e) {
				// we read a null message or received another 
				// InterruptedIOException, which means a messages was not 
				// received
                return ret;
            } catch (BadPacketException e) {
            }
        }
    }

    /** @return <tt>true<tt> if no messages (besides expected ones, such as 
     *  QRP stuff) were recieved.
     */
    private static boolean noUnexpectedMessages(Connection c) {
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                if (m instanceof RouteTableMessage)
                    ;
                if (m instanceof PingRequest)
                    ;
                else // we should never get any other sort of message...
                    return false;
            }
            catch (InterruptedIOException ie) {
                return true;
            }
            catch (BadPacketException e) {
                // ignore....
            }
            catch (IOException ioe) {
                // ignore....
            }
        }
    }


    /** @return The first QueyrRequest received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private static QueryRequest getFirstQueryRequest(Connection c) {
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                if (m instanceof RouteTableMessage)
                    ;
                if (m instanceof PingRequest)
                    ;
                else if (m instanceof QueryRequest) 
                    return (QueryRequest)m;
                else
                    return null;  // this is usually an error....
            }
            catch (InterruptedIOException ie) {
                return null;
            }
            catch (BadPacketException e) {
                // ignore....
            }
            catch (IOException ioe) {
                // ignore....
            }
        }
    }


    /** @return The first QueyrReply received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private static QueryReply getFirstQueryReply(Connection c) {
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                if (m instanceof RouteTableMessage)
                    ;
                if (m instanceof PingRequest)
                    ;
                else if (m instanceof QueryReply) 
                    return (QueryReply)m;
                else
                    return null;  // this is usually an error....
            }
            catch (InterruptedIOException ie) {
                return null;
            }
            catch (BadPacketException e) {
                // ignore....
            }
            catch (IOException ioe) {
                // ignore....
            }
        }
    }


	/**
	 * Asserts that the given message is a query, printing out the 
	 * message and failing if it's not.
	 *
	 * @param m the <tt>Message</tt> to check
	 */
	private static void assertQuery(Message m) {
		if(m instanceof QueryRequest) return;

		assertInstanceof("message not a QueryRequest: " + m,
		    QueryRequest.class, m);
	}


    // BEGIN TESTS
    // ------------------------------------------------------

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
                nQuery = getFirstQueryRequest(ULTRAPEERS[i]);

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
            nQuery = getFirstQueryRequest(ULTRAPEERS[i]);
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
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i]);
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
            QueryRequest local  = getFirstQueryRequest(ULTRAPEERS[i]);
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
            nQuery = getFirstQueryRequest(ULTRAPEERS[i]);
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
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i]);
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
            QueryRequest local  = getFirstQueryRequest(ULTRAPEERS[i]);
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
            nQuery = getFirstQueryRequest(ULTRAPEERS[i]);
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
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i]);
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
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i]);
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
            QueryRequest local = getFirstQueryRequest(ULTRAPEERS[i]);
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
            nQuery = getFirstQueryRequest(ULTRAPEERS[i]);
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
