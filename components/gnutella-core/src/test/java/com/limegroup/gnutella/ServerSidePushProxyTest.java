package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.Socket;

import junit.framework.Test;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
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
import com.limegroup.gnutella.util.NetworkUtils;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Map;

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
     * the client GUID of the leaf as a GUID.
     */
    private static GUID leafGUID = null;

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

    // BEGIN TESTS
    // ------------------------------------------------------

    // THIS TEST MUST BE FIRST - MAKES SURE THE UP SUPPORTS THE PUSHPROXY VM
    // EXCHANGE AND SETS UP OTHER TESTS
    public void testEstablishPushProxy() throws Exception {
        drainAll();
        Message m = null;
        clientGUID = GUID.makeGuid();
        leafGUID = new GUID(clientGUID);

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
        assertEquals(PORT, ((PushProxyAcknowledgement)m).getListeningPort());

        // ultrapeer supports push proxy setup A-OK
    }
    
    public void testGETWithServerId() throws Exception {
        tRequest("GET", // request method.
                 "/gnutella/push-proxy", // the request
                 "ServerID", // initial param
                 Base32.encode(clientGUID), // initial value
                 "127.0.0.1", // ip
                 6346, // port
                 null, // params
                 202); // opcode expected in return
    }
    
    public void testHEADWithServerId() throws Exception {
        tRequest("HEAD", "/gnutella/push-proxy", "Serverid", 
                Base32.encode(clientGUID), "10.238.1.87", 6350, null, 202);
    }
    
    public void testInvalidGUIDWithServerId() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "serverid",
                Base32.encode(GUID.makeGuid()), "127.0.0.1", 6346, null, 410);
    }
    
    public void testInvalidGUIDWithGuid()  throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "guid", 
                new GUID().toHexString(), "127.0.0.1", 6346, null, 410);
    }
    
    public void testInvalidIP() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "serverid",
                Base32.encode(clientGUID), "www.crapalapadapa.com",
                6346, null, 400);
    }
    
    public void testServerIdWithBase16Fails() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "serverid",
                 leafGUID.toHexString(), "127.0.0.1", 6346, null, 400);
    }
    
    public void testGuidIsBase16() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, null, 202);
    }
    
    public void testGuidWithBase32Fails() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "guid",
                Base32.encode(clientGUID), "127.0.0.1", 6346, null, 400);
    }
    
    public void testGuidWithGnet() throws Exception {
        tRequest("GET", "/gnet/push-proxy", "guid",
                Base32.encode(clientGUID), "127.0.0.1", 6346, null, 400);
    }
        
    public void testFileChangesIndex() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testFileWithGnet() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        tRequest("GET", "/gnet/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testCannotHaveServeridAndGuid() throws Exception {
        Map m = new HashMap();
        m.put("serverid", Base32.encode(clientGUID));
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 400);
    }
    
    public void testMultipleFileFails() throws Exception {
        Map m = new HashMap();
        m.put("FILE", new Integer(1));
        m.put("file", new Integer(2));
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 400);
    }
    
    private void tRequest(String reqMethod, String reqKey, String initKey,
                          String guid, String ip, int port, Map params,
                          int opcode)
     throws Exception {
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            
        String result = null;
        Message m = null;

        out.write(reqMethod + " " + reqKey + "?");
        out.write(initKey + "=" + guid);
        if( params != null ) {
            for(Iterator i = params.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                out.write("&" + entry.getKey() + "=" + entry.getValue());
            }
        }
        out.write(" HTTP/1.1.\r\n");
        out.write("X-Node: ");
        out.write(ip + ":" + port + "\r\n");
        out.write("\r\n");
        out.flush();
        
        // check opcode - less important, but might as well
        result = in.readLine();
        assertGreaterThan(result, -1, result.indexOf("" + opcode));
        // clear out other responses
        while (in.readLine() != null) ;
        
        if( opcode != 202 ) {
            // leaf NOT expecting PushRequest.
            try {
                do {
                    m = LEAF.receive(TIMEOUT);
                    assertTrue(!(m instanceof PushRequest));
                } while (true) ;
            }
            catch (InterruptedIOException expected) {}
        } else {
            // leaf should get PushRequest
            do {
                m = LEAF.receive(TIMEOUT);
            } while (!(m instanceof PushRequest)) ;
            PushRequest pr = (PushRequest) m;
            int idx = 0;
            if(params != null && params.get("file") != null )
                idx = ((Integer)params.get("file")).intValue();
            assertEquals(idx, pr.getIndex());
            assertEquals(new GUID(clientGUID), new GUID(pr.getClientGUID()));
            assertEquals(port, pr.getPort());
            assertEquals(ip, NetworkUtils.ip2string(pr.getIP()));
        }
    }
}
