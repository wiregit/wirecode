package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import junit.framework.*;
import java.io.*;
import java.net.*;

/**
 * Tests the <tt>UDPService</tt> class.
 */
public class UDPServiceTest extends TestCase {

	//private static final Backend BACKEND_0 = Backend.instance();

	private final ActivityCallback CALLBACK = new ActivityCallbackTest();
	private final Backend BACKEND = 
		Backend.createBackend(CALLBACK);

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
		//System.out.println("TESTING UDP QUERY REQUESTS"); 
		FileManager fm = ROUTER_SERVICE.getFileManager();
		File[] sharedDirs = SettingsManager.instance().getDirectories();
		FileDesc[] fds = fm.getSharedFileDescriptors(sharedDirs[0]);


		byte[] ipBytes = ROUTER_SERVICE.getAddress();
		String address = Message.ip2string(ipBytes);
		ROUTER_SERVICE.connectToHostAsynchronously(address, 6346);
		
		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
		}
		//System.out.println("GUESS ENABLED: "+
		//			   SettingsManager.instance().getGuessEnabled()); 
		for(int i=0; i<fds.length; i++) {
			String curName = fds[i].getName(); 
			//System.out.println("TESTING FILE: "+curName); 
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
				//router.broadcastPingRequest(new PingRequest((byte)1));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				router.broadcastQueryRequest(qr);
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
				e.printStackTrace();
			} catch(SocketException e) {
				e.printStackTrace();
			} catch(IOException e) {
				e.printStackTrace();
			}		
		}

		//System.out.println("ABOUT TO SHUTDOWN ROUTER SERVICE"); 
		//BACKEND.shutdown();
	}


	private class ActivityCallbackTest extends ActivityCallbackStub {
		public void handleQueryReply(QueryReply reply) {
			//System.out.println(""); 
			//System.out.println(""); 
			//System.out.println("RECEIVED REPLY: "+reply); 
			_reply = reply;
		}
	}
}
