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
 * The most important end-to-end message routing test.  Checks whether
 * ultrapeers handle query routing, normal routing, routing of marked pongs,
 * etc.  The test is structured with one Ultrapeer connected to two other
 * Ultrapeers as well as to a leaf.  The leaves and two Ultrapeers pass
 * varios messages to each other, and the tests verify that the correct messages
 * are received by the other nodes.  This is shown in the diagram below:
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 */
public final class UltrapeerRoutingTest extends BaseTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static final int PORT = 6667;

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 1800;

	/**
	 * The default TTL to use for request messages.
	 */
	private final static byte TTL = 7;


	/**
	 * The "soft max" TTL used by LimeWire's message routing -- hops + ttl 
	 * greater than this value have their TTLs automatically reduced.
     *
     * NOTE: This is now based on the X-Max-TTL header for these tests, 
     * since our connectin pass this headers, and this is what the
     * per-connection soft max is based on.
	 */    
    private static final byte SOFT_MAX = (byte)4; // X-Max-TTL+1


	/**
	 * The TTL of the initial "probe" queries that the Ultrapeer uses to
	 * determine how widely distributed a file is.
	 */
	private static final byte PROBE_QUERY_TTL = 2;

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

    public UltrapeerRoutingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UltrapeerRoutingTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private void buildConnections() {
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
        PingPongSettings.PINGS_ACTIVE.setValue(false);


        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());

		ROUTER_SERVICE.start();
		ROUTER_SERVICE.clearHostCatcher();
		ROUTER_SERVICE.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());
	}

	public void tearDown() throws Exception {
        drainAll();
		sleep();
		LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
		ROUTER_SERVICE.disconnect();
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
        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize();
        
        //3. routed leaf, with route table for "test"
        LEAF.initialize();

		assertTrue("ULTRAPEER_2 should be connected", ULTRAPEER_2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", ULTRAPEER_1.isOpen());
		assertTrue("LEAF should be connected", LEAF.isOpen());

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


		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		//sleep();
		drainAll();
		sleep();
        drainAll();
    }


	/**
	 * Tests broadcasting of queries from ULTRAPEER_2.
	 */
    public void testBroadcastFromUltrapeer2() throws Exception  {
		QueryRequest qr = QueryRequest.createQuery("crap");
        ULTRAPEER_2.writer().simpleWrite(qr);
        ULTRAPEER_2.writer().flush();
              
        Message m = ULTRAPEER_1.receive(TIMEOUT);
		assertInstanceof("expected a query request", QueryRequest.class, m);
        
        QueryRequest qr2 = (QueryRequest)m;
        assertEquals("unexpected query", "crap", qr2.getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

		assertTrue("should not have drained leaf successfully", 
				   !drain(LEAF));
    }
    
    /**
     * Tests a query sent from the leaf.  The query should be received by both
     * Ultrapeer connections -- the one connected to the leaf, as well as the
     * other one.
     */
    public void testBroadcastFromLeaf() throws Exception {
        //drain(LEAF);
        //1. Check that query broadcasted to ULTRAPEER_2 and ultrapeer
        QueryRequest qr = QueryRequest.createQuery("crap");
        LEAF.writer().simpleWrite(qr);
        LEAF.writer().flush();
        
        //Message m;
        Message m = ULTRAPEER_2.receive(TIMEOUT);
        assertQuery(m);
        assertEquals("unexpected query", "crap", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops());
        
        // since it's coming from the leaf, the intervening Ultrapeer 
        // sends a dynamic, probe query -- so check for that TTL
        assertEquals("unexpected TTL",  PROBE_QUERY_TTL, m.getTTL());
        
        m = ULTRAPEER_1.receive(TIMEOUT);
        assertQuery(m);
        assertEquals("unexpected query", "crap", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        
        // since it's coming from the leaf, the intervening Ultrapeer 
        // sends a dynamic, probe query -- so check for that TTL
        assertEquals("unexpected TTL",  PROBE_QUERY_TTL, m.getTTL());
        
        //2. Check that replies are routed back.
        drain(LEAF);
        drain(ULTRAPEER_2);
        Response response1=new Response(0L, 0L, "response1.txt");
        byte[] clientGUID = GUID.makeGuid();
        QueryReply reply1=new QueryReply(qr.getGUID(),
                                         (byte)2,
                                         6346,
                                         new byte[4],
                                         56,
                                         new Response[] {response1},
                                         clientGUID, false);
        ULTRAPEER_2.writer().simpleWrite(reply1);
        ULTRAPEER_2.writer().flush();
        
        Message msg = LEAF.receive(TIMEOUT);
        assertInstanceof("should be a query hit", QueryReply.class, msg);
        QueryReply replyRead=(QueryReply)msg;
        assertTrue("guids should be equal", 
                   Arrays.equals(clientGUID, replyRead.getClientGUID()));
        
        drain(LEAF);
        Response response2 = new Response(0l, 0l, "response2.txt");
        byte[] guid2 = GUID.makeGuid();
        QueryReply reply2 = 
            new QueryReply(qr.getGUID(), (byte)2, 6346, new byte[4],
                           56, new Response[] {response1}, guid2, false);
        ULTRAPEER_1.writer().simpleWrite(reply2);
        ULTRAPEER_1.writer().flush();
        
        m = LEAF.receive(TIMEOUT);
       
        assertInstanceof("message not a QueryReply", QueryReply.class, m);
        replyRead = (QueryReply)m;
        assertTrue("guids should be equal", 
                   Arrays.equals(guid2, replyRead.getClientGUID()));

        //3. Check that pushes are routed (not broadcast)
        drain(ULTRAPEER_2);
        drain(ULTRAPEER_1);

        PushRequest push1 = 
            new PushRequest(GUID.makeGuid(), (byte)2, clientGUID, 0, 
                            new byte[4], 6346);
        LEAF.writer().simpleWrite(push1);
        LEAF.writer().flush();
        m = ULTRAPEER_2.receive(TIMEOUT);

        assertInstanceof("message not a PushRequest", PushRequest.class, m);
        PushRequest pushRead = (PushRequest)m;
        assertEquals("unexpected push index", 0, pushRead.getIndex());
        assertTrue("should not have drained ULTRAPEER_1 successfully", 
                   !drain(ULTRAPEER_1));
        
        // check that pushes with unmatching client guids are not forwarded
        PushRequest push2 = 
            new PushRequest(GUID.makeGuid(),(byte)2, guid2, 1, 
                            new byte[4], 6346);
        LEAF.writer().simpleWrite(push2);
        LEAF.writer().flush();
        m = ULTRAPEER_1.receive(TIMEOUT);
        assertInstanceof("message not a PushRequest", PushRequest.class, m);
        pushRead=(PushRequest)m;
        assertEquals("unexpected push index", 1,pushRead.getIndex());
        assertTrue("should not have drained ultrapeer successfully", 
                   !drain(ULTRAPEER_2));   
        
        // Check that queries can re-route push routes
        drain(LEAF);
        drain(ULTRAPEER_2);
        ULTRAPEER_1.writer().simpleWrite(reply1);
        ULTRAPEER_1.writer().flush();

        m = LEAF.receive(TIMEOUT);
        assertInstanceof("message not a QueryReply", QueryReply.class, m);
        replyRead = (QueryReply)m; 
        assertTrue("unexpected GUID", 
                   Arrays.equals(clientGUID, replyRead.getClientGUID()));
        PushRequest push3 =
            new PushRequest(GUID.makeGuid(), (byte)2, clientGUID, 3, 
                new byte[4], 6346);
        LEAF.writer().simpleWrite(push3);
        LEAF.writer().flush();

        m = ULTRAPEER_1.receive(TIMEOUT);
        assertInstanceof("message not a PushRequest", PushRequest.class, m);
        pushRead = (PushRequest)m;
        assertEquals("unexpected push index", 3, pushRead.getIndex());
        assertTrue("should not have drained ultrapeer successfully", 
                   !drain(ULTRAPEER_2));   

    }


	/**
	 * Tests URN queries from the leaf.
	 */
    /*
	public void testUrnQueryToLeaf() throws Exception {
		QueryRequest qr = 
			QueryRequest.createQuery(HugeTestUtils.UNIQUE_SHA1);

		ULTRAPEER_2.send(qr);
		ULTRAPEER_2.flush();
		
		Message m = LEAF.receive(TIMEOUT);
		assertQuery(m);

		QueryRequest qrRead = (QueryRequest)m;
        assertEquals("unexpected query", "\\", ((QueryRequest)m).getQuery());
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));

		m = ULTRAPEER_1.receive(TIMEOUT);
		assertQuery(m);

        qrRead = (QueryRequest)m;
        assertEquals("unexpected query", "\\", ((QueryRequest)m).getQuery());
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));
        
	}
    */

    /**
     * Test to make sure that queries with one more hop to go are
     * properly routed when the last hop is an Ultrapeer.
     */
    public void testLastHopQueryRouting() throws Exception {
        // first make sure it gets through on NOT last hop...
        QueryRequest qr = QueryRequest.createQuery("junkie junk", (byte)3);
        
		ULTRAPEER_2.writer().simpleWrite(qr);
		ULTRAPEER_2.writer().flush();

		Message m = ULTRAPEER_1.receive(TIMEOUT);
		assertQuery(m);

		QueryRequest qrRead = (QueryRequest)m;
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));
        
        // now make sure it doesn't get through on last hop
        qr = QueryRequest.createQuery("junkie junk", (byte)2);
        
		ULTRAPEER_2.writer().simpleWrite(qr);
		ULTRAPEER_2.writer().flush();

        assertTrue(!drain(ULTRAPEER_1));

        // ok, now make sure a query DOES get through on the last hop
        qr = QueryRequest.createQuery("leehsu", (byte)2);        
        testLastHop(ULTRAPEER_2, ULTRAPEER_1, qr);

        //qr = QueryRequest.createQuery("susheel", (byte)2);        
        //testLastHop(ULTRAPEER_2, ULTRAPEER_1, qr);
        //testLastHop(ULTRAPEER_1, ULTRAPEER_2, qr);

        //qr = QueryRequest.createQuery("susheel", (byte)2);
        //testLastHop(ULTRAPEER_2, ULTRAPEER_1, qr);
        //testLastHop(ULTRAPEER_1, ULTRAPEER_2, qr);
        

        // old test
// 		ULTRAPEER_2.send(qr);
// 		ULTRAPEER_2.flush();

// 		m = ULTRAPEER_1.receive(TIMEOUT);
// 		assertQuery(m);

// 		qrRead = (QueryRequest)m;
// 		assertTrue("guids should be equal", 
// 				   Arrays.equals(qr.getGUID(), qrRead.getGUID())); 
        
        // end old test


        // ok, now make sure a query DOES get through on the last hop
        // when the content is on the leaf
        /*
        qr = QueryRequest.createQuery("susheel", (byte)2);
        
		ULTRAPEER_2.send(qr);
		ULTRAPEER_2.flush();

		m = ULTRAPEER_1.receive(TIMEOUT);
		assertQuery(m);

		qrRead = (QueryRequest)m;
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));  
        */
    }

    /**
     * Helper method that sends a query to that would be routed on the
     * last hop from the sender to the receiver.
     *
     * @param sender the <tt>Connection</tt>
     */
    private static void testLastHop(Connection sender, 
                                    Connection receiver, 
                                    QueryRequest qr) throws Exception {
		sender.writer().simpleWrite(qr);
		sender.writer().flush();

		Message m = receiver.receive(TIMEOUT);
		assertQuery(m);

		QueryRequest qrRead = (QueryRequest)m;
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));         
    }

	/**
	 * Tests to make sure that queries by URN are correctly forwarded
	 * only to those nodes that should receive them.  In this case, for example,
	 * the leaf and Ultrapeer routing tables have no URN data, so the leaf
	 * should not receive the queries, and the Ultrapeer should not receive
	 * them at the last hop.
	 */
    /*
	public void testThatURNOnlyQueryDoesNotGetImproperlyForwarded() 
		throws Exception {
		QueryRequest qr = 
			QueryRequest.createRequery(HugeTestUtils.SHA1);

		ULTRAPEER_1.send(qr);
		ULTRAPEER_1.flush();
		
		Message m = ULTRAPEER_2.receive(TIMEOUT);
		assertQuery(m);

		QueryRequest qrRead = (QueryRequest)m;
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));
		
		assertTrue("leaf should not have received the query", !drain(LEAF));
		

		// now test to make sure that query routing on the last hop
		// is working correctly for URN queries
		qr = QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)2);

		ULTRAPEER_1.send(qr);
		ULTRAPEER_1.flush();

		assertTrue("ultrapeer should not have received the query", 
				   !drain(ULTRAPEER_2));

	}
    */

	/**
	 * Tests URN queries from the leaf.
	 */
    /*
	public void testUrnQueryFromLeaf() throws Exception {
		QueryRequest qr = 
			QueryRequest.createRequery(HugeTestUtils.SHA1);

		LEAF.send(qr);
		LEAF.flush();
		
		Message m = ULTRAPEER_1.receive(TIMEOUT);
		assertQuery(m);

		QueryRequest qrRead = (QueryRequest)m;
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));

		m = ULTRAPEER_2.receive(TIMEOUT);
		assertQuery(m);

		qrRead = (QueryRequest)m;
		assertTrue("guids should be equal", 
				   Arrays.equals(qr.getGUID(), qrRead.getGUID()));				
	}
    */


	/**
	 * Tests the broadcasting of queries from ultrapeer 2 to the leaf.
	 */
    public void testBroadcastFromUltrapeer2ToLeaf() throws Exception {

        QueryRequest qr = QueryRequest.createQuery("test");
        ULTRAPEER_2.writer().simpleWrite(qr);
        ULTRAPEER_2.writer().flush();
              
        Message m = ULTRAPEER_1.receive(TIMEOUT);
		assertInstanceof("expected a query request", QueryRequest.class, m);
        assertEquals("unexpected query", "test", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

        m = LEAF.receive(TIMEOUT);
		assertInstanceof("expected a query request", QueryRequest.class, m);
        assertEquals("unexpected query", "test", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());
    }

	/**
	 * Tests broadcasting of queries from the Ultrapeer to other hosts.
	 * In particular, this tests to make sure that the leaf correctly
	 * receives the query.
	 */
    public void testBroadcastFromUltrapeerToBoth() throws Exception {
        QueryRequest qr= QueryRequest.createQuery("susheel test");
        ULTRAPEER_1.writer().simpleWrite(qr);
        ULTRAPEER_1.writer().flush();
              
        Message m=ULTRAPEER_2.receive(TIMEOUT);
		assertInstanceof("expected a query request", QueryRequest.class, m);
        assertEquals("susheel test", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

        m=LEAF.receive(TIMEOUT);
		assertInstanceof("expected a query request", QueryRequest.class, m);
		assertEquals("unexpected query string", "susheel test", 
					 ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
		assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());
    }

	/**
	 * Tests broadcasting pings between the various hosts.  In particular,
	 * this tests to make sure that leaves do not receive ping broadcasts.
	 */
    /*
    public void testPingBroadcast() throws Exception {
        //Send ping
        Message m=new PingRequest((byte)7);
        ULTRAPEER_1.send(m);
        ULTRAPEER_1.flush();
              
        m=ULTRAPEER_2.receive(TIMEOUT);
        assertInstanceof("message should be a ping request", PingRequest.class, m);
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
		assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

		
		assertTrue("Leaf should not have received message", !drain(LEAF));

        //Send reply
        drain(ULTRAPEER_1);        
        PingReply pong = PingReply.create(m.getGUID(), (byte)7, 6344, new byte[4]);

        ULTRAPEER_2.send(pong);
        ULTRAPEER_2.flush();
        for (int i=0; i<10; i++) {
            PingReply pongRead=(PingReply)ULTRAPEER_1.receive(TIMEOUT);
            if (pongRead.getPort()==pong.getPort())
                return;
        }
        fail("Pong wasn't routed");
    }
    */

	/**
	 * Tests the broadcasting of big pings -- pings that include GGEP extensions,
	 * and so have a payload -- between the various hosts.
	 */
    /*
    public void testBigPingBroadcast() throws Exception {
        //1a. Send big ping (not GGEP...which should be ok)
        byte[] payload= new byte[16];
        byte c = 65; //'A'
        for(int i=0;i<16;i++, c++)
            payload[i] = c;
        
        Message m=new PingRequest(GUID.makeGuid(), (byte)7, (byte)0,payload);
        LEAF.send(m);
        LEAF.flush();
            
        //1b. Make sure ultrapeer gets it with payload.
        m=ULTRAPEER_1.receive(TIMEOUT);
        PingRequest ping = null;
        try{
            ping = (PingRequest)m;
        }catch(ClassCastException cce){
            fail("Big ping not created properly on ULTRAPEER_2 client", cce);
        }

        assertEquals("unexpected hops", (byte)1, m.getHops()); 
		assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());
		assertEquals("unexpected message length", 16, m.getLength());
        //assertTrue(m.getLength()==16);
        //lets make sure the payload got there OK
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try{
            ping.write(stream);
        }catch(IOException ioe){
            fail("error while writing payload in ULTRAPEER_2 client", ioe);
        }
        byte[] b = stream.toByteArray();
        //get rid of bytes 0-22(inclusive) ie the header

        String out = new String(b,23,b.length-23);

        assertEquals("wrong payload in ULTRAPEER_2 client",
            "ABCDEFGHIJKLMNOP", out);
        
        //1c. Make sure ULTRAPEER_2 also gets it with payload, as ULTRAPEER_2 is now also
		// an ultrapeer
        m=ULTRAPEER_2.receive(TIMEOUT);
        ping = null;
        try{
            ping = (PingRequest)m;
        }catch(ClassCastException cce){
            fail("Big ping not created properly on ULTRAPEER_2 client", cce);
        }
        assertEquals("unexpected hops", (byte)1, m.getHops());
		assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());
        //assertEquals("unexpected message length", 0, m.getLength());
		assertEquals("unexpected message length", 16, m.getLength());


        //2a. Send reply from ultrapeer
        //create payload for big pong
        byte[] payload2 = new byte[14+2];
        //add the port
        payload2[0] = 0x0F;
        payload2[1] = 0x00;//port 

        payload2[2] = 0x10;
        payload2[3] = 0x10;
        payload2[4] = 0x10;
        payload2[5] = 0x10;//ip = 16.16.16.16

        payload2[6] = 0x0F;//
        payload2[7] = 0x00;//
        payload2[8] = 0x00;//
        payload2[9] = 0x00;//15 files shared

        payload2[10] = 0x0F;//
        payload2[11] = 0x00;//
        payload2[12] = 0x00;//
        payload2[13] = 0x00;//15 KB
        //OK Now for the big pong part
        payload2[14] = (byte) 65;
        payload2[15] = (byte) 66;

        drain(LEAF);       

        PingReply pong = 
            PingReply.createFromNetwork(m.getGUID(), (byte)7, (byte)0, payload2);
        ULTRAPEER_1.send(pong);
        ULTRAPEER_1.flush();

        //2b. Make sure leaf reads it.
        PingReply ourPong = null;
        for (int i=0; i<10; i++) {
            PingReply pongRead=(PingReply)LEAF.receive(TIMEOUT);
            if (pongRead.getPort()==pong.getPort()){
                ourPong = pongRead;
                break;
            }
        }
        assertNotNull("Pong wasn't routed", ourPong);
        //Lets check that the pong came back in good shape
        assertEquals("wrong port", 15, ourPong.getPort());
        String ip = ourPong.getIP();
        assertEquals("wrong IP", "16.16.16.16", ip);
        assertEquals("wrong files", 15, ourPong.getFiles());
        assertEquals("Wrong share size", 15, ourPong.getKbytes());
        stream = new ByteArrayOutputStream();
        try{
            ourPong.write(stream);
        }catch(IOException ioe){
            fail("problem with writing out big pong", ioe);
        }
        byte[] op = stream.toByteArray();
        byte[] big = new byte[2];
        big[0] = op[op.length-2];
        big[1] = op[op.length-1];
        out = "";//reset
        out = new String(big);
        assertEquals("Big part of pong lost", "AB", out);
    }
    */



	/**
	 * Tests to make sure that pongs that had no entry in the routing
	 * tables (that had no corresponding ping) are not forwarded.
	 */
    public void testMisroutedPong() throws Exception {
        Message m = 
            PingReply.createExternal(GUID.makeGuid(), (byte)6, 7399, 
                                     new byte[4], false);

        ULTRAPEER_1.writer().simpleWrite(m);
        ULTRAPEER_1.writer().flush();
              
		assertTrue("should not have drained ultrapeer successfully", 
				   !drain(ULTRAPEER_2));
		assertTrue("should not have drained leaf successfully", 
				   !drain(LEAF));
    }

	/**
	 * Tests that Ultrapeer pongs are correctly sent to leaves to
	 * provide them with distributed host data.
	 */
    public void testUltrapeerPong() throws Exception {
        byte[] guid=GUID.makeGuid();
        byte[] ip={(byte)18, (byte)239, (byte)0, (byte)143};
        Message m = PingReply.createExternal(guid, (byte)7, 7399, ip, true);
        ULTRAPEER_1.writer().simpleWrite(m);
        ULTRAPEER_1.writer().flush();
              
        m=LEAF.receive(TIMEOUT);
        assertInstanceof("expected a pong", PingReply.class, m);
        assertEquals("unexpected port", 7399, ((PingReply)m).getPort());        

		assertTrue("should not have drained ultrapeer successfully", 
				   !drain(ULTRAPEER_2));
    }


	/**
	 * Tests that duplicate queries are not forwarded if the connection that
	 * originated the connection is dropped.
	 */
    public void testDropAndDuplicate() throws Exception {
        //Send query request from leaf, received by ultrapeer (and ULTRAPEER_2)
        QueryRequest qr = QueryRequest.createQuery("crap");
        LEAF.writer().simpleWrite(qr);
        LEAF.writer().flush();
        
        Message m=ULTRAPEER_1.receive(TIMEOUT);
        assertInstanceof("expected a QueryRequest", QueryRequest.class, m);
        assertEquals("unexpected query", "crap", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 

		// since it's coming from the leaf, the intervening Ultrapeer 
		// sends a dynamic, probe query -- so check for that TTL
		assertEquals("unexpected TTL", PROBE_QUERY_TTL, m.getTTL());

        //After closing leaf (give it some time to clean up), make sure
        //duplicate query is dropped.
        drain(ULTRAPEER_1);
        LEAF.close();
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        ULTRAPEER_2.writer().simpleWrite(qr);
        ULTRAPEER_2.writer().flush();

		assertTrue("should not have drained ultrapeer successfully", 
				   !drain(ULTRAPEER_1));   
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
}
