/*
 * Tests the scenarios where a Promotion Request vendor message
 * arrives at a leaf.
 */
package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.messages.vendor.*;

public class ClientSidePromotionRequestTest extends ClientSideTestCase {
	
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
				new QueryReply.IPPortCombo("1.2.3.4",15),
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
		try {
			RouterService.getMessageRouter().handlePromotionRequestVM(promotingLeaf,UltrapeerStub);
			fail("leaf should have started promotion process");
		} catch (RuntimeException expected) {}
	}
}
