package com.limegroup.gnutella.messages.vendor;

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
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/**
 *  Tests that an Ultrapeer correctly handles out-of-band queries.  Essentially 
 *  tests the following methods of MessageRouter: handleQueryRequest,
 *  handleUDPMessage, handleLimeAckMessage, Expirer, QueryBundle, sendQueryReply
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                       |    |    |    |
 *                       |    |    |    |
 *                       |    |    |    |
 *                     LEAF  LEAF LEAF LEAF
 *
 *  This test should cover the case for leaves too, since there is no difference
 *  between Leaf and UP when it comes to this behavior.
 */
public final class ServerSideGiveStatsVMTest extends BaseTestCase {

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
	private static final byte SOFT_MAX = 3;

	/**
	 * The TTL of the initial "probe" queries that the Ultrapeer uses to
	 * determine how widely distributed a file is.
	 */
	private static final byte PROBE_QUERY_TTL = 2;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static Connection LEAF_1;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static Connection LEAF_2;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static Connection LEAF_3;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static Connection LEAF_4;


    /**
     * Leaf connection to the Ultrapeer.
     */
    private static Connection TCP_TEST_LEAF;

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
     * query 1 will find it's way to leaves 1, 2, 3 and UP 2 -- See the QRT and
     * other setup later in the test to see why
     */
    private final static QueryRequest query1 = 
        QueryRequest.createQuery("sumeet", (byte)3);

    /**
     * query 2 will find it's way to leaves 1 and 2 and no UPs -- See the QRT
     * and other setup later in the test to see why 
     */
    private final static QueryRequest query2 = 
        QueryRequest.createQuery("ashish", (byte)3);
    /**
     * query 3 will not go anywhere at all -- -- See the QRT and other setup
     * later in the test to see why 
     */
    private static final QueryRequest query3 = 
        QueryRequest.createQuery("john", (byte)3);
    
    /** GUID of the first query */
    private static final GUID GUID1 = new GUID(query1.getGUID());
    /** GUID of the second query */
    private static final GUID GUID2 = new GUID(query2.getGUID());
    /** GUID of the third query */
    private static final GUID GUID3 = new GUID(query3.getGUID());

    private static final GUID l1GUID = new GUID(GUID.makeGuid());
    private static final GUID l2GUID = new GUID(GUID.makeGuid());
    private static final GUID l3GUID = new GUID(GUID.makeGuid());
    

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public ServerSideGiveStatsVMTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideGiveStatsVMTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private static void buildConnections() throws Exception {
	    LEAF_1 =
			new Connection("localhost", PORT, new LeafHeaders("localhost"),
                                                          new EmptyResponder());

	    LEAF_2 =
			new Connection("localhost", PORT, new LeafHeaders("localhost"),
						                                  new EmptyResponder());

	    LEAF_3 =
			new Connection("localhost", PORT, new LeafHeaders("localhost"),
                                                         new EmptyResponder());

	    LEAF_4 =
			new Connection("localhost", PORT, new LeafHeaders("localhost"),
                                                        new EmptyResponder() );

	    TCP_TEST_LEAF =
			new Connection("localhost", PORT, new LeafHeaders("localhost"),
						                                 new EmptyResponder() );
        
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
        CommonUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        CommonUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(30);
		ConnectionSettings.NUM_CONNECTIONS.setValue(30);
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
		LEAF_1.close();
		LEAF_2.close();
		LEAF_3.close();
		LEAF_4.close();
        TCP_TEST_LEAF.close();
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
 		if(LEAF_1.isOpen()) {
 			drain(LEAF_1);
 		}
 		if(LEAF_2.isOpen()) {
 			drain(LEAF_2);
 		}
 		if(LEAF_3.isOpen()) {
 			drain(LEAF_3);
 		}
 		if(LEAF_4.isOpen()) {
 			drain(LEAF_4);
 		}
 		if(TCP_TEST_LEAF.isOpen()) {
 			drain(TCP_TEST_LEAF);
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
        LEAF_1.initialize();
        LEAF_2.initialize();
        LEAF_3.initialize();
        LEAF_4.initialize();
        TCP_TEST_LEAF.initialize();
                
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("ashish");
        qrt.add("sumeet");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            RouteTableMessage message = (RouteTableMessage)iter.next();
            LEAF_1.send(message);
			LEAF_1.flush();
            LEAF_2.send(message);
			LEAF_2.flush();
            
            //Leaf 4 does not have either 
            TCP_TEST_LEAF.send(message);
			TCP_TEST_LEAF.flush();
        }
        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("sumeet");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            RouteTableMessage message = (RouteTableMessage)iter.next();
            //ULTRAPEER_1.send(message);
			//ULTRAPEER_1.flush();
            LEAF_3.send(message);
			LEAF_3.flush();
        }

