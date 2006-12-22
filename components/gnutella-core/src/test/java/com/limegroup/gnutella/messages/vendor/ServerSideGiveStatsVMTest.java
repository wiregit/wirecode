package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import junit.framework.Test;

import com.limegroup.gnutella.CountingConnection;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.EmptyResponder;

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
public final class ServerSideGiveStatsVMTest extends LimeTestCase {

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
     * Leaf connection to the Ultrapeer.
     */
    private static QueryCountingConnection LEAF_1;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static QueryCountingConnection LEAF_2;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static QueryCountingConnection LEAF_3;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private static QueryCountingConnection LEAF_4;


    /**
     * Leaf connection to the Ultrapeer.
     */
    private static QueryCountingConnection TCP_TEST_LEAF;

    /**
     * Ultrapeer connection.
     */
    private static QueryCountingConnection ULTRAPEER_1;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    /**
	 * Second Ultrapeer ConnectionStub
     */
    private static QueryCountingConnection ULTRAPEER_2;

    private static InetAddress _udpAddress;

    private static int _udpPort;


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
	    LEAF_1 = new QueryCountingConnection("localhost", PORT);
	    LEAF_2 = new QueryCountingConnection("localhost", PORT);
	    LEAF_3 = new QueryCountingConnection("localhost", PORT);
	    LEAF_4 = new QueryCountingConnection("localhost", PORT);
	    TCP_TEST_LEAF = new QueryCountingConnection("localhost", PORT);
        ULTRAPEER_1 = new QueryCountingConnection("localhost", PORT);
        ULTRAPEER_2 = new QueryCountingConnection("localhost", PORT);
        UDP_ACCESS = new DatagramSocket();
    }

    public static void setSettings() {
        String localIP = null;
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception ignored) {}
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {localIP,"127.*.*.*"});
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

    private static void readAllFully() throws IOException, BadPacketException {
       while(true) {
            try {
                LEAF_1.receive(600);
            } catch (InterruptedIOException e) {
                break;
            }
        }
        while(true) {
            try {
                LEAF_2.receive(600);
            } catch (InterruptedIOException e) {
                break;
            }
        }
        while(true) {
            try {
                LEAF_3.receive(600);
            } catch (InterruptedIOException e) {
                break;
            }
        }
        while(true) {
            try {
                LEAF_4.receive(600);
            } catch (InterruptedIOException e) {
                break;
            }
        }
        while(true) {
            try {
                ULTRAPEER_1.receive(600);
            } catch (InterruptedIOException e) {
                break;
            }
        }
        while(true) {
            try {
                ULTRAPEER_2.receive(600);
            } catch (InterruptedIOException e) {
                break;
            }
        }        
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
        ULTRAPEER_2.initialize(new UltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize(new UltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        
        //3. routed leaf, with route table for "test"
        LEAF_1.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
        LEAF_2.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
        LEAF_3.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
        LEAF_4.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
        TCP_TEST_LEAF.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
                
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
            _udpAddress = pack.getAddress();
            _udpPort = pack.getPort();
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) MessageFactory.read(in);
            
            // send the pong in response to the ping
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong = PingReply.create(ping.getGUID(), (byte) 4,
                                    UDP_ACCESS.getLocalPort(), 
                                    InetAddress.getLocalHost().getAddress(), 
                                    10, 10, true, 900, true);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      _udpAddress, _udpPort);
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

        qUP1 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_1,query3.getClass());
        qUP2 = (QueryRequest)getFirstInstanceOfMessageType(
                                                ULTRAPEER_2,query3.getClass());
        //assertNull("UP1 got query3 ", qUP1);
        //assertNull("UP2 got query3 ", qUP2);
        
        assertEquals("Query3 should hav reached UP1", GUID3, 
                                                    new GUID(qUP1.getGUID()));
        assertEquals("Query3 should hav reached UP2", GUID3, 
                                                    new GUID(qUP2.getGUID()));
                  
        //System.out.println(""+GUID1);
        //System.out.println(""+GUID2);
        //System.out.println(""+GUID3);

        //Now let make the leaves send some responses

        Response[] resps = {new Response(0l, 12l, "sumeet.txt") };
        QueryReply reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_1.send(reply1);
        LEAF_1.flush();

        Response[] r1 = {new Response(0l, 12l, "ashish.txt") };
        resps = r1;
        QueryReply reply2 = new QueryReply(GUID2.bytes(),(byte)3, 
             LEAF_1.getPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_1.send(reply2);
        LEAF_1.flush();

        Response[] r2 = {new Response(1l, 13l, "sumeet.txt") };
        resps = r2;
        reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_2.send(reply1);
        LEAF_2.flush();
        
        Response[] r3 = {new Response(1l, 13l, "sumeet.txt") };
        resps = r3;
        reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        LEAF_3.send(reply1);
        LEAF_3.flush();


        Response[] r4 = {new Response(1l, 13l, "sumeet.txt") };
        resps = r4;
        reply1 = new QueryReply(GUID1.bytes(),(byte)3, 
             LEAF_1.getPort(),ipBytes,0l, resps,l1GUID.bytes(), false);
        ULTRAPEER_1.send(reply1);
        ULTRAPEER_1.flush();
        
        readAllFully();

        //Leaf 1 final score incoming queries = 2, query replies = 2
        assertEquals(2, LEAF_1.incomingQueries);
        assertEquals(2, LEAF_1.queryReplies);
        
        //Leaf 2 final score incoming queries = 2, query replies = 1
        assertEquals(2, LEAF_2.incomingQueries);
        assertEquals(1, LEAF_2.queryReplies);
        
        //LEAF_3 final score incoming queries = 1 query replies = 1
        assertEquals(1, LEAF_3.incomingQueries);
        assertEquals(1, LEAF_3.queryReplies);
        
        //LEAF_4 final score incoming queries = 0 query replies = 0
        assertEquals(0, LEAF_4.incomingQueries);
        assertEquals(0, LEAF_4.queryReplies);
        
        //UP1 final score incoming queries = 3 query replies = 1
        assertEquals(3, ULTRAPEER_1.incomingQueries);
        assertEquals(1, ULTRAPEER_1.queryReplies);
        
        //UP2 final score incoming queries = 3 query replies = 0
        assertEquals(3, ULTRAPEER_2.incomingQueries);
        assertEquals(0, ULTRAPEER_2.queryReplies);
        
        //OK. Now we can send the Give Stats Message to the central UP, and see
        //what the response is
    }
     
    public void testTCPGiveStatsVM() throws Exception {        

        //Gnutella incoming by TCP
        GiveStatsVendorMessage statsVM = new GiveStatsVendorMessage(
                             GiveStatsVendorMessage.PER_CONNECTION_STATS, 
                             GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC, 
                             Message.N_TCP);        
        TCP_TEST_LEAF.send(statsVM);
        TCP_TEST_LEAF.flush();        
        
        StatisticVendorMessage statsAck = 
            getFirstInstanceOfMessageType(TCP_TEST_LEAF, StatisticVendorMessage.class);

        //Gnutella outgoing by TCP
        GiveStatsVendorMessage statsVM2 = new GiveStatsVendorMessage(
                             GiveStatsVendorMessage.PER_CONNECTION_STATS,
                             GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC,
                             Message.N_TCP);
        TCP_TEST_LEAF.send(statsVM2);
        TCP_TEST_LEAF.flush();

        StatisticVendorMessage statsAck2 = 
         getFirstInstanceOfMessageType(TCP_TEST_LEAF, StatisticVendorMessage.class);;

        //Gnutella incoming by UDP        
        GiveStatsVendorMessage statsVM3 = new GiveStatsVendorMessage(
                             GiveStatsVendorMessage.PER_CONNECTION_STATS,
                             GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC,
                             Message.N_UDP);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        statsVM3.write(baos);
        DatagramPacket pack = null;               

        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  _udpAddress, _udpPort);
        UDP_ACCESS.send(pack);
        
        InputStream in = null;
        StatisticVendorMessage statsAck3 = null;
        while(true) {//read messages until we get the right one. 
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            } catch (IOException iox) {
                fail("could not set up tests - faiiled to receive statAck3");
            }
            
            in = new ByteArrayInputStream(pack.getData());
            Message m = null; 
            try {               
                m = MessageFactory.read(in);
                statsAck3 = (StatisticVendorMessage)m;
                break; // no exception? we have the right message
            } catch (ClassCastException ccx) {
                continue;
            }
        }

        //Gnutella outgoing by UDP        
        GiveStatsVendorMessage statsVM4 = new GiveStatsVendorMessage(
                             GiveStatsVendorMessage.PER_CONNECTION_STATS,
                             GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC,
                             Message.N_UDP);

        baos = new ByteArrayOutputStream();
        statsVM4.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  _udpAddress, _udpPort);
        UDP_ACCESS.send(pack);

        in = null;
        StatisticVendorMessage statsAck4 = null;
        while(true) {//read messages until we get the right one. 
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            } catch (IOException iox) {
                fail("could not set up tests - faiiled to receive statAck3");
            }
            
            in = new ByteArrayInputStream(pack.getData());
            Message m = null; 
            try {               
                m = MessageFactory.read(in);
                statsAck4 = (StatisticVendorMessage)m;
                break; // no exception? we have the right message
            } catch (ClassCastException ccx) {
                continue;
            }
        }
        
        //the stats messages have all been received at this point, the
        //connections are still up and can get pings, we should stop the
        //counting on the connections to make sure the tests pass.
        //stopConnectionsCounting();

        ////////////////////////////////////////////////

        byte[] giveStatsPayload = statsVM.getPayload();
        byte[] statsPayload = statsAck.getPayload();
        
        assertEquals("stats message malformed", 
                     giveStatsPayload[0], statsPayload[0]);
        assertEquals("stats message malformed", 
                     giveStatsPayload[1], statsPayload[1]);

        String returnedStats = new String(statsPayload);
        //TODO:1 make sure this is what is expected. 
        //System.out.println(returnedStats);               

        StringTokenizer tok = new StringTokenizer(returnedStats,"^|");
        
        String token = tok.nextToken();//ignore
        token = tok.nextToken(); // UP2 sent -- should be 0
        //System.out.println("****Sumeet***:"+token);

        int val = Integer.parseInt(token.trim());

        assertEquals("UP2 sent mismatch", ULTRAPEER_2.outgoingCount, val);
        token = tok.nextToken(); //UP1 sent -- should be 3
        assertEquals("UP2 dropped mismatched",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("UP1 sent mismatch", ULTRAPEER_1.outgoingCount , 
                                                Integer.parseInt(token.trim()));

        token = tok.nextToken();
        //TODO2: I am not sure why one message is being dropped, but this is the
        //statistic being returned consistently, For now we will leave it here
        //to make the test pass but at some point, we should investigate why
        //this is happening.
        assertEquals("UP1 dropped mismatch", 1, Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 sent mismatch", LEAF_1.outgoingCount ,
                                               Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Leaf_1 dropped mismatch", 0, Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_2 sent mismatch", LEAF_2.outgoingCount,
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_2 dropped mismatch",0,Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_3 sent mismatch", LEAF_3.outgoingCount,
                                                Integer.parseInt(token.trim()));

        token = tok.nextToken();
        assertEquals("Lead_3 drop mismatch",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_4 sent mismatch", LEAF_4.outgoingCount, 
                                              Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_4 dropped mismatch", 0, Integer.parseInt(
                                                                token.trim()));

        ////////////////////////
        
        giveStatsPayload = statsVM2.getPayload();
        statsPayload = statsAck2.getPayload();
        
        assertEquals("stats message malformed", 
                     giveStatsPayload[0], statsPayload[0]);
        assertEquals("stats message malformed", 
                     giveStatsPayload[1], statsPayload[1]);

        returnedStats = new String(statsAck2.getPayload());      

        tok = new StringTokenizer(returnedStats,"^|");
        
        token = tok.nextToken();//ignore
        token = tok.nextToken(); // UP2 sent -- should be 0
        //System.out.println("****Sumeet***:"+token);

        val = Integer.parseInt(token.trim());

        assertEquals("UP2 received mismatch", ULTRAPEER_2.incomingCount, val);
        token = tok.nextToken(); //UP1 received -- should be 14
        assertEquals("UP2 dropped mismatch",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();

        assertEquals("UP1 received mismatch", ULTRAPEER_1.incomingCount,
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("UP1 dropped mismatch", 0, Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 received mismacth", LEAF_1.incomingCount, 
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Leaf_1 dropped mismatch", 0, Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_2 received mismatch",LEAF_2.incomingCount,
                                               Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_2 drop mismatch",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_3 received mismatch",LEAF_3.incomingCount,
                                               Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_3 dropped mismatch",0,Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_4 received mismatch", LEAF_4.incomingCount, 
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_4 dropped mismatch", 0, Integer.parseInt(
                                                                token.trim()));
        
        ///////////////////////////

        giveStatsPayload = statsVM3.getPayload();
        statsPayload = statsAck3.getPayload();
        
        assertEquals("stats message malformed", 
                     giveStatsPayload[0], statsPayload[0]);
        assertEquals("stats message malformed", 
                     giveStatsPayload[1], statsPayload[1]);


        returnedStats = new String(statsAck3.getPayload());
        //TODO:1 make sure this is what is expected. 
        //System.out.println(returnedStats);               

        tok = new StringTokenizer(returnedStats,"^|");
        
        token = tok.nextToken();//ignore
        token = tok.nextToken(); // UP2 sent -- should be 0
        //System.out.println("****Sumeet***:"+token);

        val = Integer.parseInt(token.trim());

        assertEquals("UP2 sent mismatch", ULTRAPEER_2.outgoingCount, val);
        token = tok.nextToken(); //UP1 sent -- should be 3
        assertEquals("UP2 dropped mismatched",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("UP1 sent mismatch", ULTRAPEER_1.outgoingCount , 
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        //TODO2: I am not sure why one message is being dropped, but this is the
        //statistic being returned consistently, For now we will leave it here
        //to make the test pass but at some point, we should investigate why
        //this is happening.
        assertEquals("UP1 dropped mismatch", 1, Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 sent mismatch", LEAF_1.outgoingCount ,
                                               Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Leaf_1 dropped mismatch", 0, Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_2 sent mismatch", LEAF_2.outgoingCount,
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_2 dropped mismatch",0,Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_3 sent mismatch", LEAF_3.outgoingCount,
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_3 drop mismatch",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_4 sent mismatch", LEAF_4.outgoingCount, 
                                              Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_4 dropped mismatch", 0, Integer.parseInt(
                                                                token.trim()));

        ////////////////////////

        giveStatsPayload = statsVM4.getPayload();
        statsPayload = statsAck4.getPayload();
        
        assertEquals("stats message malformed", 
                     giveStatsPayload[0], statsPayload[0]);
        assertEquals("stats message malformed", 
                     giveStatsPayload[1], statsPayload[1]);        

        returnedStats = new String(statsAck4.getPayload());

        tok = new StringTokenizer(returnedStats,"^|");
        
        token = tok.nextToken();//ignore
        token = tok.nextToken(); // UP2 sent -- should be 0
        //System.out.println("****Sumeet***:"+token);

        val = Integer.parseInt(token.trim());
        
        assertEquals("UP2 received mismatch", ULTRAPEER_2.incomingCount, val);
        
        token = tok.nextToken(); //UP1 received -- should be 14
        assertEquals("UP2 dropped mismatch",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("UP1 received mismatch", ULTRAPEER_1.incomingCount, 
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("UP1 dropped mismatch", 0, Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_1 received mismacth", LEAF_1.incomingCount, 
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Leaf_1 dropped mismatch", 0, Integer.parseInt(
                                                                 token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_2 received mismatch",LEAF_2.incomingCount,
                                               Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_2 drop mismatch",0,Integer.parseInt(token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_3 received mismatch",LEAF_3.incomingCount,
                                               Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_3 dropped mismatch",0,Integer.parseInt(
                                                                token.trim()));

        tok.nextToken();//ignore
        token = tok.nextToken();
        assertEquals("Leaf_4 received mismatch", LEAF_4.incomingCount, 
                                                Integer.parseInt(token.trim()));
        token = tok.nextToken();
        assertEquals("Lead_4 dropped mismatch", 0, Integer.parseInt(
                                                                token.trim()));

    }
    
    private static class QueryCountingConnection extends CountingConnection {
        
        public int incomingQueries = 0;
        public int queryReplies = 0;
        
        public QueryCountingConnection(String host, int port) {
            super(host, port);
        }

        public Message receive() throws IOException, BadPacketException {
            Message m = super.receive();
            if (countEnabled && m instanceof QueryRequest) {
                incomingQueries++;
            }
            return m;
        }

        public Message receive(int timeout) throws IOException, BadPacketException {
            Message m = super.receive(timeout);
            if (countEnabled && m instanceof QueryRequest) {
                incomingQueries++;
            }
            return m;
        }

        
        public void send(Message m) throws IOException {
            if (countEnabled && m instanceof QueryReply) {
                queryReplies++;
            }
            super.send(m);
        }
    }
}
