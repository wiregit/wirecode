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
    private static final int ULTRAPEER_PORT = 6667;

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
			new Connection("localhost", ULTRAPEER_PORT, 
						   new LeafHeaders("localhost"),
						   new EmptyResponder()
						   );
        
        ULTRAPEER_1 = 
			new Connection("localhost", ULTRAPEER_PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder()
						   );

        ULTRAPEER_2 = 
			new Connection("localhost", ULTRAPEER_PORT,
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
        ConnectionSettings.PORT.setValue(ULTRAPEER_PORT);
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


        assertEquals("unexpected port", ULTRAPEER_PORT, 
					 ConnectionSettings.PORT.getValue());

		ROUTER_SERVICE.start();
		RouterService.clearHostCatcher();
		RouterService.connect();	
		connect();
        assertEquals("unexpected port", ULTRAPEER_PORT, 
					 ConnectionSettings.PORT.getValue());
	}

	public void tearDown() throws Exception {
        drainAll();
		sleep();
		LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
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
 			drain(ULTRAPEER_1);
 		}
 		if(ULTRAPEER_1.isOpen()) {
 			drain(ULTRAPEER_2);
 		}
 		if(LEAF.isOpen()) {
 			drain(LEAF);
 		}
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private void connect() throws Exception {
		buildConnections();
        //1. first Ultrapeer connection 
        ULTRAPEER_2.initialize();
        ULTRAPEER_2.buildAndStartQueues();

        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize();
        ULTRAPEER_1.buildAndStartQueues();
        
        //3. routed leaf, with route table for "test"
        LEAF.initialize();
        LEAF.buildAndStartQueues();
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("test");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.writer().simpleWrite((RouteTableMessage)iter.next());
			LEAF.writer().flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("awesome");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER_1.writer().simpleWrite((RouteTableMessage)iter.next());
			ULTRAPEER_1.writer().flush();
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
                c.receive(TIMEOUT);
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


    /**
     * Tests to make sure that pongs are received properly via
     * pong caching.
     */
    public void testPongsReceivedFromPing() throws Exception {
        PingPongSettings.PINGS_ACTIVE.setValue(false);

        for(int i=0; i<PongCacher.NUM_HOPS+4; i++) {
            PingReply curPong = PingReply.create(new GUID().bytes(),
                                                           (byte)3);
            for(int j=0; j<i; j++) {
                if(j < PongCacher.NUM_HOPS) {
                    curPong.hop();
                }
            }
            PongCacher.instance().addPong(curPong);            
        }
		drain(ULTRAPEER_1);
		drain(ULTRAPEER_2);
		drain(LEAF);

        Message m = new PingRequest((byte)7);
        ULTRAPEER_1.writer().simpleWrite(m);
        ULTRAPEER_1.writer().flush();        
        
        Message received;
        for(int i=0; i<PongCacher.NUM_HOPS; i++) {
            received = ULTRAPEER_1.receive(TIMEOUT);
            assertInstanceof("message should be a pong", PingReply.class, 
                             received);
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
