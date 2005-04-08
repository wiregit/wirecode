package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

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
    static InetSocketAddress addr1, addr2;
    static HeadPing ping1, ping2;

    
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


    	
    	ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    	ping1.write(baos1);
    	ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    	ping2.write(baos2);
    	
    	addr1 = new InetSocketAddress(InetAddress.getLocalHost(), port1);
    	addr2 = new InetSocketAddress(InetAddress.getLocalHost(), port2);



    	
    	FileManagerStub fmanager = new FileManagerStub();
    	
    	RouterService service = new RouterService(new ActivityCallbackStub());
    	service.start();
    	
    	PrivilegedAccessor.setValue(RouterService.class,"fileManager",fmanager);
    }
    
    public void tearDown() throws Exception {
    //	Thread.sleep(ConnectionSettings.SOLICITED_GRACE_PERIOD.getValue());
    }
    
    public static void globalTearDown() throws Exception {
    	socket1.close();
    	socket2.close();
    }
  
    public void testGeneralBehavior() throws Exception{
    	
    	MessageRouter router = RouterService.getMessageRouter();
    	router.handleUDPMessage(ping1,addr1);
    	Thread.sleep(100);
    	
    	DatagramPacket received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	
    	HeadPong pong = (HeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping1.getGUID(),pong.getGUID()));
    	
		router.handleUDPMessage(ping2,addr2);
		Thread.sleep(100);

    	received = new DatagramPacket(new byte[1024],1024);
    	socket2.receive(received);
    	pong = (HeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping2.getGUID(),pong.getGUID()));
    	
    	//and if we sleep some time, we can send from the first host again
    	Thread.sleep(ConnectionSettings.SOLICITED_GRACE_PERIOD.getValue());
    	
    	router.handleUDPMessage(ping1,addr1);
    	Thread.sleep(100);
    	received = new DatagramPacket(new byte[1024],1024);
    	socket1.receive(received);
    	pong = (HeadPong) 
			Message.read(new ByteArrayInputStream(received.getData()));
    	
    	assertTrue(Arrays.equals(ping1.getGUID(),pong.getGUID()));
    }
    
}