		assertTrue("ULTRAPEER_2 should be connected", ULTRAPEER_2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", ULTRAPEER_1.isOpen());
		assertTrue("LEAF should be connected", LEAF_1.isOpen());
		assertTrue("LEAF should be connected", LEAF_2.isOpen());
		assertTrue("LEAF should be connected", LEAF_3.isOpen());
		assertTrue("LEAF should be connected", LEAF_4.isOpen());
		assertTrue("LEAF should be connected", TCP_TEST_LEAF.isOpen());

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		sleep();
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    /**
     * this is not a real test. It just sets stuff up
     */ 
    public void testSetStuffUp() throws Exception {
        DatagramPacket pack = null;
        // set up solicited UDP support
        {
            //drainAll(); //We just did drainAll before this test was started
            PingReply pong = 
                PingReply.create(GUID.makeGuid(), (byte) 4,
                                 UDP_ACCESS.getLocalPort(), 
                                 InetAddress.getLocalHost().getAddress(), 
                                 10, 10, true, 900, true);
            ULTRAPEER_1.send(pong);
            ULTRAPEER_1.flush();

            // wait for the ping request from the test UP
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
               fail("Did not get ping", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) Message.read(in);
            
            // send the pong in response to the ping
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong = PingReply.create(ping.getGUID(), (byte) 4,
                                    UDP_ACCESS.getLocalPort(), 
                                    InetAddress.getLocalHost().getAddress(), 
                                    10, 10, true, 900, true);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      pack.getAddress(), pack.getPort());
            UDP_ACCESS.send(pack);
        }

        // set up unsolicited UDP support
        {
            // tell the UP i can support UDP connect back
            MessagesSupportedVendorMessage support = 
                MessagesSupportedVendorMessage.instance();
            ULTRAPEER_1.send(support);
            ULTRAPEER_1.flush();

            byte[] cbGuid = null;
            int cbPort = -1;
            while (cbGuid == null) {
                try {
                    Message m = ULTRAPEER_1.receive(TIMEOUT);
                    if (m instanceof UDPConnectBackVendorMessage) {
                        UDPConnectBackVendorMessage udp = 
                            (UDPConnectBackVendorMessage) m;
                        cbGuid = udp.getConnectBackGUID().bytes();
                        cbPort = udp.getConnectBackPort();
                    }
                }
                catch (Exception ie) {
                    fail("did not get the UDP CB message!", ie);
                }
            }

            // ok, now just do a connect back to the up so unsolicited support
            // is all set up
            PingRequest pr = new PingRequest(cbGuid, (byte) 1, (byte) 0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pr.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      ULTRAPEER_1.getInetAddress(), cbPort);
            UDP_ACCESS.send(pack);
        }
        //GiveStatsSpecific traffic within our small network

        TCP_TEST_LEAF.send(query1);
        TCP_TEST_LEAF.flush();
        
        byte[] ipBytes = {(byte)127,(byte)0,(byte)0,(byte)1};

        QueryRequest qLeaf1 = null;
        QueryRequest qLeaf2 = null;
        QueryRequest qLeaf3 = null;
        QueryRequest qLeaf4 = null;
        QueryRequest qUP1 = null;
        QueryRequest qUP2 = null;

        //Leaf1 should get query1 and query2
        //If we get a ClassCastException here we are in deep trouble.
        qLeaf1 = (QueryRequest)getFirstInstanceOfMessageType(
                                                      LEAF_1,query1.getClass());
        qLeaf2 = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_2,query1.getClass());
        qLeaf3  = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_3,query1.getClass());
        qLeaf4  = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_4,query1.getClass());
        qUP1 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_1,query1.getClass());
        qUP2 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_2,query1.getClass());

        assertEquals("Wrong message reached LEAF_1", GUID1,
                     new GUID(qLeaf1.getGUID()));       
        assertEquals("Wrong message reached LEAF_2", GUID1, 
                     new GUID(qLeaf2.getGUID()));
        assertEquals("Wrong message reached LEAF_3", GUID1, 
                     new GUID(qLeaf3.getGUID()));
        assertEquals("Wrong message reached UP1", GUID1, 
                     new GUID(qUP1.getGUID()));
        //This expected even though the UP never sent qrp entries
        assertEquals("Wrong message reached UP2", GUID1, 
                     new GUID(qUP2.getGUID()));
        assertNull("Leaf4 got  messages when it should not have", qLeaf4);

                
        //Send the second query and make sure it goes to all the right places
        TCP_TEST_LEAF.send(query2);
        TCP_TEST_LEAF.flush();

        qLeaf1 = (QueryRequest)getFirstInstanceOfMessageType(
                                                      LEAF_1,query2.getClass());
        qLeaf2 = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_2,query2.getClass());
        qLeaf3  = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_3,query2.getClass());
        qLeaf4  = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_4,query2.getClass());
        qUP1 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_1,query2.getClass());
        qUP2 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_2,query2.getClass());
        assertEquals("Wrong message reached LEAF_1", GUID2, 
                     new GUID(qLeaf1.getGUID()));
        assertEquals("Wrong message reached LEAF_2", GUID2, 
                     new GUID(qLeaf2.getGUID()));
        assertEquals("Wrong message reached UP1", GUID2, 
                     new GUID(qUP1.getGUID()));
        //This expected even though the UP never sent qrp entries
        assertEquals("Wrong message reached UP2", GUID2, 
                     new GUID(qUP2.getGUID()));
        assertNull("Leaf got  messages when it should not have", qLeaf3);
        assertNull("Leaf got  messages when it should not have", qLeaf4);

        //Send the third query and make sure it goes to all the right places
        TCP_TEST_LEAF.send(query3);
        TCP_TEST_LEAF.flush();
        qLeaf1 = (QueryRequest)getFirstInstanceOfMessageType(
                                                      LEAF_1,query3.getClass());
        qLeaf2 = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_2,query3.getClass());
        qLeaf3  = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_3,query3.getClass());
        qLeaf4  = (QueryRequest)getFirstInstanceOfMessageType(
                                                    LEAF_4,query3.getClass());

        assertNull("Leaf got  messages when it should not have", qLeaf1);
        assertNull("Leaf got  messages when it should not have", qLeaf2);
        assertNull("Leaf got  messages when it should not have", qLeaf3);
        assertNull("Leaf got  messages when it should not have", qLeaf4);

        ////////////////////////////////////////////////////
        //TODO1: Why is Query3 not getting through to UP1 and UP2 when others
        //queries are
        ///////////////////////////////////////////////////
        qUP1 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_1,query3.getClass());
        qUP2 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_2,query3.getClass());
        assertNull("UP1 got query3 ", qUP1);
        assertNull("UP2 got query3 ", qUP2);
                                                                   
