package com.limegroup.gnutella;

import java.io.File;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EmptyResponder;
import com.sun.java.util.collections.Iterator;

/**
 *  Tests that a Ultrapeer correctly sends XML Replies.  
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test should cover the case for leaves too, since there is no difference
 *  between Leaf and UP when it comes to this behavior.
 */
public final class ServerSideXMLReplyTest extends BaseTestCase {

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

    public ServerSideXMLReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideXMLReplyTest.class);
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
        //To keep LimeWire from connecting to the outside network, we filter out
        //all addresses but localhost and 18.239.0.*.  The latter is used in
        //pongs for testing.  TODO: it would be nice to have a way to prevent
        //BootstrapServerManager from adding defaults and connecting.
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
        ConnectionSettings.PORT.setValue(PORT);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("mp3;");
        // get the resource file for com/limegroup/gnutella
        File mp3 = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/mp3/mpg2layII_1504h_16k_frame56_24000hz_joint_CRCOrigID3v1&2_test27.mp3");
        assertTrue(mp3.exists());
        // now move them to the share dir        
        CommonUtils.copy(mp3, new File(_sharedDir, "metadata.mp3"));
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
    // BEGIN TESTS
    // ------------------------------------------------------

    public void testXMLReturned1() throws Exception {
        drainAll();

        // send a query
        QueryRequest query = QueryRequest.createQuery("metadata");
        ULTRAPEER_1.send(query);
        ULTRAPEER_1.flush();

        // wait for processing
        Thread.sleep(750);

        // confirm that result has heXML.
        QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
        assertNotNull(reply);
        assertNotNull(reply.getXMLBytes());
        assertTrue("xml length = " + reply.getXMLBytes().length,
                   reply.getXMLBytes().length > 10);
    }

    public void testXMLReturned2() throws Exception {
        drainAll();

        String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Alternative\"></audio></audios>";

        // send a query
        QueryRequest query = QueryRequest.createQuery("Alternative", richQuery);
        ULTRAPEER_1.send(query);
        ULTRAPEER_1.flush();

        // wait for processing
        Thread.sleep(750);


        // confirm that result has heXML.
        QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
        assertNotNull(reply);
        assertNotNull(reply.getXMLBytes());
        assertTrue("xml length = " + reply.getXMLBytes().length,
                   reply.getXMLBytes().length > 10);
    }

    public void testBitrateExclusion() throws Exception {
        // test that a mismatching artist name doesn't return a result
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"junk\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("junk 16", richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNull(reply);
        }        

        // test that a matching artist name does return a result
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("Test 16", richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we do get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNotNull(reply);
            assertNotNull(reply.getXMLBytes());
            assertTrue("xml length = " + reply.getXMLBytes().length,
                       reply.getXMLBytes().length > 10);
        }        

        // test that a null price value doesn't return a result
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" price=\"$19.99\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("$19.99 16", 
                                                          richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNull(reply);
        }        

        // 3 fields - bitrate matches, but only one other, so no return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\" title=\"junk\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("Test junk 16", 
                                                          richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNull(reply);
        }        

        // 3 fields - all match, should return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\" title=\"Test mpg\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("Test mpg 16", 
                                                          richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we do get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNotNull(reply);
            assertNotNull(reply.getXMLBytes());
            assertTrue("xml length = " + reply.getXMLBytes().length,
                       reply.getXMLBytes().length > 10);
        }        

        // 3 fields - 1 match, 1 null, should return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" artist=\"Test\" type=\"Audiobook\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("Test Audiobook 16", 
                                                          richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we do get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNotNull(reply);
            assertNotNull(reply.getXMLBytes());
            assertTrue("xml length = " + reply.getXMLBytes().length,
                       reply.getXMLBytes().length > 10);
        }        

        // 3 fields - 2 null, should not return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" price=\"$19.99\" type=\"Audiobook\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("$19.99 Audiobook 16", 
                                                          richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNull(reply);
        }        

        // 3 fields - 1 null, 1 mismatch, should not return
        {
            drainAll();

            String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio bitrate=\"16\" price=\"$19.99\" artist=\"Tester\"></audio></audios>";

            // send a query
            QueryRequest query = QueryRequest.createQuery("$19.99 Tester 16", 
                                                          richQuery);
            ULTRAPEER_1.send(query);
            ULTRAPEER_1.flush();

            // wait for processing
            Thread.sleep(750);

            // confirm that we don't get a result
            QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
            assertNull(reply);
        }        


    }
    

}
