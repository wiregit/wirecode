
package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.messages.*;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * tests the server side handling of UDP head pings.
 */
public class ServerSideUDPHeadTest extends BaseTestCase {
	
	public ServerSideUDPHeadTest(String name) {
		super(name);
	}
	
    public static Test suite() {
        return buildTestSuite(ServerSideUDPHeadTest.class);
    }
    
    static DatagramSocket socket1, socket2;
    static int port1, port2;
    static DatagramPacket datagram1,datagram2;
    static UDPHeadPing ping1, ping2;
    
    public static void globalSetUp() throws Exception {
    	ConnectionSettings.SOLICITED_GRACE_PERIOD.setValue(1000);
    	ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    	
    	port1 = 10000;
    	port2 = 20000;
    	
    	socket1 = new DatagramSocket(port1);
    	socket2 = new DatagramSocket(port2);
    	socket1.connect(InetAddress.getLocalHost(),6346);
    	socket1.setSoTimeout(300);
    	socket2.connect(InetAddress.getLocalHost(),6346);
    	socket2.setSoTimeout(300);
    	
    	ping1 = new UDPHeadPing(FileManagerStub._notHave);
    	ping2 = new UDPHeadPing(URN.createSHA1Urn(FileDescStub.urn));
    	
    	ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    	ping1.write(baos1);
    	ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    	ping2.write(baos2);
    	datagram1 = new DatagramPacket(baos1.toByteArray(),baos1.toByteArray().length,
    			InetAddress.getLocalHost(),port1);
    	datagram2 = new DatagramPacket(baos2.toByteArray(),baos2.toByteArray().length,
    			InetAddress.getLocalHost(),port2);
    	
    	RouterService service = new RouterService(new ActivityCallbackStub());
    	service.start();
    }
    
    public void testGeneralBehavior() throws Exception{
    	
    	MessageRouter router = RouterService.getMessageRouter();
    	router.handleUDPMessage(ping1,datagram1);
    	Thread.sleep(100);
    	
    	DatagramPacket received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	
    	UDPHeadPong pong = (UDPHeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping1.getGUID(),pong.getGUID()));
    	
    	// send second message, from the same host
    	router.handleUDPMessage(ping1,datagram1);
    	Thread.sleep(100);
    	
    	received = new DatagramPacket(new byte[1024],1024);
    	try {
    		socket1.receive(received);
    		fail("should not have sent second pong");
    	}catch(IOException expected){}
    	
		
		//but if we try to send from the other host, it works.. 
    	//first drain the ping request
		router.handleUDPMessage(ping2,datagram2);
		Thread.sleep(100);

    	received = new DatagramPacket(new byte[1024],1024);
    	socket2.receive(received);
    	pong = (UDPHeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping2.getGUID(),pong.getGUID()));
    	
    	//and if we sleep some time, we can send from the first host again
    	Thread.sleep(ConnectionSettings.SOLICITED_GRACE_PERIOD.getValue());
    	
    	router.handleUDPMessage(ping1,datagram1);
    	Thread.sleep(100);
    	received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	pong = (UDPHeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping1.getGUID(),pong.getGUID()));
    }

}
