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
import java.net.*;

/**
 *  Tests that an Ultrapeer correctly handles out-of-band queries.  Essentially 
 *  tests the following methods of MessageRouter: handleQueryRequest,
 *  handleUDPMessage, handleLimeAckMessage, Expirer, QueryBundle, sendQueryReply
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 */
public final class ServerSideOutOfBandReplyTest extends BaseTestCase {

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
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    /**
	 * Second Ultrapeer connection
     */
    private static Connection ULTRAPEER_2;

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public ServerSideOutOfBandReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideOutOfBandReplyTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private static void buildConnections() throws Exception {
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

        UDP_ACCESS = new DatagramSocket();

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
        settings.setAllowedIps(new String[] {"127.*.*.*"});
        settings.setPort(PORT);
        settings.setExtensions("txt;");
        File shareDir[] = {new File("com/limegroup/gnutella")};
        assertTrue(shareDir[0].isDirectory());
        settings.setDirectories(shareDir);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(1);
		ConnectionSettings.KEEP_ALIVE.setValue(3);
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
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
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

		System.out.println(m); 
		assertInstanceof("message not a QueryRequest",
		    QueryRequest.class, m);
	}


    // BEGIN TESTS
    // ------------------------------------------------------

    // make sure that the Ultrapeer discards out-of-band query requests that
    // have an improper address associated with it
    public void testIdentity() throws Exception {
        drainAll();

        byte[] crapIP = {0,0,0,0};
        QueryRequest query = QueryRequest.createOutOfBandQuery("berkeley", 
                                                               crapIP, 6346);
        LEAF.send(query);
        LEAF.flush();

        // ultrapeers should NOT get the QR
        assertTrue(getFirstQueryRequest(ULTRAPEER_1) == null);
        assertTrue(getFirstQueryRequest(ULTRAPEER_2) == null);
        
        // try a good query
        query = QueryRequest.createOutOfBandQuery("berkeley", 
                                                  LEAF.getLocalAddress().getAddress(),
                                                  6346);
        LEAF.send(query);
        LEAF.flush();

        // ultrapeers should get the QR
        assertTrue(getFirstQueryRequest(ULTRAPEER_1) != null);
        assertTrue(getFirstQueryRequest(ULTRAPEER_2) != null);

        // LEAF should get the reply
        assertTrue(getFirstQueryReply(LEAF) != null);
    }

    // a node should NOT send a reply out of band via UDP if it is not
    // far away (low hop)
    public void testLowHopOutOfBandRequest() throws Exception {
        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("susheel",
                                              UDP_ACCESS.getLocalAddress().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        ULTRAPEER_2.send(query);
        ULTRAPEER_2.flush();

        // ULTRAPEER_2 should get a reply via TCP
        assertTrue(getFirstQueryReply(ULTRAPEER_2) != null);
    }

    // tests basic out of band functionality
    // this tests no UDP support - it should just send a UDP reply
    public void testOutOfBandRequest() throws Exception {
        drainAll();
    }

    // tests basic out of band functionality
    // this tests solicited UDP support - it should participate in ACK exchange
    public void testOutOfBandRequestWithSolicitedSupport() throws Exception {
        drainAll();
    }

    // tests that MessageRouter expires GUIDBundles/Replies in a timely fashion
    // this test requires solicited support
    public void testExpirer() throws Exception {
        drainAll();
    }

    

}
