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
    private static final int PORT = 6669;
    
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
    private static final UDPCrawlerPing msgAll = new UDPCrawlerPing(new GUID(GUID.makeGuid()));
    private static final UDPCrawlerPing msgSome = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 2, 1,(byte)0);
    private static final UDPCrawlerPing msgLeafsOnly = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 0, 2,(byte)0);
    private static final UDPCrawlerPing msgNone = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 0, 0,(byte)0);
    private static final UDPCrawlerPing msgMore = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 20, 30,(byte)0);
    private static final UDPCrawlerPing msgTimes = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)1);
    private static final UDPCrawlerPing msgLocale = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)2);
    private static final UDPCrawlerPing msgBadMask = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0xFF);
    private static final UDPCrawlerPing msgNewOnly = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x4);
    private static final UDPCrawlerPing msgAgents = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x8);
	
    
    /**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());
	/**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;
	
	
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
        
        RouterService.setListeningPort(PORT);
		ROUTER_SERVICE.start();
		ROUTER_SERVICE.clearHostCatcher();
		ROUTER_SERVICE.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());
        
        PrivilegedAccessor.setValue(RouterService.getPromotionManager(),
				"_UDPListRequestors",
				new FixedSizeExpiringSet(200,200));
        UDP_ACCESS.connect(InetAddress.getLocalHost(),PORT);
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
 		
 		UDPCrawlerPong reply = tryMessage(msgAll);
 		//test whether we got proper # of results
 		assertEquals(3,reply.getLeaves().size());
 		assertEquals(3,reply.getUltrapeers().size());
 		
 		assertEquals(LEAF_1.getInetAddress(),LEAF_2.getInetAddress());
 		assertEquals(LEAF_1.getInetAddress(),LEAF_3.getInetAddress());
 		
 		assertEquals(UP1.getInetAddress(),UP2.getInetAddress());
 		assertEquals(UP1.getInetAddress(),UP3.getInetAddress());
 		
 		for (Iterator iter = reply.getLeaves().iterator();iter.hasNext();){
 			Endpoint e =(Endpoint)iter.next();
 			assertEquals(LEAF_1.getInetAddress(),e.getInetAddress());
 			assertTrue(e.getPort() == LEAF_1.getPort());
 		}
 		
 		for (Iterator iter = reply.getUltrapeers().iterator();iter.hasNext();){
 			Endpoint e =(Endpoint)iter.next();
 			assertEquals(UP1.getInetAddress(),e.getInetAddress());
 			assertTrue(e.getPort() == UP1.getPort());
 		}
 		sleep();
 		
 	}
 	
 	/**
 	 * sends a message requesting 0 leafs and 0 ups.
 	 */
 	public void testMsgNone() throws Exception {
 		UDPCrawlerPong reply = tryMessage(msgNone);
 		
 		assertEquals(0,reply.getLeaves().size());
 		assertEquals(0,reply.getUltrapeers().size());
 		sleep();
 	}
 	
 	/**
 	 * sends a message requesting leafs only
 	 */
 	public void testMsgLeafs() throws Exception {
 		UDPCrawlerPong reply = tryMessage(msgLeafsOnly);
 		
 		assertEquals(2,reply.getLeaves().size());
 		assertEquals(0,reply.getUltrapeers().size());
 		sleep();
 	}
 	
 	/**
 	 * sends a message requesting 1 leafs and 2 ups.
 	 */
 	public void testMsgSome() throws Exception {
 		UDPCrawlerPong reply = tryMessage(msgSome);
 		
 		assertEquals(1,reply.getLeaves().size());
 		assertEquals(2,reply.getUltrapeers().size());
 		sleep();
 	}
 	
 	/**
 	 * sends a message requesting more UPs and Leafs that the host has.
 	 */
 	public void testMsgMore() throws Exception {
 		UDPCrawlerPong reply = tryMessage(msgMore);
 		
 		//we should get the number we have connected.
 		assertEquals(3,reply.getLeaves().size());
 		assertEquals(3,reply.getUltrapeers().size());
 		sleep();
 	}
 	
 	
 	/**
 	 * requests all connections of a node; few of them disconnect
 	 * and then they are requested again.
 	 */
 	public void testAllDisconnectAllAgain() throws Exception {
 		LEAF_2.close();
 		LEAF_3.close();
 		UP2.close();
 		
 		sleep();
 		UDPCrawlerPong reply = tryMessage(msgAll);
 		
 		assertEquals(2,reply.getUltrapeers().size());
 		assertEquals(1,reply.getLeaves().size());
 		sleep();
 	}
 	
 	/**
 	 * tests whether requesting too often will give us a reply
 	 */
 	public void testHammering() throws Exception {
 		
 		//first message should go through
 		UDPCrawlerPong reply = tryMessage(msgAll);
 		
 		//second shouldn't
 		try {
 			reply = tryMessage(msgAll);
 			fail("ioex expected");
 		}catch (IOException iox) {}
 		sleep();
 		
 		//third should
 		reply = tryMessage(msgAll);
 		assertNotEquals(0,reply.getLeaves().size());
 		assertNotEquals(0,reply.getUltrapeers().size());
 		sleep();
 	}
 	
 	/**
 	 * tests a udp ping message requesting the connection lifetimes
 	 */
 	public void testConnectionTime() throws Exception{
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
 		UDPCrawlerPong reply = tryMessage(msgTimes);
 		assertTrue(reply.hasConnectionTime());
 		assertFalse(reply.hasLocaleInfo());
 		
 		//see if the result we got had any uptime (it should!)
 		ExtendedEndpoint result = (ExtendedEndpoint)reply.getUltrapeers().get(0);
 		assertGreaterThan(0,result.getDailyUptime());
 		sleep();
 	}
 	
 	/**
 	 * tests a message send from a newer peer that supports more options.
 	 */
 	public void testBadMask() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
 		UDPCrawlerPong reply = tryMessage(msgBadMask);
 		assertTrue(reply.hasConnectionTime());
 		assertTrue(reply.hasLocaleInfo());
 		
 		//see if the result we got had any uptime (it should!)
 		ExtendedEndpoint result = (ExtendedEndpoint)reply.getUltrapeers().get(0);
 		assertGreaterThan(0,result.getDailyUptime());
 		sleep();
 	}
 	
 	/**
 	 * tests a ping message requesting locale info.
 	 */
 	public void testLocale() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
 		UDPCrawlerPong reply = tryMessage(msgLocale);
 		
 		assertFalse(reply.hasConnectionTime());
 		assertTrue(reply.hasLocaleInfo());
 		
 		//see if any of the connections have the locale in them - they should
 		ExtendedEndpoint result = (ExtendedEndpoint)reply.getUltrapeers().get(0);
 		assertEquals(ApplicationSettings.LANGUAGE.getValue(),
 				result.getClientLocale());
 		sleep();
 	}
 	
 	/**
 	 * tests a ping message requesting only results that support
 	 * udp crawling themselves.
 	 */
 	public void testNewOnly() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
 		

 		//make sure some of our connections support crawling.
 		
 		MessagesSupportedVendorMessage msvm = (MessagesSupportedVendorMessage)
			PrivilegedAccessor.invokeConstructor(MessagesSupportedVendorMessage.class, new Object[0]);
 		
 		assertGreaterThan(0,msvm.supportsUDPCrawling());
 		
 		List upCons = RouterService.getConnectionManager().getInitializedConnections();
 		
 		Connection notSupporting = (Connection) upCons.get(0);
 		Connection supporting = (Connection) upCons.get(1);
 		
 		assertLessThanOrEquals(0,supporting.remoteHostSupportsUDPCrawling());
 		assertLessThanOrEquals(1,supporting.remoteHostSupportsUDPCrawling());
 		
 		PrivilegedAccessor.setValue(supporting,"_messagesSupported",msvm);
 		
 		assertLessThanOrEquals(0,notSupporting.remoteHostSupportsUDPCrawling());
 		assertGreaterThanOrEquals(1,supporting.remoteHostSupportsUDPCrawling());
 		
 		
 		
 		//so now, one less UP should be in the result.
 		
 		UDPCrawlerPong pong = tryMessage(msgNewOnly);
 		
 		assertEquals(upCons.size()-1, pong.getUltrapeers().size());
 		
 		
 		sleep();
 		
 		//now, make the other UP support that message as well
 		
 		PrivilegedAccessor.setValue(notSupporting,"_messagesSupported",msvm);
 		
 		assertGreaterThan(0,notSupporting.remoteHostSupportsUDPCrawling());
 		assertGreaterThan(0,supporting.remoteHostSupportsUDPCrawling());
 		
 		pong = tryMessage(msgNewOnly);
 		
 		assertEquals(upCons.size(),pong.getUltrapeers().size());
 		sleep();
 	}
 	
 	public void testMsgAgents() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
 		
 		UDPCrawlerPong pong = tryMessage(msgAgents);
 		
 		assertNotNull(pong.getAgents());
 		
 		//we should have 3 agents reported.
 		StringTokenizer tok = new StringTokenizer(pong.getAgents(),
 				UDPCrawlerPong.AGENT_SEP);
 		
 		assertEquals(3,tok.countTokens());
 		
 		while(tok.hasMoreTokens())
 			assertEquals("LimeWire/@version@",tok.nextToken());
 		
 	}
 	
 	private UDPCrawlerPong tryMessage(UDPCrawlerPing which) throws Exception {
 		assertTrue(UDPService.instance().isListening());
 		UDP_ACCESS.setSoTimeout(5000);
 		
 		
 		_udpAddress = UDP_ACCESS.getLocalAddress();
 		
 		//send a packet
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		which.write(baos);
 		DatagramPacket pack = new DatagramPacket(baos.toByteArray(),
 							baos.toByteArray().length,
							_udpAddress, PORT);
 		
 		assertNotNull(baos.toByteArray());
 		assertNotNull(_udpAddress);
 		
 		UDP_ACCESS.send(pack);
 		
 		//now read the response 		
 		//_udpPort = UDP_ACCESS.getLocalPort();
 		pack = new DatagramPacket(new byte[1000],1000);
 		
 		//not catching IOEx here because not replying is a valid scenario.
 		
 		UDP_ACCESS.receive(pack);
 		
 		
 		//parse the response
 		InputStream in = new ByteArrayInputStream(pack.getData());
 		UDPCrawlerPong reply = null;
 		try {
 			reply = (UDPCrawlerPong)Message.read(in);
 		}catch(BadPacketException bad) {
 			System.out.println("the size of the received response is "+
 						pack.getData().length);
 			fail("could not parse the response",bad);
 		} catch (ClassCastException ccx) {
 			System.out.println(new String(pack.getData()));
 			fail("parsed to a wrong packet type",ccx);
 		}
 		
 		//then test whether the guids are the same
 		assertEquals(new String(reply.getGUID()),new String(which.getGUID()));
 		return reply;
 	}
}
