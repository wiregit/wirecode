package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
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
public final class ServerSideDynamicQueryTest extends BaseTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static final int PORT = 6667;

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 2000;

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
     * Ultrapeer connection.
     */
    private static Connection ULTRAPEER_1;

    /**
	 * Second Ultrapeer connection
     */
    private static Connection ULTRAPEER_2;

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public ServerSideDynamicQueryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideDynamicQueryTest.class);
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
        
        ULTRAPEER_1 = 
			new Connection("localhost", PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder()
						   );

        ULTRAPEER_2 = 
			new Connection("localhost", PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder()
						   );
    }

    public static void setSettings() {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client to listen on port 6667, with
        //incoming slots and no connections.
        SettingsManager settings=SettingsManager.instance();
        //To keep LimeWire from connecting to the outside network, we filter out
        //all addresses but localhost and 18.239.0.*.  The latter is used in
        //pongs for testing.  TODO: it would be nice to have a way to prevent
        //BootstrapServerManager from adding defaults and connecting.
        settings.setBannedIps(new String[] {"*.*.*.*"});
        settings.setAllowedIps(new String[] {"127.*.*.*", "18.239.0.*"});
        settings.setPort(PORT);
        settings.setDirectories(new File[0]);
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
					 SettingsManager.instance().getPort());

		ROUTER_SERVICE.start();
		ROUTER_SERVICE.clearHostCatcher();
		ROUTER_SERVICE.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 SettingsManager.instance().getPort());
	}

    
    public void setUp() {
        setSettings();
    }


	public static void globalTearDown() throws Exception {
		ROUTER_SERVICE.disconnect();
		sleep();
		LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
		sleep();
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
 		if(LEAF.isOpen()) {
 			drain(LEAF);
 		}
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private static void connect() throws Exception {
		buildConnections();
        //1. first Ultrapeer connection 
        ULTRAPEER_2.initialize();

        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize();
        
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

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER_1.send((RouteTableMessage)iter.next());
			ULTRAPEER_1.flush();
        }

		assertTrue("ULTRAPEER_2 should be connected", ULTRAPEER_2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", ULTRAPEER_1.isOpen());
		assertTrue("LEAF should be connected", LEAF.isOpen());

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		//sleep();
		drainAll();
		sleep();
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

    public void testBasicProbeMechanicsFromUltrapeer() throws Exception {
        drainAll();

        QueryRequest request = QueryRequest.createQuery("berkeley");
        request.setTTL((byte)1);

        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF.receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(noUnexpectedMessages(ULTRAPEER_1));

        // make sure probes are routed back correctly....
		Response response1=new Response(0L, 0L, "berkeley rocks");
		byte[] guid1=GUID.makeGuid();
		QueryReply reply1=new QueryReply(request.getGUID(),
										 (byte)2,
										 6346,
										 new byte[4],
										 56,
										 new Response[] {response1},
										 guid1, false);
        drain(ULTRAPEER_2);
		LEAF.send(reply1);
		LEAF.flush();
		QueryReply qRep = getFirstQueryReply(ULTRAPEER_2);
        assertNotNull(qRep);
        assertEquals(new GUID(guid1), new GUID(qRep.getClientGUID()));

        Thread.sleep(2*1000);

        // extend the probe....
        request.setTTL((byte)2);
        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        try {
            LEAF.receive(TIMEOUT);
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {}

        reqRecvd = getFirstQueryRequest(ULTRAPEER_1);
        assertNotNull(reqRecvd);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
        assertEquals((byte)1, reqRecvd.getHops());
    }


    public void testBasicProbeMechanicsFromLeaf() throws Exception {
        drainAll();

        QueryRequest request = QueryRequest.createQuery("berkeley");
        request.hop();
        request.setTTL((byte)1);
        assertEquals(1, request.getHops());

        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF.receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(noUnexpectedMessages(ULTRAPEER_1));

        // make sure probes are routed back correctly....
		Response response1=new Response(0L, 0L, "berkeley rocks");
		byte[] guid1=GUID.makeGuid();
		QueryReply reply1=new QueryReply(request.getGUID(),
										 (byte)2,
										 6346,
										 new byte[4],
										 56,
										 new Response[] {response1},
										 guid1, false);
        drain(ULTRAPEER_2);
		LEAF.send(reply1);
		LEAF.flush();
		QueryReply qRep = getFirstQueryReply(ULTRAPEER_2);
        assertNotNull(qRep);
        assertEquals(new GUID(guid1), new GUID(qRep.getClientGUID()));

        Thread.sleep(2*1000);

        // extend the probe....
        request.setTTL((byte)2);
        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        try {
            LEAF.receive(TIMEOUT);
            fail("expected InterruptedIOException");
        }
        catch (InterruptedIOException expected) {}

        reqRecvd = getFirstQueryRequest(ULTRAPEER_1);
        assertNotNull(reqRecvd);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
        assertEquals((byte)2, reqRecvd.getHops());
    }


    public void testDuplicateProbes() throws Exception {
        drainAll();

        QueryRequest request = QueryRequest.createQuery("berkeley");
        request.setTTL((byte)1);

        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF.receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(noUnexpectedMessages(ULTRAPEER_1));

        Thread.sleep(2*1000);

        // test that the duplicate probe doesn't go anywhere it isn't supposed
        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        // should NOT be forwarded to leaf again....
        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        try {
            reqRecvd = (QueryRequest) LEAF.receive(TIMEOUT);
        }
        catch (InterruptedIOException expected) {}

        // should NOT be forwarded to other Ultrapeer....
        assertTrue(noUnexpectedMessages(ULTRAPEER_1));
    }
    
    // makes sure a probe can't be extended twice....
    public void testProbeIsLimited() throws Exception {
        drainAll();

        QueryRequest request = QueryRequest.createQuery("berkeley");
        request.setTTL((byte)1);

        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF.receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(noUnexpectedMessages(ULTRAPEER_1));

        Thread.sleep(2*1000);

        // extend the probe....
        request.setTTL((byte)3);
        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        try {
            LEAF.receive(TIMEOUT);
            fail("expected InterruptedIOException");
        }
        catch (InterruptedIOException expected) {}

        reqRecvd = getFirstQueryRequest(ULTRAPEER_1);
        assertNotNull(reqRecvd);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
        assertEquals((byte)1, reqRecvd.getHops());

        Thread.sleep(2*1000);

        // extend it again but make sure it doesn't get through...
        request.setTTL((byte)4);
        ULTRAPEER_2.send(request);
        ULTRAPEER_2.flush();

        // should NOT be forwarded to leaf again....
        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        try {
            reqRecvd = (QueryRequest) LEAF.receive(TIMEOUT);
            fail("expected InterruptedIOException");
        }
        catch (InterruptedIOException expected) {}

        // should NOT be forwarded to other Ultrapeer....
        assertTrue(noUnexpectedMessages(ULTRAPEER_1));
    }

    // tries to extend queries with original TTL > 1, should fail...
    public void testProbeIsTTL1Only() throws Exception {
        for (int i = 2; i < 5; i++) {
            drainAll();

            QueryRequest request = QueryRequest.createQuery("berkeley");
            request.setTTL((byte)i);

            ULTRAPEER_2.send(request);
            ULTRAPEER_2.flush();

            QueryRequest reqRecvd = (QueryRequest) LEAF.receive(TIMEOUT);
            assertEquals("berkeley", reqRecvd.getQuery());
            assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

            // should be forwarded to other Ultrapeer
            reqRecvd = getFirstQueryRequest(ULTRAPEER_1);
            assertNotNull(reqRecvd);
            assertEquals("berkeley", reqRecvd.getQuery());
            assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
            assertEquals((byte)1, reqRecvd.getHops());
            
            Thread.sleep(2*1000);
            
            // extend the probe....
            request.setTTL((byte)(i+1));
            ULTRAPEER_2.send(request);
            ULTRAPEER_2.flush();

            // should be counted as a duplicate and not forwarded anywhere...
            // leaves don't get any unexpected messages, no use using
            // noUnenexpectedMessages
            try {
                LEAF.receive(TIMEOUT);
                fail("expected InterruptedIOException");
            }
            catch (InterruptedIOException expected) {}

            assertTrue(noUnexpectedMessages(ULTRAPEER_1));
        }
    }

    

}
