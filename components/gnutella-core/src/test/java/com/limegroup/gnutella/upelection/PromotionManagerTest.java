package com.limegroup.gnutella.upelection;

import java.net.InetAddress;

import com.limegroup.gnutella.util.BaseTestCase;


import junit.framework.Test;
import com.sun.java.util.collections.*;

import java.net.*;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;

/**
 * Tests the functionality of the PromotionManager.
 */
public class PromotionManagerTest extends BaseTestCase {
	
	private static final long NEW_TIMEOUT = 300;
	
	static UDPSendingStub _udpStub;
	static PromotionManager _manager;
	
	static IpPort _partner1, _partner2;
	
	static PromotionRequestVendorMessage _prvm;
	
	static PromotionACKVendorMessage _ack;
	
	static GUID _guid = new GUID(GUID.makeGuid());
	
	static BestCandidates _bestCandidates =BestCandidates.instance();
	
	public PromotionManagerTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(PromotionManagerTest.class);
    }
	
	
	/**
	 * reduce the timeouts and stub out UDPService.
	 *
	 */
	public static void globalSetUp() {
		
		try {
			PrivilegedAccessor.setValue(PromotionManager.class,"UP_REQUEST_TIMEOUT",
				new Long(NEW_TIMEOUT));
			PrivilegedAccessor.setValue(PromotionManager.class,"LEAF_REQUEST_TIMEOUT",
					new Long(NEW_TIMEOUT));
			
			_udpStub = new UDPSendingStub();
			PrivilegedAccessor.setValue(PromotionManager.class,"_udpService",_udpStub);
			
		
			RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
		}catch(Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		
		_partner1 = new IpPortPair("1.2.3.1",15);
		_partner2 = new IpPortPair("1.2.3.2",15);
		
		_ack = new PromotionACKVendorMessage(_guid);
	}
	
	/**
	 * reset the promotion manager before each test.
	 */
	public void setUp() {
		_manager = new PromotionManager();
		_udpStub._lastDest = null;
		_udpStub._lastSent = null;
	}
	
	/**
	 * kill the expiring thread on teardown.
	 */
	public void tearDown(){
		
		try{
			Object expirerObj = PrivilegedAccessor.getValue(_manager,"_expirer");
			if (expirerObj != null)
				PrivilegedAccessor.setValue(expirerObj,"_cancelled",new Boolean(true));
			
		}catch(Exception e){e.printStackTrace();}
	}
	
	
	public void testInitiatePromotion() throws Exception {
		
		assertFalse(_manager.isPromoting());
		assertFalse(_manager.isPromoting(_partner2));
		
		_prvm = new PromotionRequestVendorMessage (
				new RemoteCandidate("1.2.3.1",15,(short)20),
				new RemoteCandidate("1.2.3.2",15,(short)20),
				0);
		
		_manager.initiatePromotion(_prvm);
		
		assertTrue(_manager.isPromoting());
		assertTrue(_manager.isPromoting(_partner2));
		
		//we should receive something on the UDP stub.
		assertEquals(PromotionACKVendorMessage.class,_udpStub._lastSent.getClass());
		assertTrue(_partner2.isSame(_udpStub._lastDest));
		
		
		_udpStub._lastDest=null;
		_udpStub._lastSent=null;
		
		//half a timeout later, someone else requests a promotion
		
		Thread.sleep(NEW_TIMEOUT/2);
		
		_prvm = new PromotionRequestVendorMessage (
				new RemoteCandidate("1.2.3.2",15,(short)20),
				new RemoteCandidate("1.2.3.1",15,(short)20),
				0);
		
		_manager.initiatePromotion(_prvm);
		
		//it should not be acted to since we're being promoted.
		assertNull(_udpStub._lastDest);
		assertNull(_udpStub._lastSent);
		
		assertTrue(_manager.isPromoting());
		assertTrue(_manager.isPromoting(_partner2));
		
		
		//the request times out and we try to promote the second message again
		//this time succesfully
		Thread.sleep(NEW_TIMEOUT);
		
		_manager.initiatePromotion(_prvm);
		assertNotNull(_udpStub._lastDest);
		assertNotNull(_udpStub._lastSent);
		
		assertTrue(_manager.isPromoting());
		assertTrue(_manager.isPromoting(_partner1));
		
		assertTrue(_partner1.isSame(_udpStub._lastDest));
		assertEquals(PromotionACKVendorMessage.class,_udpStub._lastSent.getClass());
		
	}
	
	/**
	 * tests the funcitonality of the RequestPromotion method.
	 */
	public void testRequestPromotion() throws Exception {
		
		// first, replace the message router with a fake one
		NotForwardingRouter fake = new NotForwardingRouter();	
		PrivilegedAccessor.setValue(RouterService.class,"router",fake);
		
		//create some fake BestCandidates
		Candidate [] update = new Candidate[2];
		
		update [0] = new RemoteCandidate("1.2.3.1",15,(short)4);
		update [1] = new RemoteCandidate("1.2.3.2",15,(short)2);
		ManagedConnectionStub stubAdv = new ManagedConnectionStub();
		update[0].setAdvertiser(stubAdv);
		update[1].setAdvertiser(stubAdv);
		
		_bestCandidates.update(update);
		
		assertTrue(_partner1.isSame(_bestCandidates.getBest()));
		
		assertFalse(_manager.isPromoting());
		
		//request a promotion
		_manager.requestPromotion();
		
		assertTrue(_manager.isPromoting());
		assertTrue(_manager.isPromoting(_partner1));
		
		assertTrue(fake.forwarded);
		
		//now, try to request another promotion within the interval.
		
		update[1] = new RemoteCandidate("1.2.3.2",15,(short)10);
		update[1].setAdvertiser(stubAdv);
		_bestCandidates.update(update);
		
		assertTrue(_partner2.isSame(_bestCandidates.getBest()));
		fake.forwarded=false;
		
		Thread.sleep(NEW_TIMEOUT/2);
		
		_manager.requestPromotion();
		
		//nothing should have changed.
		assertFalse(fake.forwarded);
		assertTrue(_manager.isPromoting());
		assertTrue(_manager.isPromoting(_partner1));
		
		//now try again after the interval
		Thread.sleep(NEW_TIMEOUT);
		
		_manager.requestPromotion();
		
		//partner2 should be promoting now.
		assertTrue(fake.forwarded);
		assertTrue(_manager.isPromoting());
		assertTrue(_manager.isPromoting(_partner2));
		
	}
	
	/**
	 * tests the functionality of the handleACK method.
	 */
	public void testHandleACK() throws Exception {
		
		//stub out connection manager and MessageRouter
		NotPromotingManager fake = new NotPromotingManager();
		fake.setSupernode(false);
		
		PrivilegedAccessor.setValue(RouterService.class,"manager",fake);
		assertFalse(RouterService.isSupernode());
		
		
		//1. test when we receive an ACK but are not partnering with anybody.
		
		assertFalse(_manager.isPromoting(_partner1));
		_manager.handleACK(_partner1,_guid);
		
		Thread.sleep(100);
		
		assertFalse(fake.called);
		
		//2.  test when are receive an ACK but the promotion partner is different
		
		PrivilegedAccessor.setValue(_manager,"_promotionPartner",_partner2);
		assertTrue(_manager.isPromoting());
		assertTrue(_manager.isPromoting(_partner2));
		
		_manager.handleACK(_partner1,_guid);
		
		Thread.sleep(NEW_TIMEOUT/3);
		
		assertFalse(fake.called);
		
		//3. test when we receive an ACK with wrong guid
		_manager.handleACK(_partner2,_guid);
		
		Thread.sleep(NEW_TIMEOUT/3);
		
		assertFalse(fake.called);
		
		//4.  test when we receive a proper ACK as a leaf
		PrivilegedAccessor.setValue(_manager,"_guid",_guid);
		_manager.handleACK(_partner2,_guid);
		
		Thread.sleep(NEW_TIMEOUT/3);
		
		assertTrue(fake.called);
		
		//5. test when we receive a proper ACK as an UP
		
		Thread.sleep(NEW_TIMEOUT);
		fake.setSupernode(true);
		assertTrue(RouterService.isSupernode());
		
		
		PrivilegedAccessor.setValue(_manager,"_promotionPartner",_partner1);
		assertTrue(_manager.isPromoting(_partner1));
		
		_manager.handleACK(_partner1,_guid);
		
		Thread.sleep(NEW_TIMEOUT/3);
		assertEquals(PromotionACKVendorMessage.class,_udpStub._lastSent.getClass());
		assertEquals(_guid, new GUID(_udpStub._lastSent.getGUID()));
		assertTrue(_partner1.isSame(_udpStub._lastDest));
	}
	
	static class UDPSendingStub extends UDPService {
		
		public Message _lastSent;
		public IpPort _lastDest;
		
		public void send (Message msg, IpPort address) {
			_lastSent=msg;
			_lastDest=address;
		}
		
		public void run(){} //do nothing.
	}
	
	static class NotForwardingRouter extends MessageRouterStub {
		public boolean forwarded = false;
		public void forwardPromotionRequest(PromotionRequestVendorMessage prvm) {
			if (prvm != null)
				forwarded = true;
		}
	}
	
	static class NotPromotingManager extends ConnectionManagerStub {
		public boolean called = false;
		public void becomeAnUPWithBackupConn(IpPort target) {
			called = true;
		}
		
	}
}
