/*
 * Tests the server-side code for the udp crawler ping
 */
package com.limegroup.gnutella.messages.vendor;

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
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;


/**
 * how it works:
 * 
 *  Leaf 1 -----------\       /-------Ultrapeer 1
 *  Leaf 2 -------- Ultrapeer 0-------Ultrapeer 2
 *  Leaf 3 ----------/   /|\  \-------Ultrapeer 3
 *                        |
 *           Leaf 0 -- (GiveUPList)
 *
 *	Ultrapeer 0 is connected to 3 other Ultrapeers and 3 leafs.
 *  Leaf 0 sends various GiveUPListVendorMessages and expects to
 *  receive various results. 
 */
public class ServerSideUPListTest extends BaseTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static final int PORT = 6667;
    
    private static InetAddress _udpAddress;

    private static int _udpPort;


	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 2000;
    
    /**
	 * The different connections to the ultrapeer.
	 */
    private static CountingConnection LEAF_1,LEAF_2, LEAF_3, UP1, UP2, UP3;
    
    /**
     * the different kinds of queries
     */
    private static final GiveUPVendorMessage msgAll = new GiveUPVendorMessage(new GUID(GUID.makeGuid()));
    private static final GiveUPVendorMessage msgSome = new GiveUPVendorMessage(new GUID(GUID.makeGuid()), 2, 1);
    private static final GiveUPVendorMessage msgLeafsOnly = new GiveUPVendorMessage(new GUID(GUID.makeGuid()), 0, 2);
    private static final GiveUPVendorMessage msgNone = new GiveUPVendorMessage(new GUID(GUID.makeGuid()), 0, 0);
    private static final GiveUPVendorMessage msgBad = new GiveUPVendorMessage(new GUID(GUID.makeGuid()), -9, -2);
	
    
    /**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());
	/**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS, UDP_WRITER;
	
	
	public ServerSideUPListTest(String name) {
		super(name);
	}
	public static Test suite() {
        return buildTestSuite(ServerSideUPListTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	/**
	 * builds the connections between the entities in the test
	 * @throws Exception something bad happened (?)
	 */
	private static void buildConnections() throws Exception {
		LEAF_1 =
			new CountingConnection("localhost", PORT, 
                            new LeafHeaders("localhost"),new EmptyResponder());

	    LEAF_2 =
			new CountingConnection("localhost", PORT, 
                            new LeafHeaders("localhost"), new EmptyResponder());

	    LEAF_3 =
			new CountingConnection("localhost", PORT, 
                           new LeafHeaders("localhost"), new EmptyResponder());
	    UP1 = 
			new CountingConnection("localhost", PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder() );
	    UP2 = 
			new CountingConnection("localhost", PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder() );
	    UP3 = 
			new CountingConnection("localhost", PORT,
						   new UltrapeerHeaders("localhost"),
						   new EmptyResponder() );
	    
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
		UP1.close();
		UP2.close();
		UP3.close();
		sleep();
	}
	
	private static void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}
	
	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private static void connect() throws Exception {
		buildConnections();
        //ultrapeers 
        UP2.initialize();
        UP1.initialize();
        UP3.initialize();
        
        //leafs
        LEAF_1.initialize();
        LEAF_2.initialize();
        LEAF_3.initialize();
                

		assertTrue("ULTRAPEER_2 should be connected", UP2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", UP1.isOpen());
		assertTrue("ULTRAPEER_3 should be connected", UP3.isOpen());
		assertTrue("LEAF should be connected", LEAF_1.isOpen());
		assertTrue("LEAF should be connected", LEAF_2.isOpen());
		assertTrue("LEAF should be connected", LEAF_3.isOpen());
		

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		sleep();
    }
    
    /**
	 * Drains all messages 
	 */
 	private static void drainAll() throws Exception {
 		if(UP1.isOpen()) {
 			drain(UP1);
 		}
 		if(UP2.isOpen()) {
 			drain(UP2);
 		}
 		if(UP3.isOpen()){
 			drain(UP3);
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
 		
 		
 	}
	
 	/**
 	 * sends out a message requesting all leafs + ups 
 	 * @throws Exception
 	 */
 	public void testMsgAll() throws Exception {
 		_udpAddress = UDP_ACCESS.getLocalAddress();
 		_udpPort = PORT;
 		
 		//send a packet
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		msgAll.write(baos);
 		DatagramPacket pack = new DatagramPacket(baos.toByteArray(),
 							baos.toByteArray().length,
							_udpAddress, _udpPort);
 		assertNotNull(baos.toByteArray());
 		assertNotNull(_udpAddress);
 		UDP_ACCESS.send(pack);
 		
 		//now read the response 		
 		_udpAddress = UDP_ACCESS.getLocalAddress();
 		_udpPort = UDP_ACCESS.getLocalPort();
 		pack = new DatagramPacket(new byte[1000],1000);
 		try {
 			UDP_ACCESS.receive(pack);
 		} catch(IOException bad) {
 			fail("did not get reply",bad);
 		}
 		
 		//truncate the response to the appropriate size
 		/*assertNotEquals(pack.getLength(),1000);
 		byte [] full = pack.getData();
 		byte []truncated = new byte [pack.getLength()];
 		System.arraycopy(full,0,truncated,0,truncated.length);
 		pack = new DatagramPacket(truncated,truncated.length);
 		*/
 		//parse the response
 		InputStream in = new ByteArrayInputStream(pack.getData());
 		UPListVendorMessage reply = null;
 		try {
 			reply = (UPListVendorMessage)Message.read(in);
 		}catch(BadPacketException bad) {
 			System.out.println("the size of the received response is "+
 						pack.getData().length);
 			fail("could not parse the response",bad);
 		} catch (ClassCastException ccx) {
 			System.out.println(new String(pack.getData()));
 			fail("parsed to a wrong packet type",ccx);
 		}
 		
 		//then test whether the guids are the same

 		assertEquals(new String(reply.getGUID()),new String(msgAll.getGUID()));
 		
 		
 	}
}
