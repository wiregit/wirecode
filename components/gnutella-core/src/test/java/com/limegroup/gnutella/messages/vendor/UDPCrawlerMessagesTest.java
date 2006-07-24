/*
 * Tests the server-side code for the udp crawler ping
 */
package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import junit.framework.Test;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.CountingConnection;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.IPPortCombo;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory.VendorMessageParser;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.MojitoDHT;


public class UDPCrawlerMessagesTest extends BaseTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static final int PORT = 6669;
    
    private static InetAddress _udpAddress;

    /**
	 * The different connections to the ultrapeer.
	 */
    private static CountingConnection LEAF_1,LEAF_2, LEAF_3, UP1, UP2, UP3;
    
    /**
     * the different kinds of queries
     */
    private static final UDPCrawlerPing msgAll = new UDPCrawlerPing(new GUID(GUID.makeGuid()));
    
    /**
	 * The central Ultrapeer used in the test.
	 */
	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());
	/**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;
	
	
	public UDPCrawlerMessagesTest(String name) {
		super(name);
	}
	public static Test suite() {
        return buildTestSuite(UDPCrawlerMessagesTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	/**
	 * builds the connections between the entities in the test
	 * @throws Exception something bad happened (?)
	 */
	private static void buildConnections() throws Exception {
		LEAF_1 = new CountingConnection("localhost", PORT);
	    LEAF_2 = new CountingConnection("localhost", PORT);
	    LEAF_3 = new CountingConnection("localhost", PORT);
	    UP1 =  new CountingConnection("localhost", PORT);
	    UP2 = new CountingConnection("localhost", PORT);
	    UP3 =  new CountingConnection("localhost", PORT);
	    
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
        RouterService.clearHostCatcher();
        RouterService.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());
        
        Object handler = RouterService.getMessageRouter().getUDPMessageHandler(UDPCrawlerPing.class);
        PrivilegedAccessor.setValue(handler,
				"_UDPListRequestors",
				new FixedSizeExpiringSet(200,200));
        UDP_ACCESS.connect(InetAddress.getLocalHost(),PORT);
        VendorMessageFactory.setParser(VendorMessage.F_CRAWLER_PONG, VendorMessage.F_LIME_VENDOR_ID, 
                new UDPCrawlerPongParserStub());
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
        UP1.initialize(new UltrapeerHeaders("localhost"), new EmptyResponder());
        UP2.initialize(new UltrapeerHeaders("localhost"), new EmptyResponder());
        UP3.initialize(new UltrapeerHeaders("localhost"), new EmptyResponder());
        
        //leafs
        LEAF_1.initialize(new LeafHeaders("localhost"),new EmptyResponder());
        LEAF_2.initialize(new LeafHeaders("localhost"), new EmptyResponder());
        LEAF_3.initialize(new LeafHeaders("localhost"), new EmptyResponder());
                

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
 		UDPCrawlerPong reply = new UDPCrawlerPong(msgAll);
 		//test whether we got proper # of results
        byte[] payload = reply.getPayload();
        assertEquals(3,payload[0]);
        assertEquals(3,payload[1]);
 		
 		assertEquals(LEAF_1.getInetAddress(),LEAF_2.getInetAddress());
 		assertEquals(LEAF_1.getInetAddress(),LEAF_3.getInetAddress());
 		
 		assertEquals(UP1.getInetAddress(),UP2.getInetAddress());
 		assertEquals(UP1.getInetAddress(),UP3.getInetAddress());
 		
        int index = 3; //the index within the result block.
        
        byte [] current = new byte[6];
        
        System.arraycopy(payload,index,current,0,6);
        IPPortCombo combo = 
            IPPortCombo.getCombo(current);
        Endpoint e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(UP1.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == UP1.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        combo = IPPortCombo.getCombo(current);
        e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(UP2.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == UP2.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        combo = IPPortCombo.getCombo(current);
        e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(UP3.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == UP3.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        combo = IPPortCombo.getCombo(current);
        e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(LEAF_1.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == LEAF_1.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        combo = IPPortCombo.getCombo(current);
        e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(LEAF_2.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == LEAF_2.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        combo = IPPortCombo.getCombo(current);
        e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(LEAF_3.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == LEAF_3.getPort());
 	}
 	
 	/**
 	 * sends a message requesting 0 leafs and 0 ups.
 	 */
 	public void testMsgNone() throws Exception {
 	    UDPCrawlerPing msgNone = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 0, 0,(byte)0);
 		UDPCrawlerPong reply = new UDPCrawlerPong(msgNone);
        byte[] payload = reply.getPayload();
 		assertEquals(0,payload[0]);
 		assertEquals(0,payload[1]);
 	}
 	
 	/**
 	 * sends a message requesting leafs only
 	 */
 	public void testMsgLeafs() throws Exception {
       UDPCrawlerPing msgLeafsOnly = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 0, 2,(byte)0);
 		UDPCrawlerPong reply = new UDPCrawlerPong(msgLeafsOnly);
        byte[] payload = reply.getPayload();
 		assertEquals(2,payload[1]);
 		assertEquals(0,payload[0]);
 	}
 	
 	/**
 	 * sends a message requesting 1 leafs and 2 ups.
 	 */
 	public void testMsgSome() throws Exception {
 	    UDPCrawlerPing msgSome = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 2, 1,(byte)0);
 		UDPCrawlerPong reply = new UDPCrawlerPong(msgSome);
        byte[] payload = reply.getPayload();
 		
 		assertEquals(1,payload[1]);
 		assertEquals(2,payload[0]);
 		sleep();
 	}
 	
 	/**
 	 * sends a message requesting more UPs and Leafs that the host has.
 	 */
 	public void testMsgMore() throws Exception {
 	    UDPCrawlerPing msgMore = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 20, 30,(byte)0);
 		UDPCrawlerPong reply = new UDPCrawlerPong(msgMore);
        byte[] payload = reply.getPayload();
 		
 		//we should get the number we have connected.
 		assertEquals(3,payload[1]);
 		assertEquals(3,payload[0]);
 		sleep();
 	}
 	
    
    public void testUDPCrawlerPingMessage() throws Exception {
        try {
            tryMessage();
        } catch (Exception ex) {
            fail("message should not fail");
            throw ex;
        }
        sleep();
    }
    
    /**
     * tests whether requesting too often will give us a reply
     */
    public void testHammering() throws Exception {
        
        //first message should go through
        tryMessage();
        
        //second shouldn't
        try {
            tryMessage();
            fail("ioex expected");
        }catch (IOException iox) {}
        sleep();
        
        //third should
        tryMessage();
        sleep();
    }
 	
 	/**
 	 * tests a udp ping message requesting the connection lifetimes
 	 */
 	public void testConnectionTime() throws Exception{
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgTimes = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)1);
 		UDPCrawlerPong reply = new UDPCrawlerPong(msgTimes);
        byte[] payload = reply.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
 		assertTrue((format & UDPCrawlerPing.CONNECTION_TIME)
                == (int)UDPCrawlerPing.CONNECTION_TIME);
 		assertFalse((format & UDPCrawlerPing.LOCALE_INFO)
                == (int)UDPCrawlerPing.LOCALE_INFO);
 		
 		//see if the result we got had any uptime (it should!)
 		short uptime = ByteOrder.leb2short(payload,9);
 		assertGreaterThan(0,uptime);
 	}
 	
 	/**
 	 * tests a message send from a newer peer that supports more options.
 	 */
 	public void testBadMask() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgBadMask = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0xFF);
 		UDPCrawlerPong reply = new UDPCrawlerPong(msgBadMask);
        byte[] payload = reply.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertTrue((format & UDPCrawlerPing.CONNECTION_TIME)
                == (int)UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.LOCALE_INFO)
                == (int)UDPCrawlerPing.LOCALE_INFO);
        
        //see if the result we got had any uptime (it should!)
        short uptime = ByteOrder.leb2short(payload,13);
        assertGreaterThan(0,uptime);
 	}
 	
 	/**
 	 * tests a ping message requesting locale info.
 	 */
 	public void testLocale() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgLocale = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)2);
        UDPCrawlerPong reply = new UDPCrawlerPong(msgLocale);
        byte[] payload = reply.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertFalse((format & UDPCrawlerPing.CONNECTION_TIME)
                == (int)UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.LOCALE_INFO)
                == (int)UDPCrawlerPing.LOCALE_INFO);
 		
 		//see if any of the connections have the locale in them - they should
        String lang = new String(payload, 9, 2);
 		assertEquals(ApplicationSettings.LANGUAGE.getValue(),
               lang);
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
 		
 		assertLessThanOrEquals(1,notSupporting.remoteHostSupportsUDPCrawling());
 		assertLessThanOrEquals(1,supporting.remoteHostSupportsUDPCrawling());
 		
 		PrivilegedAccessor.setValue(supporting,"_messagesSupported",msvm);
 		
 		assertLessThanOrEquals(1,notSupporting.remoteHostSupportsUDPCrawling());
 		assertGreaterThanOrEquals(1,supporting.remoteHostSupportsUDPCrawling());
 		
 		
 		
 		//so now, only one UP should be in the result.
        UDPCrawlerPing msgNewOnly = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x4);
 		UDPCrawlerPong pong = new UDPCrawlerPong(msgNewOnly);
 		
 		assertEquals(1, pong.getPayload()[0]);
 		
 		
 		
 		//now, make one other UP support that message as well
 		
 		PrivilegedAccessor.setValue(notSupporting,"_messagesSupported",msvm);
 		
 		assertGreaterThan(0,notSupporting.remoteHostSupportsUDPCrawling());
 		assertGreaterThan(0,supporting.remoteHostSupportsUDPCrawling());
 		
 		pong = new UDPCrawlerPong(msgNewOnly);
 		
 		assertEquals(2,pong.getPayload()[0]);
 	}
 	
 	public void testMsgAgents() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgAgents = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x8);
 		UDPCrawlerPong pong = new UDPCrawlerPong(msgAgents);
        byte[] payload = pong.getPayload();
        int agentsOffset=(payload[0]+payload[1])*6+3;
        int agentsSize = ByteOrder.leb2short(payload,agentsOffset);
 		assertGreaterThan(0, agentsSize);
 		
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(payload,agentsOffset+2,agentsSize);
            
        GZIPInputStream gais = null;
        gais = new GZIPInputStream(bais);
        DataInputStream dais = new DataInputStream(gais);
        byte [] length = new byte[2];
        dais.readFully(length);
        int len = ByteOrder.leb2short(length,0);
        byte []agents = new byte[len];
        dais.readFully(agents);
            
        String agentsString = new String(agents);
 		//we should have 3 agents reported.
 		StringTokenizer tok = new StringTokenizer(agentsString,
 				UDPCrawlerPong.AGENT_SEP);
 		
 		assertEquals(7,tok.countTokens());
 		
 		while(tok.hasMoreTokens())
 			assertEquals(CommonUtils.getHttpServer(),tok.nextToken());
 		
 	}
    
    public void testMsgNodeConnectionUptime() throws Exception {
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(5);
        PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgNodeUptime = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x10);
        UDPCrawlerPong pong = new UDPCrawlerPong(msgNodeUptime);
        byte[] payload = pong.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertFalse((format & UDPCrawlerPing.CONNECTION_TIME)
                == (int)UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.NODE_UPTIME)
                == (int)UDPCrawlerPing.NODE_UPTIME);
        
        //we should have an uptime > 5
        assertGreaterThan(5,ByteOrder.leb2int(payload,3));
        //the offset should be correctly set
        byte[] current = new byte[6];
        System.arraycopy(payload,7,current,0,6);
        IPPortCombo combo = 
            IPPortCombo.getCombo(current);
        Endpoint e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(UP1.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == UP1.getPort());
    }
    
    public void testMsgDHTStatus() throws Exception {
        PrivilegedAccessor.setValue(ROUTER_SERVICE, "dhtManager", new DHTManagerStub());
        UDPCrawlerPing msgDHTCapable = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x20);
        DHTSettings.ACTIVE_DHT_CAPABLE.setValue(true);
        UDPCrawlerPong pong = new UDPCrawlerPong(msgDHTCapable);
        byte[] payload = pong.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertFalse((format & UDPCrawlerPing.CONNECTION_TIME)
                == (int)UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.DHT_STATUS)
                == (int)UDPCrawlerPing.DHT_STATUS);
        
        byte status = payload[3];
        assertTrue( (status & UDPCrawlerPong.DHT_CAPABLE_MASK) == UDPCrawlerPong.DHT_CAPABLE_MASK);
        assertTrue( (status & UDPCrawlerPong.DHT_WAITING_MASK) == UDPCrawlerPong.DHT_WAITING_MASK);
        assertFalse( (status & UDPCrawlerPong.DHT_ACTIVE_MASK) == UDPCrawlerPong.DHT_ACTIVE_MASK);
        
        //now see if offset is correctly set
        msgDHTCapable = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x30);
        pong = new UDPCrawlerPong(msgDHTCapable);
        payload = pong.getPayload();
        status = payload[7];
        assertTrue( (status & UDPCrawlerPong.DHT_WAITING_MASK) == UDPCrawlerPong.DHT_WAITING_MASK);
    }
    
 	private void tryMessage() throws Exception {
 		assertTrue(UDPService.instance().isListening());
 		UDP_ACCESS.setSoTimeout(5000);
 		
 		
 		_udpAddress = UDP_ACCESS.getLocalAddress();
 		
 		//send a packet
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		msgAll.write(baos);
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
        byte[] buf = new byte[23];
        in.read(buf,0,23);
        assertEquals((byte)0x31,buf[16]);
        byte vendorId[] = new byte[4];
        in.read(vendorId, 0, 4);
        assertEquals(VendorMessage.F_LIME_VENDOR_ID, vendorId);
        byte rest[] = new byte[in.available()];
        in.read(rest, 0, rest.length);
        //get the selector....
        int selector = ByteOrder.ushort2int(ByteOrder.leb2short(rest, 0));
        // get the version....
        int version = ByteOrder.ushort2int(ByteOrder.leb2short(rest, 2));
        assertEquals(VendorMessage.F_CRAWLER_PONG, selector);
        assertEquals(UDPCrawlerPong.VERSION, version);
 	}
    
    private static class UDPCrawlerPongParserStub implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, int network) throws BadPacketException {
            return null;
        }
    }
    
    private static class DHTManagerStub implements DHTManager{
        
        public void addBootstrapHost(SocketAddress hostAddress) {}

        public void addressChanged() {}

        public List<IpPort> getActiveDHTNodes(int maxNodes) {return null;}

        public int getDHTVersion() {return 0;}

        public MojitoDHT getMojitoDHT() {return null;}

        public boolean isActiveNode() {return false;}

        public boolean isRunning() {return true;}

        public boolean isWaiting() {return true;}

        public void start(boolean activeMode) {}

        public void stop() {}

        public void switchMode(boolean toActiveMode) {}

        public void handleLifecycleEvent(LifecycleEvent evt) {}
    }
}
