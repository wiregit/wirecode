
package com.limegroup.gnutella.upelection;

import sun.net.NetworkServer;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.vendor.BestCandidatesVendorMessage;
import com.limegroup.gnutella.messages.vendor.FeaturesVendorMessage;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.upelection.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.*;
import junit.framework.Test;

import java.io.IOException;

/**
 * Small test for the CandidateAdvertiser thread.
 */
public class CandidateAdvertiserTest extends BaseTestCase {
	
	private static final long TEST_TIMEOUT=300;
	
	BestCandidates _bestCandidates = BestCandidates.instance();
	
	/**
	 * the advertiser
	 */
	static CandidateAdvertiser _advertiser;
	
	/**
	 * some notification objects
	 */
	static Object _leafLock, _upLock;
	
	/**
	 * some connections
	 */
	static UPConnection _supporting, _notSupporting; 
	static LeafConnection _leaf;
	static List _list;
	
	/**
	 * a stub for the connectionManager
	 */
	static ConnectionManagerStub _manager = new ConnectionManagerStub();
	
	public CandidateAdvertiserTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(CandidateAdvertiserTest.class);
    }
	
	
	public void setUp() {
		
		_leafLock = new Object();
		_upLock = new Object();
		
		_notSupporting = new UPConnection(false);
		_supporting = new UPConnection(true);
		_leaf = new LeafConnection();
		
		_list = new LinkedList();
		_list.add(_notSupporting);
		_list.add(_supporting);
		_list.add(_leaf);
		
		try{
			//reduce the timeouts before creating.
			PrivilegedAccessor.setValue(CandidateAdvertiser.class,"INITIAL_DELAY",new Long(TEST_TIMEOUT));
			PrivilegedAccessor.setValue(CandidateAdvertiser.class,"LEAF_INTERVAL",new Long(TEST_TIMEOUT));
			PrivilegedAccessor.setValue(CandidateAdvertiser.class,"UP_INTERVAL",new Long(TEST_TIMEOUT));
			
			_bestCandidates.purge();
			
			_advertiser = (CandidateAdvertiser)
			PrivilegedAccessor.getValue(BestCandidates.class, "_advertiser");
			
			_advertiser.cancel();
			_advertiser = new CandidateAdvertiser();
			
			PrivilegedAccessor.setValue(_bestCandidates,"_advertiser",_advertiser);
			
			//replace the original connection manager with the stub
			_manager.setInitializedConnections(_list);
			
			PrivilegedAccessor.setValue(RouterService.class, "manager", 
					_manager);
			
		}catch(Exception bad) {
			bad.printStackTrace(); 
			return;
		}
		
	}
	
	/**
	 * tests whether leafs receive <tt> FeaturesVendorMessage </tt>'s 
	 */
	public void testLeafFunctionality() throws Exception {
		_manager.setSupernode(false);
		assertFalse(RouterService.isSupernode());
		
		//we are a leaf.  We should be receiving things on the leaf connection.
		
		synchronized(_leafLock) {
			_leafLock.wait(TEST_TIMEOUT);
		}
		
		//waits on the UP locks should timeout.
		long now = System.currentTimeMillis();
		synchronized(_upLock) {
			_upLock.wait(TEST_TIMEOUT);
		}
		assertGreaterThanOrEquals(TEST_TIMEOUT,System.currentTimeMillis() -now);
		
		assertTrue(_leaf._gotIt);
		assertFalse(_supporting._gotNotNull);
		assertFalse(_notSupporting._gotNotNull);
	}
	
	/**
	 * tests whether UPs receive BestCandidatesMessages and whether they are
	 * the right kind.
	 */
	public void testUPFunctionality() throws Exception {
		_manager.setSupernode(true);
		
		assertTrue(RouterService.isSupernode());
		
		long now = System.currentTimeMillis();
		
		
		synchronized(_upLock) {
			_upLock.wait(TEST_TIMEOUT);
		}
		synchronized(_leafLock) {
			_leafLock.wait(TEST_TIMEOUT);
		}
		
		assertGreaterThanOrEquals(2*TEST_TIMEOUT,System.currentTimeMillis() -now);
		
		assertFalse(_supporting._gotNotNull);
		assertFalse(_notSupporting._gotNotNull);
		assertFalse(_leaf._gotIt);
		
		//create a BestCandidatesVendorMessage with some fake candidates
		Candidate [] candidates = new Candidate[2];
		candidates[0] = new RemoteCandidate("1.2.3.4",15,(short)2);
		BestCandidatesVendorMessage bcvm = new BestCandidatesVendorMessage(candidates);
		_advertiser.setMsg(bcvm);
		
		assertNotNull(PrivilegedAccessor.getValue(_advertiser, "_bcvm"));
		
		now = System.currentTimeMillis();
		
		//we should not receive a null message
		synchronized(_upLock) {
			_upLock.wait(TEST_TIMEOUT);
		}
		
		assertTrue(_supporting._gotNotNull);
		assertFalse(_notSupporting._gotNotNull);
		assertFalse(_leaf._gotIt);
		assertLessThanOrEquals(TEST_TIMEOUT+20, System.currentTimeMillis()-now);
		
		
		//after another interval, we should get the same message
		_supporting._gotNotNull=false;
		now = System.currentTimeMillis();
		
		synchronized(_leafLock) {
			_leafLock.wait(TEST_TIMEOUT);
		}
		synchronized(_upLock) {
			_upLock.wait(TEST_TIMEOUT);
		}
		
		
		assertTrue(_supporting._gotNotNull);
		assertFalse(_notSupporting._gotNotNull);
		assertFalse(_leaf._gotIt);
		assertGreaterThanOrEquals(TEST_TIMEOUT,System.currentTimeMillis() -now);
		
	}
	
	/**
	 * A connection which waits to receive a Features Message
	 */
	static class LeafConnection extends ManagedConnectionStub {
		
		public boolean _gotIt=false;
		
		public void send(Message m) {
			if (m instanceof FeaturesVendorMessage)
				synchronized(_leafLock){
					_leafLock.notifyAll();
					_gotIt=true;
				}
		}
	}
	
	/**
	 * a connection which claims it supports best candidates messages
	 * and waits for that method to be called
	 */
	static class UPConnection extends ManagedConnectionStub {
		
		boolean _supporting;
		public boolean _gotNotNull,_gotNull;
		
		public UPConnection(boolean supporting) {
			_supporting = supporting;
		}
		
		public void handleBestCandidatesMessage(BestCandidatesVendorMessage m) {
		
			synchronized(_upLock){
				_gotNotNull=true;
				_upLock.notifyAll();
			}
		
		}
		
		public int remoteHostSupportsBestCandidates() {
			return _supporting ? BestCandidatesVendorMessage.VERSION : -1;
		}
		
		public void send(Message m) {
			//stub
		}
	}
	
	
}
