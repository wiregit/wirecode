package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import junit.framework.*;
import java.io.*;
import java.net.*;

/**
 * Tests the <tt>UDPService</tt> class.  This class requires that a server on
 * is already running that can accept queries.
 */
public class UDPServiceTest extends TestCase {

	//private static final Backend BACKEND_0 = Backend.instance();

	private final ActivityCallback CALLBACK = new ActivityCallbackTest();
	private final MessageRouter ROUTER = new MessageRouterTestStub();

	private Backend BACKEND;

	private RouterService ROUTER_SERVICE;
	
	private final int BUFFER_SIZE = 1024;
	
	private QueryReply _reply = null;

	private PingReply _pingReply = null;

	/**
	 * Constructs a new <tt>UDPServiceTest</tt> instance.
	 */
	public UDPServiceTest(String name) {
		super(name);
	}

	/**
	 * Run this suite of tests.
	 */
	public static Test suite() {
		return new TestSuite(UDPServiceTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	public void setUp() {
		BACKEND = Backend.createLongLivedBackend(CALLBACK, ROUTER);
		ROUTER_SERVICE = BACKEND.getRouterService();
	}

	public void tearDown() {
		BACKEND.shutdown("GUESS CLIENT");
	}


	private void establishConnection() {
		byte[] ipBytes = ROUTER_SERVICE.getAddress();
		String address = Message.ip2string(ipBytes);
		try {
			ROUTER_SERVICE.connectToHostBlocking(address, 6346);
			Thread.sleep(1000);
		} catch(IOException e) {
			fail("unexpected exception: "+e);
		} catch(InterruptedException e) {
			fail("unexpected exception: "+e);
		}
		
		RouterService.disconnect();
	}

	/**
	 * Test that sending pings to GUESS server returns the appropriate pongs.
	 */
	public void testPings() {
		establishConnection();
		PingRequest ping = new PingRequest((byte)1);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ping.write(baos);
			byte[] data = baos.toByteArray();
			InetAddress ip = InetAddress.getLocalHost();
			
			System.out.println("localhost: "+ip); 
			UDPService service = ROUTER_SERVICE.getUdpService();			
			service.send(ping, ip, 6346);
			System.out.println("send ping: "+ping); 
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}

			if(_pingReply == null) {
				fail("PingReply unexpectedly null");
			}
			assertEquals("GUID of pong should equal GUID of ping", ping.getGUID(), 
						 _pingReply.getGUID());
			
		} catch(Throwable t) {
			t.printStackTrace();
			fail("unexpected throwable: "+t);
		}		
	}

	/**
	 * Tests sending query requests to the UDP port.
	 */
	public void testQueryRequests() {
		establishConnection();
		FileManager fm = ROUTER_SERVICE.getFileManager();
		File[] sharedDirs = SettingsManager.instance().getDirectories();
		FileDesc[] fds = fm.getSharedFileDescriptors(sharedDirs[0]);		
		for(int i=0; i<fds.length; i++) {
			String curName = fds[i].getName(); 
			QueryRequest qr = new QueryRequest((byte)1, (byte)0, 
											   curName); 
			PingRequest ping = new PingRequest((byte)1);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ping.write(baos);
				byte[] data = baos.toByteArray();
				InetAddress ip = InetAddress.getLocalHost();
			
				UDPService service = ROUTER_SERVICE.getUdpService();
				service.send(ping, ip, 6346);
				MessageRouter router = ROUTER_SERVICE.getMessageRouter();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				ROUTER_SERVICE.query(ROUTER_SERVICE.newQueryGUID(), 
									 curName, 0, null);
				try {
					while(_reply == null) {
						Thread.sleep(100);
					}
				} catch(InterruptedException e) {
					e.printStackTrace();
				}				
				
				try {
					RemoteFileDesc[] rfds = _reply.toRemoteFileDescArray(true);
					assertEquals("should only be one response in reply", rfds.length, 1);	
					assertEquals("reply name should equal query name", rfds[0].getFileName(),
								 curName);
					_reply = null;
				} catch(BadPacketException e) {
					fail("unexpected exception: "+e);
				}
			} catch(UnknownHostException e) {
				fail("unexpected exception: "+e);
				e.printStackTrace();
			} catch(SocketException e) {
				e.printStackTrace();
				fail("unexpected exception: "+e);
			} catch(IOException e) {
				e.printStackTrace();
				fail("unexpected exception: "+e);
			}		
		}
	}


	/**
	 * Helper class to intercept query hits coming back from the server.
	 */
	private class ActivityCallbackTest extends ActivityCallbackStub {
		public void handleQueryReply(QueryReply reply) {
			_reply = reply;
		}
	}

	/**
	 * Helper class to intercept any desired messages coming back from the server.
	 */
	private class MessageRouterTestStub extends MessageRouterStub {
		public void handleUDPPingReply(PingReply reply, ReplyHandler handler,
									   InetAddress address, int port) {
			super.handleUDPPingReply(reply, handler, address, port);
			_pingReply = reply;
		}
	}
}
