
package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.messages.*;

import junit.framework.Test;

import com.sun.java.util.collections.*;
import java.net.*;
import java.io.*;

/**
 * Tests the issuing of Push Request through udp and failover to tcp.
 */
public class UDPPushTest extends BaseTestCase {
	
	public UDPPushTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(UDPPushTest.class);
    }    
	
	
	
	static RemoteFileDesc rfd1, rfd2;
	
	/**
	 * the socket that will supposedly be the push download
	 */
	static Socket socket;
	
	/**
	 * the socket that will listen for the tcp push request
	 */
	static ServerSocket serversocket;
	
	/**
	 * the socket that will listen for the udp push request
	 */
	static DatagramSocket udpsocket;
	
	public static void globalSetUp() throws Exception {
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.SOLICITED_GRACE_PERIOD.setValue(5000l);
		
		serversocket = new ServerSocket(10000);
		
		serversocket.setSoTimeout(1000);
		
		udpsocket = new DatagramSocket(20000);
		udpsocket.setSoTimeout(1000);
				
		ActivityCallback ac = new ActivityCallbackStub();
		RouterService rs = new RouterService(ac);
		rs.start();
		
	}
	
	public void setUp() {
		long now = System.currentTimeMillis();
		
		Set proxies = new HashSet();
		proxies.add(new PPI());
		LimeXMLDocument doc = null;
		Set urns = null;
		

		
		rfd1 = new RemoteFileDesc(
				"127.0.0.1",20000,30l,"file1",
				100,GUID.makeGuid(),SpeedConstants.CABLE_SPEED_INT,
				false,1,false,
				doc,urns,
				false,true,
				"LIME",now,
				proxies,now);
		
		rfd2 = new RemoteFileDesc(
				"127.0.0.1",20000,31l,"file2",
				100,GUID.makeGuid(),SpeedConstants.CABLE_SPEED_INT,
				false,1,false,
				doc,urns,
				false,true,
				"LIME",now,
				proxies,now);

	}
	
	/**
	 * tests the scenario where we try to send a push to a host
	 * that did not reply through oob
	 */
	public void testNoUDPPush() throws Exception {
		assertFalse(rfd1.canPushUDP());
		
		requestPush(rfd1);
		
		DatagramPacket empty = new DatagramPacket(new byte[1000],1000);
		try {
			udpsocket.receive(empty);
			fail("received something? "+new String(empty.getData()));
		}catch(IOException expected){}
		
		Socket s = serversocket.accept();
		
		assertTrue(s.isConnected());
		s.close();
		
	}
	
	/**
	 * tests the scenario where an udp push is sent, but no
	 * connection is received so the failover tcp push is sent.
	 */
	public void testUDPPushFailover() throws Exception {
		Endpoint us = new Endpoint("127.0.0.1",20000);
		rfd1.setOOBStatus(System.currentTimeMillis(),us);
		
		assertTrue(rfd1.canPushUDP());
		
		requestPush(rfd1);
		
		try {
			serversocket.accept();
			fail("tcp attempt was made");
		}catch(IOException expected){}
		
		DatagramPacket push = new DatagramPacket(new byte[1000],1000);
		udpsocket.receive(push);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
		PushRequest pr = (PushRequest)Message.read(bais);
		assertEquals(rfd1.getIndex(),pr.getIndex());
		
		Thread.sleep(4500);
		
		
		Socket s = serversocket.accept();
		assertTrue(s.isConnected());

		
	}
	
	
	static void requestPush(final RemoteFileDesc rfd) throws Exception{
		Thread t = new Thread() {
			public void run() {
				RouterService.getDownloadManager().sendPush(rfd);
			}
		};
		t.start();
		Thread.sleep(100);
	}
	
	static class PPI implements PushProxyInterface {
		public int getPushProxyPort() {
			return 10000;
		}
		
		public InetAddress getPushProxyAddress() {
			try{
				return InetAddress.getLocalHost();
			}catch(UnknownHostException uhe) {
				return null;
			}
		}
	}

}
