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
public class UltrapeerRoutingTest extends com.limegroup.gnutella.util.BaseTestCase {

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
        return new TestSuite(UltrapeerRoutingTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private void buildConnections() {
	    LEAF =
			new Connection("localhost", PORT, 
						   new ClientProperties("localhost"),
						   new EmptyResponder()
						   );
        
        ULTRAPEER_1 = 
			new Connection("localhost", PORT,
						   new SupernodeProperties("localhost"),
						   new EmptyResponder()
						   );

        ULTRAPEER_2 = 
			new Connection("localhost", PORT,
						   new SupernodeProperties("localhost"),
						   new EmptyResponder()
						   );
    }

	public void setUp() throws Exception {
		AbstractSettings.revertToDefault();
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
		ConnectionSettings.KEEP_ALIVE.setValue(3);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);


        assertEquals("unexpected port", PORT, 
					 SettingsManager.instance().getPort());

		ROUTER_SERVICE.start();
		ROUTER_SERVICE.clearHostCatcher();
		ROUTER_SERVICE.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 SettingsManager.instance().getPort());
	}

	public void tearDown() throws Exception {
		ROUTER_SERVICE.disconnect();
		sleep();
		LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
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
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("test");
        qrt.add("susheel");
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
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
	 * Tests to make sure that queries by URN are correctly forwarded
	 * only to those nodes that should receive them.  In this case, for example,
	 * the leaf and Ultrapeer routing tables have no URN data, so the leaf
	 * should not receive the queries, and the Ultrapeer should not receive
	 * them at the last hop.
	 */
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

	/**
	 * Tests URN queries from the leaf.
	 */
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
		

		// now test to make sure that query routing on the last hop
		// is working correctly for URN queries
		qr = QueryRequest.createRequery(HugeTestUtils.SHA1, (byte)2);

		LEAF.send(qr);
		LEAF.flush();

		assertTrue("ultrapeer2 should not have received the query", 
				   !drainQuery(ULTRAPEER_2));

		assertTrue("ultrapeer1 should not have received the query", 
				   !drainQuery(ULTRAPEER_1));
		
	}

// 	public void testUrnQueryBetweenUltrapeers() throws Exception {
//         urnTest(ULTRAPEER_1, ULTRAPEER_2, LEAF);
// 	}