//          System.out.println(""+GUID1);
//          System.out.println(""+GUID2);
//          System.out.println(""+GUID3);


        //Now let make the leaves send some responses

        Response[] resps = {new Response(0l, 12l, "sumeet.txt") };
        QueryReply reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getListeningPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_1.send(reply1);
        LEAF_1.flush();

        Response[] r1 = {new Response(0l, 12l, "ashish.txt") };
        resps = r1;
        QueryReply reply2 = new QueryReply(GUID2.bytes(),(byte)3, 
             LEAF_1.getListeningPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_1.send(reply2);
        LEAF_1.flush();

        Response[] r2 = {new Response(1l, 13l, "sumeet.txt") };
        resps = r2;
        reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getListeningPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_2.send(reply1);
        LEAF_2.flush();
        
        Response[] r3 = {new Response(1l, 13l, "sumeet.txt") };
        resps = r3;
        reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getListeningPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_3.send(reply1);
        LEAF_3.flush();


        Response[] r4 = {new Response(1l, 13l, "sumeet.txt") };
        resps = r4;
        reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getListeningPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        ULTRAPEER_1.send(reply1);
        ULTRAPEER_1.flush();

        //Leaf 1 final score incoming queries = 2, query replies = 2
        //Leaf 2 final score incoming queries = 2, query replies = 1
        //LEAF_3 final score incoming queries = 1 query replies = 1
        //LEAF_4 final score incoming queries = 0 query replies = 0
        //UP1 final score incoming queries = 2 query replies = 1        
        //UP2 final score incoming queries = 2 query replies = 0

        //OK. Now we can send the Give Stats Message to the central UP, and see
        //what the response is
    }
     
    public void testTCPGiveStatsVM() throws Exception {
        GiveStatsVendorMessage statsVM = new GiveStatsVendorMessage(
                             GiveStatsVendorMessage.PER_CONNECTION_STATS, 
                             GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC, 
                             Message.N_TCP);        
        TCP_TEST_LEAF.send(statsVM);
        TCP_TEST_LEAF.flush();        
        
        StatisticVendorMessage statsAck = 
        (StatisticVendorMessage)getFirstInstanceOfMessageType(TCP_TEST_LEAF,
                                                               Class.forName(
            "com.limegroup.gnutella.messages.vendor.StatisticVendorMessage"));
        
        String returnedStats = new String(statsAck.getPayload());
        //TODO:1 make sure this is what is expected. 
        //System.out.println(returnedStats);               

        StringTokenizer tok = new StringTokenizer(returnedStats,":|");
        
        String token = tok.nextToken();//ignore
        token = tok.nextToken();//ignore
        token = tok.nextToken(); // UP2 sent -- should be 0
        //System.out.println("****Sumeet***:"+token);

        int val = Integer.parseInt(token.trim());//(Integer.valueOf(token)).intValue();

        assertEquals("UP2 sent no messages", 0, val);
        tok.nextToken();//ignore
        token = tok.nextToken(); //UP1 sent -- should be 3
        assertEquals("UP2 dropped 0 messages",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("UP1 sent 3 messages", 3, Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("UP1 dropped 1 sent message", 1, Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 sent 4 messages",4,Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 dropped no message", 0, Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_2 sent 3 messages",3,Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Lead_2 dropped no message",0,Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_3 sent 3 messages",3,Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Lead_3 dropped no message",0,Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_4 sent no messages", 0, Integer.parseInt(
                                                                 token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Lead_4 dropped no message", 0, Integer.parseInt(
                                                                token.trim()));



        GiveStatsVendorMessage statsVM2 = new GiveStatsVendorMessage(
                             GiveStatsVendorMessage.PER_CONNECTION_STATS,
                             GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC,
                             Message.N_TCP);
        TCP_TEST_LEAF.send(statsVM2);
        TCP_TEST_LEAF.flush();

        statsAck = 
        (StatisticVendorMessage)getFirstInstanceOfMessageType(TCP_TEST_LEAF,
                                                               Class.forName(
            "com.limegroup.gnutella.messages.vendor.StatisticVendorMessage"));
        
        returnedStats = new String(statsAck.getPayload());

        tok = new StringTokenizer(returnedStats,":|");
        
        token = tok.nextToken();//ignore
        token = tok.nextToken();//ignore
        token = tok.nextToken(); // UP2 sent -- should be 0
        //System.out.println("****Sumeet***:"+token);

        val = Integer.parseInt(token.trim());

        assertEquals("UP2 received 14 messages", 14, val);
        tok.nextToken();//ignore
        token = tok.nextToken(); //UP1 received -- should be 14
        assertEquals("UP2 dropped 0 sent messages",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("UP1 received 15 messages", 15, Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("UP1 dropped 0 sent message", 0, Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 received 3 messages", 3, Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 dropped no message", 0, Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_2 received 3 messages",3,Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Lead_2 dropped no message",0,Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_3 received 2 messages",2,Integer.parseInt(token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Lead_3 dropped no message",0,Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_4 received 1 message", 1, Integer.parseInt(
                                                                 token.trim()));
        tok.nextToken(); //ignore
        token = tok.nextToken();
        assertEquals("Lead_4 dropped no message", 0, Integer.parseInt(
                                                                token.trim()));


        //TODO:1 make sure this is what is expected. 
        //System.out.println(returnedStats);       

    }

   
    //OK. Now we should have some basic traffic flow between this little
    //network of ours. The querys are all created from TCP_TEST_LEAF and
    //sent to the "CENTRAL_UP" which is connected to every faked up node we
    //are using in this test.
    
    //Now lets pre-program some leaves and UPs to respond to specific
    //queries they willl get in order to be able to count the Gnutella
    //Traffic these nodes generate
    
        
    //Now let's create some queries and send them out to the central UP
    //which will forard to the appropriate leaves as per qrp.
    


}
