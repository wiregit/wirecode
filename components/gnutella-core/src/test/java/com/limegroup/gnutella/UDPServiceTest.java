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
	private final Backend BACKEND = 
		Backend.createBackend(CALLBACK, 40*1000);

	private final RouterService ROUTER_SERVICE = BACKEND.getRouterService();
	
	private final int BUFFER_SIZE = 1024;
	
	private QueryReply _reply = null;

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

	/**
	 * Tests sending query requests to the UDP port.
	 */
	public void testQueryRequests() {
		FileManager fm = ROUTER_SERVICE.getFileManager();
		File[] sharedDirs = SettingsManager.instance().getDirectories();
		FileDesc[] fds = fm.getSharedFileDescriptors(sharedDirs[0]);


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
					RemoteFileDesc[] rfds = _reply.toRemoteFileDescArray();
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

		BACKEND.shutdown("GUESS CLIENT");
	}


	/**
	 * Helper class to intercept query hits coming back from the server.
	 */
	private class ActivityCallbackTest extends ActivityCallbackStub {
		public void handleQueryReply(QueryReply reply) {
			_reply = reply;
		}
	}
}
