package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import java.net.*;

/**
 * Tests the <tt>UDPAcceptor</tt> class.
 */
public class UDPAcceptorTest extends TestCase {

	private static final Backend BACKEND_1 = Backend.instance();

	private static final Backend BACKEND_2 = Backend.instance();
	
	private final int BUFFER_SIZE = 1024;

	/**
	 * Constructs a new <tt>UDPAcceptorTest</tt> instance.
	 */
	public UDPAcceptorTest(String name) {
		super(name);
	}

	/**
	 * Run this suite of tests.
	 */
	public static Test suite() {
		return new TestSuite(UDPAcceptorTest.class);
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
		FileManager fm = BACKEND_1.getFileManager();
		File[] sharedDirs = SettingsManager.instance().getDirectories();
		FileDesc[] fds = fm.getSharedFileDescriptors(sharedDirs[0]);
		QueryRequest qr = new QueryRequest(GUID.makeGuid(),
										   (byte)6, (byte)4, 
                                           (new String("Huge")).getBytes());//fds[0].getName()); 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			qr.write(baos);
			byte[] data = baos.toByteArray();
			InetAddress ip = InetAddress.getLocalHost();
			UDPAcceptor acceptor1 = BACKEND_1.getUdpAcceptor();
			UDPAcceptor acceptor2 = BACKEND_2.getUdpAcceptor();
			DatagramSocket socket1 = acceptor1.getDatagramSocket();
			DatagramSocket socket2 = acceptor2.getDatagramSocket();
			//DatagramSocket socket = new DatagramSocket(acceptor1.getPort(), ip);

			//byte[] datagramBytes = new byte[BUFFER_SIZE];
			//DatagramPacket incomingDatagram = 
			//  new DatagramPacket(datagramBytes, BUFFER_SIZE);
			//socket.receive(incomingDatagram);
			

			DatagramPacket outgoingDatagram = 
			    new DatagramPacket(data, data.length, ip, socket2.getLocalPort());
			socket1.send(outgoingDatagram);
		} catch(UnknownHostException e) {
			e.printStackTrace();
		} catch(SocketException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}		
	}
}
