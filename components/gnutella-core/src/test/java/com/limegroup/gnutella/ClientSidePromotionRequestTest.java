/*
 * Tests the scenarios where a Promotion Request vendor message
 * arrives at a leaf.
 */
package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.messages.vendor.*;

import java.net.*;
import java.io.*;

public class ClientSidePromotionRequestTest extends ClientSideTestCase {
	
	static DatagramSocket _socket;
	
	static PromotionRequestVendorMessage wrongLeaf, promotingLeaf;
	
	static ReplyHandler UltrapeerStub = new ReplyHandlerStub() {
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
		wrongLeaf = new PromotionRequestVendorMessage(
				new QueryReply.IPPortCombo("1.2.3.4",15),
				new QueryReply.IPPortCombo("1.2.3.4",15),
				1);
		promotingLeaf = new PromotionRequestVendorMessage(
				new QueryReply.IPPortCombo("127.0.0.1",SERVER_PORT),
				new QueryReply.IPPortCombo("127.0.0.1",8000),
				2);
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
	
	public void testPromotingLeaf() throws Exception {
		
		_socket = new DatagramSocket(8000);
		_socket.setSoTimeout(500);
		
		//send the promotion message
		RouterService.getMessageRouter().handlePromotionRequestVM(promotingLeaf,UltrapeerStub);
			
		//listen for the ping
		DatagramPacket ping = new DatagramPacket(new byte[1000],1000);
		
		_socket.receive(ping);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(ping.getData());
		LimeACKVendorMessage challenge=null;
		try {
			challenge = (LimeACKVendorMessage)Message.read(bais);
		} catch (BadPacketException bpe) {
			fail(" could not parse the ping", bpe);
		}
		
		//verify the ack request
		assertEquals(new String(promotingLeaf.getGUID()), new String(challenge.getGUID()));
		assertEquals(0, challenge.getNumResults());
		
		//create our response
		LimeACKVendorMessage response = new LimeACKVendorMessage(new GUID(challenge.getGUID()),0);
		
		//call the handle method with the wrong datagram - shouldn't matter.
		//note we call the main handle method, otherwise the listener won't get notified.
		try {
			RouterService.getMessageRouter().handleUDPMessage(response,ping);
			fail("should have thrown.");
		}catch (RuntimeException expected) {
			
		}
	}
}
