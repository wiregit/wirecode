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
import com.bitzi.util.*;

import junit.framework.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/**
 *  Tests that an Ultrapeer correctly handles all aspects of PushProxy.  For
 *  example:
 *  1) handles the VendorMessage exchange as expected
 *  2) handles HTTP requests as expected, forwarding on a PushRequest
 *
 *  This class tests a lot of different pieces of code.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  The leaf must be connected in the first test.
 */
public final class ServerSidePushProxyTest extends BaseTestCase {

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
     * the client guid of the LEAF - please set in the first test.
     */
    private static byte[] clientGUID = null;

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public ServerSidePushProxyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSidePushProxyTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private static void buildConnections() throws Exception {
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
 		if((LEAF != null) && LEAF.isOpen()) {
 			drain(ULTRAPEER_2);
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
        
        // for Ultrapeer 1
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER_1.send((RouteTableMessage)iter.next());
			ULTRAPEER_1.flush();
        }

		assertTrue("ULTRAPEER_2 should be connected", ULTRAPEER_2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", ULTRAPEER_1.isOpen());

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

    // THIS TEST MUST BE FIRST - MAKES SURE THE UP SUPPORTS THE PUSHPROXY VM
    // EXCHANGE AND SETS UP OTHER TESTS
    public void testEstablishPushProxy() throws Exception {
        drainAll();
        Message m = null;
        clientGUID = GUID.makeGuid();

        LEAF = new Connection("localhost", PORT, new LeafHeaders("localhost"),
                              new EmptyResponder());
        // routed leaf, with route table for "test"
        LEAF.initialize();
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }

        // make sure UP is advertised proxy support
        do {
            m = LEAF.receive(TIMEOUT);
        } while (!(m instanceof MessagesSupportedVendorMessage)) ;
        assertTrue(((MessagesSupportedVendorMessage)m).supportsPushProxy() > 0);

        // send proxy request
        PushProxyRequest req = new PushProxyRequest(new GUID(clientGUID));
        LEAF.send(req);
        LEAF.flush();

        // wait for ack
        do {
            m = LEAF.receive(TIMEOUT);
        } while (!(m instanceof PushProxyAcknowledgement)) ;
        assertTrue(Arrays.equals(m.getGUID(), clientGUID));
        assertTrue(((PushProxyAcknowledgement)m).getListeningPort() == PORT);

        // ultrapeer supports push proxy setup A-OK
    }


    public void testGoodPushProxyRequest() throws Exception {
        Message m = null;
        String result = null;

        Socket s = new Socket("localhost", PORT);
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        // first test a GET
        out.write("GET /gnutella/push-proxy?ServerID=");
        out.write(Base32.encode(clientGUID));
        out.write(" HTTP/1.1\r\n");
        out.write("X-Node:127.0.0.1:6346\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode - less important, but might as well
        result = in.readLine();
        assertTrue(result, (result.indexOf("202") > -1));
        // clear out other responses
        while (in.readLine() != null) ;

        // leaf should get PushRequest
        do {
            m = LEAF.receive(TIMEOUT);
        } while (!(m instanceof PushRequest)) ;
        PushRequest pr = (PushRequest) m;
        assertEquals(0, pr.getIndex());
        assertEquals(new GUID(clientGUID), new GUID(pr.getClientGUID()));
        assertEquals(6346, pr.getPort());
        assertTrue(pr.getIP()[0] == 127);
        assertTrue(pr.getIP()[1] == 0);
        assertTrue(pr.getIP()[2] == 0);
        assertTrue(pr.getIP()[3] == 1);

        // test a HEAD
        s = new Socket("localhost", PORT);
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        out.write("HEAD /gnutella/push-proxy?ServerID=");
        out.write(Base32.encode(clientGUID));
        out.write(" HTTP/1.1\r\n");
        out.write("X-Node:10.238.1.87:6350\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode - less important, but might as well
        result = in.readLine();
        assertTrue(result, (result.indexOf("202") > -1));
        // clear out other responses
        while (in.readLine() != null) ;
        
        // leaf should get PushRequest
        do {
            m = LEAF.receive(TIMEOUT);
        } while (!(m instanceof PushRequest)) ;
        pr = (PushRequest) m;
        assertEquals(0, pr.getIndex());
        assertEquals(new GUID(clientGUID), new GUID(pr.getClientGUID()));
        assertEquals(6350, pr.getPort());
        assertEquals(pr.getIP()[0], 10);
        assertEquals(ByteOrder.ubyte2int(pr.getIP()[1]), 238);
        assertEquals(pr.getIP()[2], 1);
        assertEquals(pr.getIP()[3], 87);
        
    }
    

    public void testBadPushProxyRequest() throws Exception {
        Message m = null;
        String result = null;

        Socket s = new Socket("localhost", PORT);
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        // first test a GET with a unrecognized guid
        out.write("GET /gnutella/push-proxy?ServerID=");
        out.write(Base32.encode(GUID.makeGuid()));
        out.write(" HTTP/1.1\r\n");
        out.write("X-Node:127.0.0.1:6346\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode - less important, but might as well
        result = in.readLine();
        assertTrue(result, (result.indexOf("410") > -1));
        // clear out other responses
        while (in.readLine() != null) ;

        // leaf should NOT get PushRequest
        try {
            do {
                m = LEAF.receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // now test a HEAD with a bad IP
        s = new Socket("localhost", PORT);
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        out.write("HEAD /gnutella/push-proxy?ServerID=");
        out.write(Base32.encode(clientGUID));
        out.write(" HTTP/1.1\r\n");
        out.write("X-Node:www.crapalapadapa.com:6346\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode - less important, but might as well
        result = in.readLine();
        assertTrue(result, (result.indexOf("400") > -1));
        // clear out other responses
        while (in.readLine() != null) ;

        // leaf should NOT get PushRequest
        try {
            do {
                m = LEAF.receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}


    }
    

}
