package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;

import junit.framework.*;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * This test makes sure that pong caching is working correctly between
 * Ultrapeers.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 */
public final class PongCachingTest extends BaseTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static final int SERVER_PORT = 6667;

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 1800;


    /**
     * Leaf connection to the Ultrapeer.
     */
    private Connection LEAF;

    /**
     * Ultrapeer connection.
     */
    private Connection ULTRAPEER_1;

    /**
	 * Second Ultrapeer connection
     */
    private Connection ULTRAPEER_2;

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public PongCachingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PongCachingTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private void buildConnections() {
	    LEAF =
			new Connection("localhost", SERVER_PORT, 
						   new LeafHeaders("localhost"),
						   new EmptyResponder()
						   );
        
        ULTRAPEER_1 = 
			new Connection("localhost", SERVER_PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder()
						   );

        ULTRAPEER_2 = 
			new Connection("localhost", SERVER_PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder()
						   );
    }

	public void setUp() throws Exception {
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
        ConnectionSettings.PORT.setValue(SERVER_PORT);
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
		ConnectionSettings.SEND_QRP.setValue(false);

        assertEquals("unexpected port", SERVER_PORT, 
					 ConnectionSettings.PORT.getValue());

		ROUTER_SERVICE.start();
		RouterService.clearHostCatcher();
		RouterService.connect();	
		connect();
        assertEquals("unexpected port", SERVER_PORT, 
					 ConnectionSettings.PORT.getValue());
	}

	public void tearDown() throws Exception {
        drainAll();
		sleep();
		LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
        ConnectionSettings.SEND_QRP.setValue(true);
		RouterService.disconnect();
		sleep();
	}

	private void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}

	/**
	 * Drains all messages 
	 */
 	private void drainAll() throws Exception {
 		if(ULTRAPEER_1.isOpen()) {
 			drain(ULTRAPEER_1, TIMEOUT);
 		}
 		if(ULTRAPEER_1.isOpen()) {
 			drain(ULTRAPEER_2, TIMEOUT);
 		}
 		if(LEAF.isOpen()) {
 			drain(LEAF, TIMEOUT);
 		}
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private void connect() throws Exception {
		buildConnections();
        //1. first Ultrapeer connection 
        ULTRAPEER_2.initialize();
        assertTrue("should be open", ULTRAPEER_2.isOpen());
        assertTrue("should be up", ULTRAPEER_2.isSupernodeSupernodeConnection());

        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize();
        assertTrue("should be open", ULTRAPEER_1.isOpen());
        assertTrue("should be up", ULTRAPEER_1.isSupernodeSupernodeConnection());        
        
        //3. routed leaf, with route table for "test"
        LEAF.initialize();
        assertTrue("should be open", LEAF.isOpen());
        assertTrue("should be up", LEAF.isClientSupernodeConnection());        
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("test");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("awesome");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER_1.send((RouteTableMessage)iter.next());
			ULTRAPEER_1.flush();
        }

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
    }

    /**
     * Tests to make sure that pongs are received properly via
     * pong caching.
     */
    public void testPongsReceivedFromPing() throws Exception {
        PingPongSettings.PINGS_ACTIVE.setValue(false);


        byte[] ip = { (byte)1, (byte)2, (byte)3, (byte)4 };

        for(int i=0; i<PongCacher.NUM_HOPS+4; i++) {
            PingReply curPong = 
                PingReply.create(new GUID().bytes(), (byte)3, 13232, ip, 0, 0, 
                    true, -1, false);
            for(int j=0; j<i; j++) {
                if(j < PongCacher.NUM_HOPS) {
                    curPong.hop();
                }
            }
            PongCacher.instance().addPong(curPong);            
        }
        
        List pongs = PongCacher.instance().getBestPongs();
        assertEquals( PongCacher.NUM_HOPS, pongs.size() );

        Message m = new PingRequest((byte)7);
        ULTRAPEER_1.send(m);
        ULTRAPEER_1.flush();        
        
        Message received;   
        for(int i=0; i<PongCacher.NUM_HOPS; i++) {
            received = getFirstMessageOfType(ULTRAPEER_1, PingReply.class, 10000);
            assertNotNull("should have gotten pong. hop: " + i, received);
        }
        PingPongSettings.PINGS_ACTIVE.setValue(true);
    }

    /**
     * Test to make sure that pings are periodically sent as they should be.
     */
    public void testPingsSentPeriodically() throws Exception {

        // make sure we keep getting pings
        for(int i=0; i<3; i++) {
            Message m = ULTRAPEER_2.receive(Pinger.PING_INTERVAL+200);
            assertInstanceof("message should be a ping request", 
                             PingRequest.class, m);
        }
    }
}
