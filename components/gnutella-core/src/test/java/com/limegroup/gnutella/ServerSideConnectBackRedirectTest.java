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
 *  Tests that an Ultrapeer correctly handles connect back redirect messages.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test only covers Ultrapeer behavior - leaves don't participate in
 *  server side connect back stuff.
 */
public final class ServerSideConnectBackRedirectTest extends BaseTestCase {

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
     * Just a TCP connection to use for testing.
     */
    private static ServerSocket TCP_ACCESS;

    /**
     * The port for TCP_ACCESS
     */ 
    private static final int TCP_ACCESS_PORT = 10776;

    /**
	 * Second Ultrapeer connection
     */
    private static Connection ULTRAPEER_2;

	/**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());

    public ServerSideConnectBackRedirectTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideConnectBackRedirectTest.class);
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
        TCP_ACCESS = new ServerSocket(TCP_ACCESS_PORT);

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
        if ((LEAF != null) && LEAF.isOpen())
            LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
		sleep();
        UDP_ACCESS.close();
        TCP_ACCESS.close();
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

    // RUN THIS TEST FIRST
    public void testConfirmSupport() throws Exception {
	    LEAF = new Connection("localhost", PORT, new LeafHeaders("localhost"),
                              new EmptyResponder());

        //3. routed leaf, with route table for "test"
        LEAF.initialize();
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }
		assertTrue("LEAF should be connected", LEAF.isOpen());

        MessagesSupportedVendorMessage msvm = 
        (MessagesSupportedVendorMessage) getFirstMessageOfType(LEAF,
        MessagesSupportedVendorMessage.class, 500);
        assertNotNull(msvm);
        assertGreaterThan(0, msvm.supportsTCPConnectBackRedirect());
        assertGreaterThan(0, msvm.supportsUDPConnectBackRedirect());
    }

    public void testUDPConnectBackRedirect() throws Exception {
        drainAll();
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
            new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                       UDP_ACCESS.getLocalPort());
        
        LEAF.send(udp);
        LEAF.flush();
        
        // we should NOT get a ping request over our UDP socket....
        UDP_ACCESS.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            UDP_ACCESS.receive(pack);
            assertTrue(false);
        }
        catch (IOException good) {
        }
 
        cbGuid = new GUID(GUID.makeGuid());
        udp = new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                         UDP_ACCESS.getLocalPort());
        
        ULTRAPEER_1.send(udp);
        ULTRAPEER_1.flush();

        // we should get a ping reply over our UDP socket....
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                Message m = Message.read(in);
                if (m instanceof PingRequest) {
                    PingRequest reply = (PingRequest) m; 
                    assertEquals(new GUID(reply.getGUID()), cbGuid);
                    break;
                }
            }
            catch (IOException bad) {
                assertTrue("Did not get reply", false);
            }
        }
    }


    public void testUDPConnectBackAlreadyConnected() throws Exception {
        drainAll();

        DatagramSocket tempSock = new DatagramSocket(LEAF.getSocket().getLocalPort());
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
        new UDPConnectBackRedirect(cbGuid, LEAF.getSocket().getInetAddress(),
                                   LEAF.getSocket().getLocalPort());
        
        ULTRAPEER_1.send(udp);
        ULTRAPEER_1.flush();

        // we should NOT get a ping request over our UDP socket....
        tempSock.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            tempSock.receive(pack);
            assertTrue(false);
        }
        catch (IOException good) {
        }
        tempSock.close();
    }


    public void testTCPConnectBackRedirect() throws Exception {
        drainAll();
        
        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                       TCP_ACCESS.getLocalPort());
        
        LEAF.send(tcp);
        LEAF.flush();
        
        // we should NOT get a incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            assertTrue(false);
        }
        catch (IOException good) {
        }

        tcp = new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                         TCP_ACCESS.getLocalPort());
        
        ULTRAPEER_1.send(tcp);
        ULTRAPEER_1.flush();

        // we should get a incoming connection
        try {
            TCP_ACCESS.accept();
        }
        catch (IOException good) {
            assertTrue(false);
        }

    }

    public void testTCPConnectBackAlreadyConnected() throws Exception {
        drainAll();

        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(LEAF.getSocket().getInetAddress(),
                                       TCP_ACCESS.getLocalPort());
        
        ULTRAPEER_1.send(tcp);
        ULTRAPEER_1.flush();

        // we should NOT get an incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            assertTrue(false);
        }
        catch (IOException good) {
        }
    }


    public void testConnectBackExpirer() throws Exception {
        drainAll();
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
            new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                       UDP_ACCESS.getLocalPort());
        
        ULTRAPEER_2.send(udp);
        ULTRAPEER_2.flush();
        
        // we should NOT get a ping request over our UDP because we just did this
        UDP_ACCESS.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            UDP_ACCESS.receive(pack);
            assertTrue(false);
        }
        catch (IOException good) {
        }

        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                       TCP_ACCESS.getLocalPort());
        
        ULTRAPEER_2.send(tcp);
        ULTRAPEER_2.flush();
        
        // we should NOT get a incoming connection since we did this already
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            assertTrue(false);
        }
        catch (IOException good) {
        }

        // simulate the running of the thread - technically i'm not testing
        // the situation precisely, but i'm confident the schedule work so the
        // abstraction isn't terrible
        Thread cbThread = new Thread(new MessageRouter.ConnectBackExpirer());
        cbThread.start();
        cbThread.join();

        // now these two things should work....
        cbGuid = new GUID(GUID.makeGuid());
        udp = new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                         UDP_ACCESS.getLocalPort());
        
        ULTRAPEER_2.send(udp);
        ULTRAPEER_2.flush();
        
        // we should get a ping request over our UDP
        UDP_ACCESS.setSoTimeout(1000);
        // we should get a ping reply over our UDP socket....
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                Message m = Message.read(in);
                if (m instanceof PingRequest) {
                    PingRequest reply = (PingRequest) m; 
                    assertEquals(new GUID(reply.getGUID()), cbGuid);
                    break;
                }
            }
            catch (IOException bad) {
                assertTrue("Did not get reply", false);
            }
        }

        tcp = new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                         TCP_ACCESS.getLocalPort());
        
        ULTRAPEER_2.send(tcp);
        ULTRAPEER_2.flush();
        
        // we should get a incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
        }
        catch (IOException good) {
            assertTrue(false);
        }
    }


    // ------------------------------------------------------

}