// 	public void testUrnQueryBetweenUltrapeers2() throws Exception {
//         urnTest(ULTRAPEER_2, ULTRAPEER_1, LEAF);
// 	}


	/**
	 * Tests to make sure that the passing of query routing tables between
	 * Ultrapeers is working correctly.
	 */
	public void testUltrapeerQueryRouting() throws Exception {
		QueryRequest qr = QueryRequest.createQuery("crap", (byte)2);

		
		ULTRAPEER_1.send(qr);
		ULTRAPEER_1.flush();

		assertTrue("Ultrapeer 2 should not have received the query",
				   !drain(ULTRAPEER_2));
	}

	/**
	 * Tests a query sent from the leaf.  The query should be received by both
	 * Ultrapeer connections -- the one connected to the leaf, as well as the
	 * other one.
	 */
    public void testBroadcastFromLeaf() throws Exception {
		
		//1. Check that query broadcasted to ULTRAPEER_2 and ultrapeer
		QueryRequest qr = QueryRequest.createQuery("crap");
		LEAF.send(qr);
		LEAF.flush();
		
		//Message m;
		Message m = ULTRAPEER_2.receive(TIMEOUT);
		assertQuery(m);
		assertEquals("unexpected query", "crap", ((QueryRequest)m).getQuery());
		assertEquals("unexpected hops", (byte)1, m.getHops()); //used to be not decremented
		
		// since it's coming from the leaf, the intervening Ultrapeer 
		// sends a dynamic, probe query -- so check for that TTL
		assertEquals("unexpected TTL",  PROBE_QUERY_TTL, m.getTTL());
		
		m = ULTRAPEER_1.receive(TIMEOUT);
		assertQuery(m);
		assertEquals("unexpected query", "crap", ((QueryRequest)m).getQuery());
		assertEquals("unexpected hops", (byte)1, m.getHops()); //used to be not decremented
		
		// since it's coming from the leaf, the intervening Ultrapeer 
		// sends a dynamic, probe query -- so check for that TTL
		assertEquals("unexpected TTL",  PROBE_QUERY_TTL, m.getTTL());
		
		//2. Check that replies are routed back.
		drain(LEAF);
		Response response1=new Response(0L, 0L, "response1.txt");
		byte[] guid1=GUID.makeGuid();
		QueryReply reply1=new QueryReply(qr.getGUID(),
										 (byte)2,
										 6346,
										 new byte[4],
										 56,
										 new Response[] {response1},
										 guid1);
		ULTRAPEER_2.send(reply1);
		ULTRAPEER_2.flush();
		
		QueryReply replyRead=(QueryReply)LEAF.receive(TIMEOUT);
		assertTrue("guids should be equal", 
				   Arrays.equals(guid1, replyRead.getClientGUID()));
		
		drain(LEAF);
		Response response2 = new Response(0l, 0l, "response2.txt");
		byte[] guid2 = GUID.makeGuid();
		QueryReply reply2 = 
			new QueryReply(qr.getGUID(), (byte)2, 6346, new byte[4],
						   56, new Response[] {response1}, guid2);
		ULTRAPEER_1.send(reply2);
		ULTRAPEER_1.flush();
		
		m = LEAF.receive(TIMEOUT);
	   
		assertTrue("message not a QueryReply", m instanceof QueryReply);
		replyRead = (QueryReply)m;
		assertTrue("guids should be equal", 
				   Arrays.equals(guid2, replyRead.getClientGUID()));
		
		//3. Check that pushes are routed (not broadcast)
		drain(ULTRAPEER_2);
		drain(ULTRAPEER_1);
		PushRequest push1=new PushRequest(GUID.makeGuid(),
										  (byte)2,
										  guid1,
										  0, new byte[4],
										  6346);
		LEAF.send(push1);
		LEAF.flush();
		m = ULTRAPEER_2.receive(TIMEOUT);
		assertTrue("message not a PushRequest", m instanceof PushRequest);
		PushRequest pushRead = (PushRequest)m;
		assertEquals("unexpected push index", 0, pushRead.getIndex());
		assertTrue("should not have drained ULTRAPEER_1 successfully", 
				   !drain(ULTRAPEER_1));
		
		PushRequest push2=new PushRequest(GUID.makeGuid(),
										  (byte)2,
										  guid2,
										  1, new byte[4],
										  6346);
		LEAF.send(push2);
		LEAF.flush();
		m = ULTRAPEER_1.receive(TIMEOUT);
		assertTrue("message not a PushRequest", m instanceof PushRequest);
		pushRead=(PushRequest)m;
		assertEquals("unexpected push index", 1,pushRead.getIndex());
		assertTrue("should not have drained ultrapeer successfully", 
				   !drain(ULTRAPEER_2));   
		
		//4. Check that queries can re-route push routes
		drain(LEAF);
		drain(ULTRAPEER_2);
		ULTRAPEER_1.send(reply1);
		ULTRAPEER_1.flush();

		m = LEAF.receive(TIMEOUT);
		assertTrue("message not a QueryReply", m instanceof QueryReply);
		replyRead = (QueryReply)m; 
		assertTrue("unexpected GUID", 
				   Arrays.equals(guid1, replyRead.getClientGUID()));
		PushRequest push3 =
			new PushRequest(GUID.makeGuid(), (byte)2, guid1, 3, new byte[4], 6346);
		LEAF.send(push3);
		LEAF.flush();

		m = ULTRAPEER_1.receive(TIMEOUT);
		assertTrue("message not a PushRequest", m instanceof PushRequest);
		pushRead = (PushRequest)m;
		assertEquals("unexpected push index", 3, pushRead.getIndex());
		assertTrue("should not have drained ultrapeer successfully", 
				   !drain(ULTRAPEER_2));   

    }


	/**
	 * Tests broadcasting of queries from ULTRAPEER_2.
	 */
    public void testBroadcastFromUltrapeer2() 
             throws Exception  {
        //System.out.println("-Testing normal broadcast from ULTRAPEER_2 connnection"
        //                   +", no forwarding to leaf");
        //drain(ULTRAPEER_1);
        //drain(LEAF);

        //QueryRequest qr = new QueryRequest(TTL, 0, "crap", false);
		QueryRequest qr = QueryRequest.createQuery("crap");
        ULTRAPEER_2.send(qr);
        ULTRAPEER_2.flush();
              
        Message m = ULTRAPEER_1.receive(TIMEOUT);
		assertTrue("expected a query request", m instanceof QueryRequest);
        assertEquals("unexpected query", "crap", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

		assertTrue("should not have drained leaf successfully", 
				   !drain(LEAF));
    }

	/**
	 * Tests the broadcasting of queries from ultrapeer 2 to the leaf.
	 */
    public void testBroadcastFromUltrapeer2ToLeaf() 
		throws Exception {
        //System.out.println("-Testing normal broadcast from ULTRAPEER_2 connnection"
        //                   +", with forwarding to leaf");
        //drain(ULTRAPEER_1);
        //drain(LEAF);

        //QueryRequest qr = new QueryRequest(TTL, 0, "test", false);
        QueryRequest qr = QueryRequest.createQuery("test");
        ULTRAPEER_2.send(qr);
        ULTRAPEER_2.flush();
              
        Message m = ULTRAPEER_1.receive(TIMEOUT);
		assertTrue("expected a query request", m instanceof QueryRequest);
        assertEquals("unexpected query", "test", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

        m = LEAF.receive(TIMEOUT);
		assertTrue("expected a query request", m instanceof QueryRequest);
        assertEquals("unexpected query", "test", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());
    }

	/**
	 * Tests broadcasting of queries from the Ultrapeer to other hosts.
	 * In particular, this tests to make sure that the leaf correctly
	 * receives the query.
	 */
    public void testBroadcastFromUltrapeerToBoth() 
             throws Exception {
        //System.out.println("-Testing normal broadcast from ULTRAPEER_2 connnection"
        //                   +", with forwarding to leaf");
        //drain(LEAF);
        //drain(ULTRAPEER_2);

        //QueryRequest qr=new QueryRequest(TTL, 0, "susheel test", false);
        QueryRequest qr= QueryRequest.createQuery("susheel test");
        ULTRAPEER_1.send(qr);
        ULTRAPEER_1.flush();
              
        Message m=ULTRAPEER_2.receive(TIMEOUT);
		assertTrue("expected a query request", m instanceof QueryRequest);
        assertEquals("susheel test", ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

        m=LEAF.receive(TIMEOUT);
		assertTrue("expected a query request", m instanceof QueryRequest);
		assertEquals("unexpected query string", "susheel test", 
					 ((QueryRequest)m).getQuery());
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
		assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());
    }

	/**
	 * Tests broadcasting pings between the various hosts.  In particular,
	 * this tests to make sure that leaves do not receive ping broadcasts.
	 */
    public void testPingBroadcast() throws Exception {
        //System.out.println("-Testing ping broadcast from ULTRAPEER_2 connnection"
        //                   +", no forwarding to leaf, with reply");
        //drain(ULTRAPEER_2);
        //drain(LEAF);

        //Send ping
        Message m=new PingRequest((byte)7);
        ULTRAPEER_1.send(m);
        ULTRAPEER_1.flush();
              
        m=ULTRAPEER_2.receive(TIMEOUT);
        assertTrue("message should be a ping request", m instanceof PingRequest);
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
		assertEquals("unexpected TTL", (byte)(SOFT_MAX-1), m.getTTL());

		
		assertTrue("Leaf should not have received message", !drain(LEAF));

        //Send reply
        drain(ULTRAPEER_1);        
        PingReply pong = 
			new PingReply(m.getGUID(), (byte)7, 6344, new byte[4], 3, 7);

        ULTRAPEER_2.send(pong);
        ULTRAPEER_2.flush();
        for (int i=0; i<10; i++) {
            PingReply pongRead=(PingReply)ULTRAPEER_1.receive(TIMEOUT);
            if (pongRead.getPort()==pong.getPort())
                return;
        }
        fail("Pong wasn't routed");
    }

	/**
	 * Tests the broadcasting of big pings -- pings that include GGEP extensions,
	 * and so have a payload -- between the various hosts.
	 */
    public void testBigPingBroadcast() 
             throws Exception {
        //System.out.println("-Testing big ping broadcast from leaf connnection"
        //                   +", no payload forwarding to ULTRAPEER_2, with big reply");
        //drain(ULTRAPEER_2);
        //drain(ULTRAPEER_1);

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

        assertTrue("wrong payload in ULTRAPEER_2 client "+out,
                   out.equals("ABCDEFGHIJKLMNOP"));
        
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

        PingReply pong=new PingReply(m.getGUID(),
                                     (byte)7,
                                     (byte)0,
                                     payload2);
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
        assertTrue("Pong wasn't routed", ourPong != null);
        //Lets check that the pong came back in good shape
        assertTrue("wrong port", ourPong.getPort() == 15);
        String ip = ourPong.getIP();
        assertTrue("wrong IP", ip.equals("16.16.16.16"));
        assertTrue("wrong files", ourPong.getFiles() == 15);
        assertTrue("Wrong share size", ourPong.getKbytes() == 15);
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
        assertTrue("Big part of pong lost", out.equals("AB"));
        //System.out.println("Passed");
    }




    public void testMisroutedPong() 
             throws Exception {
        //System.out.println("-Testing misrouted normal pong"
        //                   +", not forwarded to leaf");
        drain(ULTRAPEER_2);
        drain(LEAF);

        Message m=new PingReply(GUID.makeGuid(), 
                                (byte)7, 6399, new byte[4], 
                                0, 0, false);                                
        ULTRAPEER_1.send(m);
        ULTRAPEER_1.flush();
              
		assertTrue("should not have drained ultrapeer successfully", 
				   !drain(ULTRAPEER_2));
		assertTrue("should not have drained leaf successfully", 
				   !drain(LEAF));
    }

    public void testUltrapeerPong() 
             throws Exception {
        //System.out.println("-Testing misrouted ultrapeer pong"
        //                   +", forwarded to leaf");
        drain(ULTRAPEER_2);
        drain(LEAF);

        byte[] guid=GUID.makeGuid();
        byte[] ip={(byte)18, (byte)239, (byte)0, (byte)143};
        Message m=new PingReply(guid, 
                                (byte)7, 6399, ip, 
                                0, 0, true);                                
        ULTRAPEER_1.send(m);
        ULTRAPEER_1.flush();
              
        m=LEAF.receive(TIMEOUT);
        assertTrue("expected a query hit", m instanceof PingReply);
        assertEquals("unexpected port", 6399, ((PingReply)m).getPort());        

		assertTrue("should not have drained ultrapeer successfully", 
				   !drain(ULTRAPEER_2));
    }


    /** This test makes sure that querys with no query string but with
     *  specified urn get through to leaves, etc.
     */ 
	/*
    public void testNullQueryURNRequest() 
        throws Exception {

        // make sure it gets through with all combinations of one sender and two
        // receivers.
        urnTest(ULTRAPEER_1, ULTRAPEER_2, LEAF);
        urnTest(ULTRAPEER_2, LEAF, ULTRAPEER_1);
        urnTest(LEAF, ULTRAPEER_1, ULTRAPEER_2);        
    }
	*/


	/**
	 * Tests a URN query send from the first <tt>Connnection</tt> to the other
	 * two <tt>Connnection</tt>s.
	 *
	 * @param sndr the <tt>Connnection</tt> sending the query
	 * @param rcv1 the first <tt>Connnection</tt> receiving the query
	 * @param rcv2 the second <tt>Connnection</tt> receiving the query
	 */
    private void urnTest(Connection sndr, Connection rcv1,
						 Connection rcv2) 
        throws Exception {
        // make urns...
        Set currUrnSet = new HashSet();
        Set currUrnTypeSet = new HashSet();
        for(int j = 0; j < HugeTestUtils.URNS.length; j++) {
			currUrnSet.add(HugeTestUtils.URNS[j]);
			currUrnTypeSet.add(HugeTestUtils.URNS[j].getUrnType());
        }

        // build the null QR
        GUID guid = new GUID(GUID.makeGuid());
        QueryRequest qr = 
			new QueryRequest(guid.bytes(), TTL, 0, "", "", false,
							 currUrnTypeSet, currUrnSet, false);
        
		//QueryRequest qr = QueryRequest.createRequery(HugeTestUtils.URNS[0]);
        //GUID guid = new GUID(qr.getGUID());

		//Set currUrnSet = qr.getQueryUrns();

        // send the QR - FROM sndr
        sndr.send(qr);
        sndr.flush();

		Message m = rcv1.receive(TIMEOUT);
		assertQuery(m);

        // did recv1 get everything a-ok?
        QueryRequest reqRead=(QueryRequest)m;
        assertTrue("guids should be equal",
				   Arrays.equals(guid.bytes(), reqRead.getGUID()));
        assertNotNull("query urns should not be null", reqRead.getQueryUrns());
        assertEquals("urn sets should be equal",currUrnSet, reqRead.getQueryUrns());

        // did recv2 get everything a-ok?
        m = rcv2.receive(TIMEOUT);
		assertQuery(m);
		reqRead = (QueryRequest)m;
        assertTrue("guids should be equal", Arrays.equals(guid.bytes(), reqRead.getGUID()));
        assertNotNull("query urns should not be null",reqRead.getQueryUrns());
        assertEquals("urn sets should be equal", currUrnSet, reqRead.getQueryUrns());
    }


	/**
	 * Tests that duplicate queries are not forwarded if the connection that
	 * originated the connection is dropped.
	 */
    public void testDropAndDuplicate() 
		throws Exception {
        //System.out.println("-Testing that duplicates are dropped "
        //                   +"when original connection closed");


        //Send query request from leaf, received by ultrapeer (and ULTRAPEER_2)
        //QueryRequest qr=new QueryRequest(TTL, 0, "crap", false);
        QueryRequest qr = QueryRequest.createQuery("crap");
        LEAF.send(qr);
        LEAF.flush();
        
        Message m=ULTRAPEER_1.receive(TIMEOUT);
        assertTrue("expected a QueryRequest", m instanceof QueryRequest);
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
        ULTRAPEER_2.send(qr);
        ULTRAPEER_2.flush();

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
	 * Tries to receive any outstanding queries on c.
	 *
     * @return <tt>true</tt> if this got a message, otherwise <tt>false</tt>
	 */
    private static boolean drainQuery(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                Message m = c.receive(TIMEOUT);
				if(m instanceof QueryRequest) {
					ret = true;
				} else {
					System.out.println(m); 
				}
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
	 * Asserts that the given message is a query, printing out the 
	 * message and failing if it's not.
	 *
	 * @param m the <tt>Message</tt> to check
	 */
	private void assertQuery(Message m) {
		if(m instanceof QueryRequest) return;

		System.out.println(m); 
		assertTrue("message not a QueryRequest", m instanceof QueryRequest);			
	}
}

class LeafProperties extends Properties {
    public LeafProperties() {
        put(ConnectionHandshakeHeaders.USER_AGENT, CommonUtils.getHttpServer());
        put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "False");
        put(ConnectionHandshakeHeaders.GGEP, "0.5");
    }
}

class UltrapeerProperties extends Properties {
    public UltrapeerProperties() {
        put(ConnectionHandshakeHeaders.USER_AGENT, CommonUtils.getHttpServer());
        put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "true");
        put(ConnectionHandshakeHeaders.GGEP, "1.0");  //just for fun
    }
}


class EmptyResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        return new HandshakeResponse(new Properties());
    }
}
