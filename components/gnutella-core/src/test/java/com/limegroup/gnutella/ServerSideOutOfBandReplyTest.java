package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
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
 *
 *  This test should cover the case for leaves too, since there is no difference
 *  between Leaf and UP when it comes to this behavior.
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
	private static final byte SOFT_MAX = 3;

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
        //To keep LimeWire from connecting to the outside network, we filter out
        //all addresses but localhost and 18.239.0.*.  The latter is used in
        //pongs for testing.  TODO: it would be nice to have a way to prevent
        //BootstrapServerManager from adding defaults and connecting.
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
		UltrapeerSettings.MAX_LEAVES.setValue(1);
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
        assertInstanceof("message not a QueryRequest: " + m,
		    QueryRequest.class, m);
	}


    // BEGIN TESTS
    // ------------------------------------------------------

    // tests basic out of band functionality
    // this tests no UDP support - it should just send a UDP reply
    // this test is DEPRECATED - we do not send out-of-band replies unless
    // you are GUESS-capable
    public void notestOutOfBandRequest() throws Exception {
        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("susheel",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER_1.writer().simpleWrite(query);
        ULTRAPEER_1.writer().flush();

        // we should get a reply via UDP
        UDP_ACCESS.setSoTimeout(500);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            UDP_ACCESS.receive(pack);
        }
        catch (IOException bad) {
            fail("Did not get reply", bad);
        }
        InputStream in = new ByteArrayInputStream(pack.getData());
        // as long as we don't get a ClassCastException we are good to go
        QueryReply reply = (QueryReply) Message.read(in);
        assertTrue(Arrays.equals(reply.getGUID(), query.getGUID()));
    }

    // tests basic out of band functionality
    // this tests solicited UDP support - it should participate in ACK exchange
    public void testBasicOutOfBandRequest() throws Exception {
        DatagramPacket pack = null;
        // set up solicited UDP support
        {
            drainAll();
            PingReply pong = 
                PingReply.create(GUID.makeGuid(), (byte) 4,
                                 UDP_ACCESS.getLocalPort(), 
                                 InetAddress.getLocalHost().getAddress(), 
                                 10, 10, true, 900, true);
            ULTRAPEER_1.writer().simpleWrite(pong);
            ULTRAPEER_1.writer().flush();

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
            ULTRAPEER_1.writer().simpleWrite(support);
            ULTRAPEER_1.writer().flush();

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

        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER_1.writer().simpleWrite(query);
        ULTRAPEER_1.writer().flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = Message.read(in);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());

        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()),
                                     reply.getNumResults());
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should get TWO replies, each with one response
        //1)
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = Message.read(in);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());

        //2) null out 'message' so we can get the next reply....
        message = null;
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = Message.read(in);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());

        // make sure that if we send the ACK we don't get another reply - this
        // is current policy but we may want to change it in the future
        baos = new ByteArrayOutputStream();
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = Message.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}


    }

    // makes sure that if we ack back with less results than what the server
    // has we only get back what we asked for.
    public void testPiecemealOutOfBandResults() throws Exception {
        DatagramPacket pack = null;

        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER_1.writer().simpleWrite(query);
        ULTRAPEER_1.writer().flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = Message.read(in);
            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());

        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 1);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should get the reply!
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = Message.read(in);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());

        // make sure that if we send the ACK we don't get another reply - this
        // is current policy but we may want to change it in the future
        baos = new ByteArrayOutputStream();
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = Message.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}
    }

    // makes sure that if we ACK back with a 0 for the number of results we
    // don't get a query reply
    public void testSimpleACKOutOfBandResults() throws Exception {
        DatagramPacket pack = null;

        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER_1.writer().simpleWrite(query);
        ULTRAPEER_1.writer().flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = Message.read(in);
            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());

        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 0);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = Message.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}
    }

    // tests that MessageRouter expires GUIDBundles/Replies in a timely fashion
    // this test requires solicited support
    public void testExpirer() throws Exception {
        drainAll();
        DatagramPacket pack = null;

        // THIS TESTS ASSUMES SOLICITED SUPPORT - set up in a previous test

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("berkeley",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER_2.writer().simpleWrite(query);
        ULTRAPEER_2.writer().flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = Message.read(in);
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(1, reply.getNumResults());

        // WAIT for the expirer to expire the query reply
        Thread.sleep(60 * 1000); // 1 minute - expirer must run twice

        // ok - we should ACK the ReplyNumberVM and NOT get a reply
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 
                                     reply.getNumResults());
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!  keep reading until buffer is empty
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = Message.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(ack.getGUID(), message.getGUID()))));
            }
        }
        catch (IOException expected) {}

        // good - now lets test that if we send a LOT of out-of-band queries,
        // we get a lot of ReplyNumberVMs but at the 251st we don't get a
        // ReplyNumberVM - this test may be fragile because i'm hardcoding
        // MAX_BUFFERED_REPLIES from MessageRouter
        final int MAX_BUFFERED_REPLIES = 15;

        // ok, we need to set MAX_BUFFERED_REPLIES in MessageRouter
        MessageRouter.MAX_BUFFERED_REPLIES = MAX_BUFFERED_REPLIES;

        // send 15 queries
        Random rand = new Random();
        int numReplyNumberVMs = 0;
        for (int i = 0; i < MAX_BUFFERED_REPLIES; i++) {
            query = 
            QueryRequest.createOutOfBandQuery((i%2==0) ? "berkeley" : "susheel",
                                                  InetAddress.getLocalHost().getAddress(),
                                                  UDP_ACCESS.getLocalPort());
            query.hop();

            if (rand.nextInt(2) == 0) {
                ULTRAPEER_2.writer().simpleWrite(query);
                ULTRAPEER_2.writer().flush();
            }
            else {
                ULTRAPEER_1.writer().simpleWrite(query);
                ULTRAPEER_1.writer().flush();
            }

            Thread.sleep(1250);
            // count 15 ReplyNumberVMs
            try {
                while (true) {
                    UDP_ACCESS.setSoTimeout(500);
                    pack = new DatagramPacket(new byte[1000], 1000);
                    UDP_ACCESS.receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    message = Message.read(in);
                    if (message instanceof ReplyNumberVendorMessage)
                        numReplyNumberVMs++;
                }
            }
            catch (IOException expected) {}
        }

        assertEquals("Didn't get all VMs!!", MAX_BUFFERED_REPLIES,
            numReplyNumberVMs);

        // send 2 new queries that shouldn't be ACKed
        for (int i = 0; i < 2; i++) {
            query = 
            QueryRequest.createOutOfBandQuery((i%2==0) ? "berkeley" : "susheel",
                                                  InetAddress.getLocalHost().getAddress(),
                                                  UDP_ACCESS.getLocalPort());
            query.hop();

            ULTRAPEER_1.writer().simpleWrite(query);
            ULTRAPEER_1.writer().flush();
        }

        // count NO ReplyNumberVMs
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = Message.read(in);
                assertNotInstanceof( ReplyNumberVendorMessage.class, message );
            }
        }
        catch (IOException expected) {}
    }

    // make sure that the Ultrapeer discards out-of-band query requests that
    // have an improper address associated with it
    public void testIdentity() throws Exception {
        drainAll();

        byte[] crapIP = {0,0,0,0};
        QueryRequest query = QueryRequest.createOutOfBandQuery("berkeley", 
                                                               crapIP, 6346);
        LEAF.writer().simpleWrite(query);
        LEAF.writer().flush();

        // ultrapeers should NOT get the QR
        assertNull(getFirstQueryRequest(ULTRAPEER_1));
        assertNull(getFirstQueryRequest(ULTRAPEER_2));
        
        
        Socket socket = 
            (Socket)PrivilegedAccessor.getValue(LEAF, "_socket");
        // try a good query
        query = 
            QueryRequest.createOutOfBandQuery("berkeley", 
                                              socket.getLocalAddress().getAddress(),
                                              6346);
        LEAF.writer().simpleWrite(query);
        LEAF.writer().flush();

        Thread.sleep(4000);

        // ultrapeers should get the QR
        assertNotNull(getFirstQueryRequest(ULTRAPEER_1) );
        assertNotNull(getFirstQueryRequest(ULTRAPEER_2) );

        // LEAF should get the reply
        assertNotNull(getFirstQueryReply(LEAF));
    }

    // a node should NOT send a reply out of band via UDP if it is not
    // far away (low hop)
    public void testLowHopOutOfBandRequest() throws Exception {
        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("susheel",
                                              UDP_ACCESS.getLocalAddress().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        ULTRAPEER_2.writer().simpleWrite(query);
        ULTRAPEER_2.writer().flush();

        // ULTRAPEER_2 should get a reply via TCP
        assertNotNull(getFirstQueryReply(ULTRAPEER_2));
    }

    

}
