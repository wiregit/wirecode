
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.*;

import com.sun.java.util.collections.*;

import junit.framework.Test;

/**
 * tests the handling of PromotionRequestVendorMessages by Message Router.
 * 
 * As UP:
 * 1. Promotion request arrives from a leaf.
 * 2. Promotion request arrives at hops 0 and the ip of the requestor
 *    doesn't match the ip in the message
 * 3. A message arrives that has travelled 2 hops
 * 4. Promotion request arrives and is forwarded to a leaf
 * 5. Promotion request arrives and is forwarded to an UP.
 * 
 * As LEAF:
 * 1. Promotion request arrives that is not intended for us
 * 2. Promotion request arrives that is intended for us. 
 */
public class PRVMHandlingTest extends BaseTestCase {
	
	static long NEW_DELAY= 300;
	
	/**
	 * a connection manager stub
	 */
	static ConnectionManagerStub _manager;
	
	/**
	 * a leaf connection
	 */
	static GoodLeafCandidate _leaf;
	
	/**
	 * a promotion manager stub
	 */
	static PromotionManagerStub _promoter;
	
	/**
	 * two ultrapeers.
	 */
	static UPStub _up1, _up2;
	
	/**
	 * couple of remote candidates
	 */
	static Candidate _up1_ttl1, _up2_ttl2;
	
	static BestCandidates _bestCandidates = BestCandidates.instance();
	
	public PRVMHandlingTest(String str){
		super(str);
	}
	
	public static Test suite() {
        return buildTestSuite(PRVMHandlingTest.class);
    }
	
	/**
	 * create the various objects:  
	 * 
	 * The Best Candidates routing table contains:
	 * 
	 * TTL 0 - _leaf, TTL 1 - _up1_ttl1, TTL 2- _up2_ttl2;
	 * 
	 * Candidate _up1_ttl1 is advertised by _up1, and _up2_ttl2 by _up2.
	 * 
	 */
	public static void globalSetUp() throws Exception {
		
		PrivilegedAccessor.setValue(CandidateAdvertiser.class,"INITIAL_DELAY", new Long(NEW_DELAY));
		
		_manager = new ConnectionManagerStub();
		
		_leaf = new GoodLeafCandidate("1.1.1.1", 15,(short)30,20);
		
		_up1 = new UPStub("2.2.2.2",15);
		_up2 = new UPStub("3.3.3.3",15);
		
		_up1_ttl1 = new RemoteCandidate("2.2.2.3",15,(short)40);
		_up2_ttl2 = new RemoteCandidate("3.3.3.4",15,(short)20);
		
		_up1_ttl1.setAdvertiser(_up1);
		_up2_ttl2.setAdvertiser(_up2);
		
		List l = new LinkedList();
		List l2 = new LinkedList();
		
		l.add(_up1);l.add(_up2);
		
		_manager.setInitializedConnections(l);
		
		l2.add(_leaf);
		_manager.setInitializedClientConnections(l2);
		
		PrivilegedAccessor.setValue(RouterService.class,"manager",_manager);
		
		_promoter = new PromotionManagerStub();
		
		
		PrivilegedAccessor.setValue(RouterService.class,"router", new StandardMessageRouter());
		
		PrivilegedAccessor.setValue(RouterService.getMessageRouter(),"_promotionManager",_promoter);
		
		
	}
	
