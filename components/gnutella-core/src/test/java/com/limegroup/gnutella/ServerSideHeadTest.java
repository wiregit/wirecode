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
 * tests the server side handling of head pings through UDP.
 */
public class ServerSideHeadTest extends BaseTestCase {
	
	public ServerSideHeadTest(String name) {
		super(name);
	}
	
    public static Test suite() {
        return buildTestSuite(ServerSideHeadTest.class);
    }
    
    static DatagramSocket socket1, socket2;
    static int port1, port2;
    static DatagramPacket datagram1,datagram2,datagram3,datagram4;
    static HeadPing ping1, ping2,ping3,ping4,pingTcp;

    
    public static void globalSetUp() throws Exception {
    	ConnectionSettings.SOLICITED_GRACE_PERIOD.setValue(1000l);
    	ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    	
    	port1 = 10000;
    	port2 = 20000;
    	
    	socket1 = new DatagramSocket(port1);
    	socket2 = new DatagramSocket(port2);

    	socket1.setSoTimeout(300);
    	socket2.setSoTimeout(300);
    	

    	ping1 = new HeadPing(FileManagerStub._notHave);
    	ping2 = new HeadPing(URN.createSHA1Urn(FileDescStub.DEFAULT_URN));
    	ping3 = new HeadPing(URN.createSHA1Urn(FileDescStub.DEFAULT_URN),
    			new Endpoint("127.0.0.1",port2));
    	ping4 = new HeadPing(FileManagerStub._notHave,
    			new Endpoint("127.0.0.1",port1));

    	
    	ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    	ping1.write(baos1);
    	ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    	ping2.write(baos2);
    	ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
    	ping3.write(baos3);
    	ByteArrayOutputStream baos4 = new ByteArrayOutputStream();
    	ping4.write(baos4);
    	
    	datagram1 = new DatagramPacket(baos1.toByteArray(),baos1.toByteArray().length,
    			InetAddress.getLocalHost(),port1);
    	datagram2 = new DatagramPacket(baos2.toByteArray(),baos2.toByteArray().length,
    			InetAddress.getLocalHost(),port2);
    	datagram3 = new DatagramPacket(baos3.toByteArray(),baos3.toByteArray().length,
    			InetAddress.getLocalHost(),port1);
    	datagram4 = new DatagramPacket(baos4.toByteArray(),baos4.toByteArray().length,
    			InetAddress.getLocalHost(),port2);
    	
    	FileManagerStub fmanager = new FileManagerStub();
    	
    	RouterService service = new RouterService(new ActivityCallbackStub());
    	service.start();
    	
    	PrivilegedAccessor.setValue(RouterService.class,"fileManager",fmanager);
    }
    
    public void tearDown() throws Exception {
    	Thread.sleep(ConnectionSettings.SOLICITED_GRACE_PERIOD.getValue());
    }
    
    public static void globalTearDown() throws Exception {
    	socket1.close();
    	socket2.close();
    }
  
    public void testGeneralBehavior() throws Exception{
    	
    	MessageRouter router = RouterService.getMessageRouter();
    	router.handleUDPMessage(ping1,datagram1);
    	Thread.sleep(100);
    	
    	DatagramPacket received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	
    	HeadPong pong = (HeadPong) 
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
    	pong = (HeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping2.getGUID(),pong.getGUID()));
    	
    	//and if we sleep some time, we can send from the first host again
    	Thread.sleep(ConnectionSettings.SOLICITED_GRACE_PERIOD.getValue());
    	
    	router.handleUDPMessage(ping1,datagram1);
    	Thread.sleep(100);
    	received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	pong = (HeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping1.getGUID(),pong.getGUID()));
    }
    
    /**
     * tests the scenario where a ping requests to be sent elsewhere.
     */
    public void testRedirectPing() throws Exception {
    	
    	PrivilegedAccessor.setValue(UDPService.instance(),
    			"_acceptedUnsolicitedIncoming",new Boolean(false));
    	
    	MessageRouter router = RouterService.getMessageRouter();
    	
    	router.handleUDPMessage(ping3,datagram3);
    	Thread.sleep(100);
    	
    	DatagramPacket received = new DatagramPacket(new byte[1024],1024);
    	
    	socket2.receive(received);
    	
    	try{
    		socket1.receive(received);
    		fail("pong sent to wrong place");
    	}catch(IOException expected) {}
    	
    	
    	
    	HeadPong pong = (HeadPong) 
		Message.read(new ByteArrayInputStream(received.getData()));
	
    	assertTrue(Arrays.equals(ping3.getGUID(),pong.getGUID()));
    	
    	//now test a redirect message for a file we do not have.
    	router.handleUDPMessage(ping4,datagram4);
    	Thread.sleep(100);
    	
    	
    	try{
    		socket1.receive(received);
    		fail("pong redirected when file was not found");
    	}catch(IOException expected) {}
    	
    	try{
    		socket2.receive(received);
    		fail("pong sent to wrong place");
    	}catch(IOException expected) {}
    	
    }

}
