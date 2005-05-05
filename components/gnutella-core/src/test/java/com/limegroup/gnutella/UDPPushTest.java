
package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.xml.LimeXMLDocument;

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
	
	
	
	static RemoteFileDesc rfd1, rfd2,rfdAlt;
	
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
	
	
	static byte [] guid = GUID.fromHexString("BC1F6870696111D4A74D0001031AE043");
	
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
		try{
			Map map = (Map)
				PrivilegedAccessor.getValue(RouterService.getDownloadManager(),
					"UDP_FAILOVER");
			map.clear();
		}catch(Exception tough){tough.printStackTrace();}
		
		long now = System.currentTimeMillis();
		
		Set proxies = new TreeSet(IpPort.COMPARATOR);
        try {
            proxies.add(new IpPortImpl(InetAddress.getLocalHost().getHostAddress(),10000));
        } catch (UnknownHostException bad) {
            ErrorService.error(bad);
        }
		LimeXMLDocument doc = null;
		Set urns = null;
		

		
		rfd1 = new RemoteFileDesc(
				"127.0.0.1",20000,30l,"file1",
				100,guid,SpeedConstants.CABLE_SPEED_INT,
				false,1,false,
				doc,urns,
				false,true,
				"LIME",now,
				proxies,now);
		
		rfd2 = new RemoteFileDesc(
				"127.0.0.1",20000,31l,"file2",
				100,guid,SpeedConstants.CABLE_SPEED_INT,
				false,1,false,
				doc,urns,
				false,true,
				"LIME",now,
				proxies,now);
		
		rfdAlt = new RemoteFileDesc(
				"127.0.0.1",20000,30l,"file1",
				100,guid,SpeedConstants.CABLE_SPEED_INT,
				false,1,false,
				doc,urns,
				false,true,
				"ALT",now,
				proxies,now);
		
		Acceptor acc = RouterService.getAcceptor();
		try{
			PrivilegedAccessor.setValue(acc,"_acceptedIncoming",new Boolean(true));
		}catch(Exception bad) {
			ErrorService.error(bad);
		}

		assertTrue(RouterService.acceptedIncomingConnection());
	}
	
	
	/**
	 * tests the scenario where an udp push is sent, but no
	 * connection is received so the failover tcp push is sent.
	 */
	public void testUDPPushFailover() throws Exception {
		
		
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
		
		Thread.sleep(5200);
		
		
		Socket s = serversocket.accept();
		assertTrue(s.isConnected());s.close();

		
	}
	
	/**
	 * tests the scenario where an udp push is sent, no
	 * connection is received but since we're trying to contact
	 * an altloc no failover tcp push is sent.
	 */
	public void testUDPPushFailoverAlt() throws Exception {
		
		
		requestPush(rfdAlt);
		
		try {
			serversocket.accept();
			fail("tcp attempt was made");
		}catch(IOException expected){}
		
		DatagramPacket push = new DatagramPacket(new byte[1000],1000);
		udpsocket.receive(push);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
		PushRequest pr = (PushRequest)Message.read(bais);
		assertEquals(rfd1.getIndex(),pr.getIndex());
		
		Thread.sleep(5200);
		
		try {
			Socket s =serversocket.accept();
			s.close();
			fail("tcp attempt was made");
		}catch(IOException expected){}
		Thread.sleep(3000);
		
	}
	
	/**
	 * tests the scenario where an UDP push is sent and
	 * a connection is established, so no failover occurs.
	 */
	public void testUDPPush() throws Exception {

		
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
		
		socket = new Socket(InetAddress.getLocalHost(),10000);
		Socket other = serversocket.accept();
		
		
		assertEquals(InetAddress.getLocalHost(),socket.getInetAddress());
		assertEquals(10000,socket.getPort());
		
		sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file1\n\n");
		
		
		RouterService.getDownloadManager().acceptDownload(socket);
		other.close();
		
		Thread.sleep(5000);
		
		try {
			Socket s =serversocket.accept();
			s.close();
			fail("tcp attempt was made");
		}catch(IOException expected){}
		Thread.sleep(3000);
		
	}
	
	/**
	 * tests the scenario where two pushes are made to the same host
	 * for different files and both succeed.
	 */
	public void testTwoPushesBothGood() throws Exception{


		
		requestPush(rfd1);
		requestPush(rfd2);
		
		try {
			serversocket.accept();
			fail("tcp attempt was made");
		}catch(IOException expected){}
		
		DatagramPacket push = new DatagramPacket(new byte[1000],1000);
		udpsocket.receive(push);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
		PushRequest pr = (PushRequest)Message.read(bais);
		assertEquals(rfd1.getIndex(),pr.getIndex());
		
		udpsocket.receive(push);
		bais = new ByteArrayInputStream(push.getData());
		pr = (PushRequest)Message.read(bais);
		assertEquals(rfd2.getIndex(),pr.getIndex());
		
		Thread.sleep(2000);
		
		socket = new Socket(InetAddress.getLocalHost(),10000);
		Socket other = serversocket.accept();
		
		sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file1\n\n");
		RouterService.getDownloadManager().acceptDownload(socket);
		socket.close();
		
		socket = new Socket(InetAddress.getLocalHost(),10000);
		other = serversocket.accept();
		socket.setSoTimeout(1000);
		sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file2\n\n");
		RouterService.getDownloadManager().acceptDownload(socket);
		socket.close();
		Thread.sleep(5200);
		
		try {
			serversocket.accept();
			fail("tcp attempt was made");
		}catch(IOException expected){}
	}
	
	public void testTwoPushesOneFails() throws Exception {

		
		requestPush(rfd1);
		requestPush(rfd2);
		
		try {
			serversocket.accept();
			fail("tcp attempt was made");
		}catch(IOException expected){}
		
		DatagramPacket push = new DatagramPacket(new byte[1000],1000);
		udpsocket.receive(push);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
		PushRequest pr = (PushRequest)Message.read(bais);
		assertEquals(rfd1.getIndex(),pr.getIndex());
		
		udpsocket.receive(push);
		bais = new ByteArrayInputStream(push.getData());
		pr = (PushRequest)Message.read(bais);
		assertEquals(rfd2.getIndex(),pr.getIndex());
		
		Thread.sleep(2000);
		
		socket = new Socket(InetAddress.getLocalHost(),10000);
		Socket other = serversocket.accept();
		
		sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file1\n\n");
		RouterService.getDownloadManager().acceptDownload(socket);
		socket.close();
		
		Thread.sleep(5200);
		
		
		serversocket.accept().close();
			
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
	
	static void sendGiv(final Socket sock,final String str) {
		Thread t = new Thread() {
			public void run()  {
				try {
					sock.getOutputStream().write(str.getBytes());
				}catch(IOException e) {
					fail(e);
				}
			}
		};
		t.start();
	}
}