	/**
	 * make sure the table gets updated with our best candidate.
	 */
	public void setUp() {
		try {
			
			Candidate []candidates = new Candidate[3];
			candidates[0]=_leaf;
			candidates[1]=_up1_ttl1;
			candidates[2]=_up2_ttl2;
			
			PrivilegedAccessor.setValue(_bestCandidates,"_best",candidates);
			
			assertTrue(_up1_ttl1.isSame(_bestCandidates.getBest()));
			
			_up1.setLastSent(null);
			_up2.setLastSent(null);
			_leaf.setLastSent(null);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Tests when a request arrives at a leaf that is not
	 * intended for that leaf.
	 */
	public void testLeafNotForUs()throws Exception{
		
		_manager.setSupernode(false);
		assertFalse(RouterService.isSupernode());
		
		assertFalse(_promoter.promoted);
		
		PromotionRequestVendorMessage notUs = new PromotionRequestVendorMessage (
				_up1_ttl1,_up2_ttl2,1);
		
		RouterService.getMessageRouter().handlePromotionRequestVM(notUs, _up1);
		
		assertFalse(_promoter.promoted);
		_manager.setSupernode(true);
	}
	
	/**
	 * tests a scenario where a request arrives at a leaf and it gets promoted.
	 */
	public void testLeafForUs() throws Exception {
		_manager.setSupernode(false);
		assertFalse(RouterService.isSupernode());
		
		assertFalse(_promoter.promoted);
		PromotionRequestVendorMessage us = new PromotionRequestVendorMessage (
				new RemoteCandidate("127.0.0.1",
						RouterService.getPort(), (short)1),
				_up1_ttl1,1);
		
		RouterService.getMessageRouter().handlePromotionRequestVM(us, _up1);
		assertTrue(_promoter.promoted);
		_manager.setSupernode(true);
	}
	
	/**
	 * tests the scenario where the request arrives from a leaf
	 */
	public void testRequestFromLeaf() throws Exception {
		
		assertTrue(RouterService.isSupernode());
		
		PromotionRequestVendorMessage fromLeaf = 
			new PromotionRequestVendorMessage(_up2_ttl2,_up1_ttl1,0);
		
		RouterService.getMessageRouter().handlePromotionRequestVM(fromLeaf,_leaf);
		
		assertNull(_up1.getLastSent());
		assertNull(_up2.getLastSent());
		assertNull(_leaf.getLastSent());
	}
	
	/**
	 * tests a scenario where a request arrives that has 
	 * supposedly travelled 0 hops but its sender does not match.
	 */
	public void testRequestIPNotMatch() throws Exception {
		
		PromotionRequestVendorMessage ipNotMatch =
			new PromotionRequestVendorMessage(_up1_ttl1,_up2_ttl2,0);
		
		RouterService.getMessageRouter().handlePromotionRequestVM(ipNotMatch,_up1);
		
		assertNull(_up1.getLastSent());
		assertNull(_up2.getLastSent());
		assertNull(_leaf.getLastSent());
	}
	
	/**
	 * a request has already travelled two hops.
	 */
	public void test2HopsTravelled() throws Exception {
		
		PromotionRequestVendorMessage twoHops =
			new PromotionRequestVendorMessage(_up1_ttl1,
					_up2_ttl2,2);
		
		RouterService.getMessageRouter().handlePromotionRequestVM(twoHops,_up1);
		assertNull(_up1.getLastSent());
		assertNull(_up2.getLastSent());
		assertNull(_leaf.getLastSent());
	}
	
	/**
	 * tests a scenario when a request is forwarded to a leaf
	 */
	public void testForwardedToLeaf() throws Exception {
		
		PromotionRequestVendorMessage toLeaf =
			new PromotionRequestVendorMessage(_leaf,
					new RemoteCandidate(_up1.getAddress(),_up1.getPort(),(short)1),1);
		
		RouterService.getMessageRouter().handlePromotionRequestVM(toLeaf,_up1);
		
		assertNull(_up1.getLastSent());
		assertNull(_up2.getLastSent());
		assertNotNull(_leaf.getLastSent());
	}
	
	
	public void testForwaredToUP() throws Exception {
		PromotionRequestVendorMessage toUP =
			new PromotionRequestVendorMessage(_up1_ttl1,
					new RemoteCandidate(_up2.getAddress(),_up2.getPort(),(short)1),0);
		
		assertTrue(_bestCandidates.getCandidates()[1].isSame(_up1_ttl1));
		
		RouterService.getMessageRouter().handlePromotionRequestVM(toUP,_up2);
		
		assertNotNull(_up1.getLastSent());
		assertNull(_up2.getLastSent());
		assertNull(_leaf.getLastSent());
	}
	
	public void testNotForwaredToUP() throws Exception {
		PromotionRequestVendorMessage toUP =
			new PromotionRequestVendorMessage(_up2_ttl2,
					new RemoteCandidate(_up1.getAddress(),_up1.getPort(),(short)1),0);
		
		assertTrue(_bestCandidates.getCandidates()[1].isSame(_up1_ttl1));
		
		RouterService.getMessageRouter().handlePromotionRequestVM(toUP,_up1);
		
		assertNull(_up1.getLastSent());
		assertNull(_up2.getLastSent());
		assertNull(_leaf.getLastSent());
	}
	
	/**
	 * stub which doesn't promote anything.
	 */
	static class PromotionManagerStub extends PromotionManager {
		
		public boolean promoted = false;
		
		public void initiatePromotion(PromotionRequestVendorMessage m) {
			promoted = true;
		}
	}
	
	static class UPStub extends ManagedConnectionStub {
		
		public UPStub(String host, int port) {
			super(host,port);
		}
		
		public boolean isGoodUltrapeer() {return true;}
		
		public boolean isOpen() {return true;}
	}
}
