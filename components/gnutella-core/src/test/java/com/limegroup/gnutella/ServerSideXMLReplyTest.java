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
    private static final int ULTRAPEER_PORT = 6667;

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 2000;


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
        ConnectionSettings.PORT.setValue(ULTRAPEER_PORT);
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
		UltrapeerSettings.MAX_LEAVES.setValue(1);
		ConnectionSettings.NUM_CONNECTIONS.setValue(3);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
    }

	public static void globalSetUp() throws Exception {
        setSettings();

        assertEquals("unexpected port", ULTRAPEER_PORT, 
					 ConnectionSettings.PORT.getValue());

		ROUTER_SERVICE.start();
		RouterService.clearHostCatcher();
		RouterService.connect();	
		connect();
        assertEquals("unexpected port", ULTRAPEER_PORT, 
					 ConnectionSettings.PORT.getValue());
	}

    
    public void setUp() {
        setSettings();
    }


	public static void globalTearDown() throws Exception {
		RouterService.disconnect();
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
        ULTRAPEER_2.buildAndStartQueues();

        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize();
        ULTRAPEER_1.buildAndStartQueues();
        
        //3. routed leaf, with route table for "test"
        LEAF.initialize();
        LEAF.buildAndStartQueues();
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.writer().simpleWrite((RouteTableMessage)iter.next());
			LEAF.writer().flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
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


    // BEGIN TESTS
    // ------------------------------------------------------

    public void testXMLReturned1() throws Exception {
        drainAll();

        // send a query
        QueryRequest query = QueryRequest.createQuery("metadata");
        ULTRAPEER_1.writer().simpleWrite(query);
        ULTRAPEER_1.writer().flush();

        // confirm that result has heXML.
        QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
        assertTrue(reply != null);
        assertTrue(reply.getXMLBytes() != null);
        assertTrue("xml length = " + reply.getXMLBytes().length,
                   reply.getXMLBytes().length > 10);
    }

    public void testXMLReturned2() throws Exception {
        drainAll();

        String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio genre=\"Alternative\"></audio></audios>";

        // send a query
        QueryRequest query = QueryRequest.createQuery("Alternative", richQuery);
        ULTRAPEER_1.writer().simpleWrite(query);
        ULTRAPEER_1.writer().flush();

        // confirm that result has heXML.
        QueryReply reply = getFirstQueryReply(ULTRAPEER_1);
        assertTrue(reply != null);
        assertTrue(reply.getXMLBytes() != null);
        assertTrue("xml length = " + reply.getXMLBytes().length,
                   reply.getXMLBytes().length > 10);
    }

}
