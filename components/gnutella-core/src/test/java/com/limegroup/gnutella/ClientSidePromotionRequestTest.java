/*
 * Tests the scenarios where a Promotion Request vendor message
 * arrives at a leaf.
 */
package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.upelection.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;

import java.util.Properties;


import java.net.*;
import java.io.*;

public class ClientSidePromotionRequestTest extends ClientSideTestCase {
	
	
	static DatagramSocket _socket;
	
	static PromotionRequestVendorMessage wrongLeaf, promotingLeaf;
	
	static CountingConnection requestor;
	static ServerSocket _listener;
	
	static MiniAcceptor _acceptor;
	
	
	
	
	static Connection UltrapeerStub = new ManagedConnectionStub() {
		public boolean isGoodUltrapeer() {return true;}
	};
	
	public ClientSidePromotionRequestTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(ClientSidePromotionRequestTest.class);
    }   
	
	public static ActivityCallback getActivityCallback() {
		return new ActivityCallbackStub();
	}
	
	public static Integer numUPs() {return new Integer(1);}
	
	public static void doSettings() throws Exception{
		
		ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
		ConnectionSettings.ENCODE_DEFLATE.setValue(true);
		
		wrongLeaf = new PromotionRequestVendorMessage(
				new RemoteCandidate("1.2.3.4",15,(short)10),
				new RemoteCandidate("1.2.3.4",15,(short)10),
				1);
		promotingLeaf = new PromotionRequestVendorMessage(
				new RemoteCandidate("127.0.0.1",SERVER_PORT,(short)10),
				new RemoteCandidate("127.0.0.1",8000,(short)10),
				2);
		ConnectionSettings.NUM_CONNECTIONS.setValue(32);
		PrivilegedAccessor.setValue(PromotionManager.class,"REQUEST_TIMEOUT",new Long(500));
		
	}
	
	
	/**
	 * tests the scenario where a promotion request arrives that is not for us.
	 */
	public void testWrongLeaf() throws Exception {
		try {
			RouterService.getMessageRouter().handlePromotionRequestVM(wrongLeaf,UltrapeerStub);
		} catch (RuntimeException bad) {
			fail("leaf started promotion process, it shouldn't have");
		}
	}
	
	
	/**
	 * tests the scenario where a leaf receives a promotion request
	 * but the requesting UP fails to ACK it.
	 */
	public void testPromotingTimeout() throws Exception {
		performTest(true);
		assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
	}
	/**
	 * tests the scenario where a leaf receives a promotion request and 
	 * promotes itself.
	 */
	public void testPromotingLeaf() throws Exception {
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		performTest(false);
		assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
		assertTrue(RouterService.isSupernode());
	}
	

	private void performTest(boolean timeout) throws Exception {
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		
		
		PrivilegedAccessor.setValue(
				RouterService.getPromotionManager(),"_promotionPartner",null);
				
//		assert we are a leaf on startup
		assertFalse(RouterService.isSupernode());
		
		assertEquals(32,ConnectionSettings.NUM_CONNECTIONS.getValue());
		
		if (_acceptor == null)
			_acceptor = new MiniAcceptor(new UPResponder(),8000);
		
		Thread.sleep(500);
		try {
			_socket = new DatagramSocket(8000,InetAddress.getByName("127.0.0.1"));
			_socket.setSoTimeout(500);
		}catch(BindException ignore){}
		
		//receive the promotion message
		RouterService.getMessageRouter().handlePromotionRequestVM(promotingLeaf,UltrapeerStub);
			
		//listen for the ping
		DatagramPacket ping = new DatagramPacket(new byte[1000],1000);
		
		_socket.receive(ping);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(ping.getData());
		Message raw = null;
		PromotionACKVendorMessage challenge=null;
		try {
			raw = Message.read(bais);
			challenge = (PromotionACKVendorMessage)raw;
		} catch (BadPacketException bpe) {
			fail(" could not parse the ping ", bpe);
		} catch (ClassCastException ccx) {
			fail(" received class "+raw.getClass(), ccx);
		}
		
		
		//create the remote response
		PromotionACKVendorMessage response = new PromotionACKVendorMessage();
		
		//at this point, if we want to timeout, wait some time.
		
		if(timeout) 
			Thread.sleep(2000);
		
		
		//send it through the proper channel
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		response.write(baos);
		DatagramPacket packet = new DatagramPacket(baos.toByteArray(),baos.toByteArray().length,
					InetAddress.getByName("127.0.0.1"), SERVER_PORT);
		_socket.connect(_socket.getLocalAddress(),SERVER_PORT);
		_socket.send(packet);
		
		//sleep some time, this needs to be longer than the sleeps in 
		//ConnectionManager.becomeAnUp...
		Thread.sleep(2000);
		
	}
	/**
	 * a tiny UP responder which contains only the agent info.
	 *
	 */
	private class UPResponder implements HandshakeResponder {
		public HandshakeResponse respond(HandshakeResponse other, boolean outgoing) throws IOException {
			Properties prop = new Properties();
			prop.setProperty("User-Agent","LimeWire/@version@");
			prop.setProperty("X-Ultrapeer","True");
			return HandshakeResponse.createResponse(prop);
			
		}
		public void setLocalePreferencing(boolean b) {}
	}
	
}
