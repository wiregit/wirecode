
package com.limegroup.gnutella.upelection;

import java.io.IOException;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.upelection.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.messages.*;

import com.sun.java.util.collections.*;

import junit.framework.Test;

/**
 * black box test for the propagation
 * 
 * We have 3 ultrapeers and 2 leaves connected to us.
 * 
 * 1. test initial announcement
 */
public class ServerSideBestCandidatesTest extends BaseTestCase {
	
	private static final long NEW_TIMEOUT=300;
	
	
	static ConnectionManagerStub _manager;
	
	static UPConn _UP1, _UP2, _UP3;
	static GoodLeafCandidate _leaf1, _leaf2;
	
	static Candidate _remote1, _remote2;
	
	public ServerSideBestCandidatesTest(String name){
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(ServerSideBestCandidatesTest.class);
    }
	
	/**
	 * initialize the connections on startup.
	 *
	 */
	public static void globalSetUp(Class callingClass) {

		try{
		_manager = new ConnectionManagerStub(true);
		_manager.setSupernode(true);
		
		_UP1 = new UPConn("1.1.1.1",1000);
		_UP2 = new UPConn("2.2.2.2",1000);
		_UP3 = new UPConn("3.3.3.3",1000);
		
		_leaf1 = new GoodLeafCandidate("4.4.4.4",1000, (short)20,400);
		_leaf2 = new GoodLeafCandidate("5.5.5.5",1000, (short)30,200);
		
		List leaves = new LinkedList();
		List UPs = new LinkedList();
		
		leaves.add(_leaf1);
		leaves.add(_leaf2);
		UPs.add(_UP1);
		UPs.add(_UP2);
		UPs.add(_UP3);
		
		_manager.setInitializedClientConnections(leaves);
		_manager.setInitializedConnections(UPs);
		
		PrivilegedAccessor.setValue(RouterService.class,"manager",_manager);
		
		
		//decrease the periods
		PrivilegedAccessor.setValue(CandidateAdvertiser.class,"INITIAL_DELAY", 
				new Long(2*NEW_TIMEOUT));
		
		PrivilegedAccessor.setValue(CandidateAdvertiser.class,"UP_INTERVAL", 
				new Long(NEW_TIMEOUT));
		
		PrivilegedAccessor.setValue(Connection.class,"ADVERTISEMENT_INTERVAL",
				new Long(NEW_TIMEOUT));
		
		_remote1 = new RemoteCandidate("1.2.3.4",15,(short)20);
		_remote2 = new RemoteCandidate("1.2.3.5",15,(short)10);
		
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * reset the best candidates table for each test.
	 */
	public void setUp() {
		
		BestCandidates.purge();
		
	}
	
	/**
	 * tests an intial announcement after coming on to the network.
	 * @throws Exception
	 */
	public void testAnnouncement() throws Exception {
		assertTrue(RouterService.isSupernode());
		assertEquals(3,RouterService.getConnectionManager().getNumInitializedConnections());
		
		//wait until the initialize call is issued
		Thread.sleep(2*NEW_TIMEOUT+20);
		
		//make sure all UPs receive the announcement.
		assertEquals(BestCandidatesVendorMessage.class,_UP1.getLastSent().getClass());
		assertEquals(BestCandidatesVendorMessage.class,_UP2.getLastSent().getClass());
		assertEquals(BestCandidatesVendorMessage.class,_UP3.getLastSent().getClass());
		
		BestCandidatesVendorMessage bcvm = (BestCandidatesVendorMessage)
			_UP1.getLastSent();
		
		assertTrue(_leaf2.isSame(bcvm.getBestCandidates()[0]));
		
		bcvm = (BestCandidatesVendorMessage) _UP2.getLastSent();
		
		assertTrue(_leaf2.isSame(bcvm.getBestCandidates()[0]));
		
		bcvm = (BestCandidatesVendorMessage) _UP3.getLastSent();
		
		assertTrue(_leaf2.isSame(bcvm.getBestCandidates()[0]));
	}
	
	/**
	 * the Routing table gets purged, however all the connections
	 * have already received their announcements with the same message.
	 */
	public void testSecondAnnouncement() throws Exception {
		
		_UP1.setLastSent(null);
		_UP2.setLastSent(null);
		_UP3.setLastSent(null);
		
		Thread.sleep(2*NEW_TIMEOUT);
		
		long now = System.currentTimeMillis();
		_UP1.waitForSend((int)NEW_TIMEOUT);
		_UP2.waitForSend((int)NEW_TIMEOUT);
		_UP3.waitForSend((int)NEW_TIMEOUT);
		assertGreaterThanOrEquals(3*NEW_TIMEOUT,System.currentTimeMillis()-now);
		
		assertNull(_UP1.getLastSent());
		assertNull(_UP2.getLastSent());
		assertNull(_UP3.getLastSent());
		
	}
	
	/**
	 * tests a leaf disconnecting - the UPs should get an update.
	 */
	public void testLeafDisconnect() throws Exception {
		
		assertNull(_UP1.getLastSent());
		assertNull(_UP2.getLastSent());
		assertNull(_UP3.getLastSent());
		
		Thread.sleep(3*NEW_TIMEOUT);
		
		assertNull(_UP1.getLastSent());
		assertNull(_UP2.getLastSent());
		assertNull(_UP3.getLastSent());
		
		List oneLeaf = new LinkedList();
		oneLeaf.add(_leaf1);
		_manager.setInitializedClientConnections(oneLeaf);
		BestCandidates.routeFailed(_leaf2);
		
		
		Thread.sleep(2*NEW_TIMEOUT);
		
		assertNotNull(_UP1.getLastSent());
		assertNotNull(_UP2.getLastSent());
		assertNotNull(_UP3.getLastSent());
		
		BestCandidatesVendorMessage bcvm = (BestCandidatesVendorMessage)
		_UP1.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
	
		bcvm = (BestCandidatesVendorMessage) _UP2.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
	
		bcvm = (BestCandidatesVendorMessage) _UP3.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
	}
	
	/**
	 * one UP advertises his candidates - the others should get them.
	 * then another UP advertises a worse candidate - nobody should get an update
	 * then the first UP advertises a worse candidate - everybody gets it.
	 * then another UP advertises a better candidate -everybody gets it.
	 * then that last UP goes down - everybody gets the worse candidate from the previous UP.
	 */
	public void testUPAdvertising() throws Exception {
		assertFalse(_UP1.isSame(_UP2));
		Thread.sleep(2*NEW_TIMEOUT+20);
		assertNotNull(BestCandidates.getCandidates()[0]);
		//at this stage the ultrapeers have received a candidate at our ttl0.
		//one of them will advertise their candidate at ttl0, and everyone should
		//get a candidate at ttl1.
		Candidate[] update = new Candidate[2];
		update[0]=_remote1;
		BestCandidatesVendorMessage bcvm = new BestCandidatesVendorMessage(update);
		
		//UP1 will send the message
		
		_UP1.handleVendorMessage(bcvm);
		
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		
		//sleep some time
		
		Thread.sleep(NEW_TIMEOUT+20);
		
		//all ultrapeers should receive an advertisement
		
		bcvm = (BestCandidatesVendorMessage) _UP1.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
	
		bcvm = (BestCandidatesVendorMessage) _UP2.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
	
		bcvm = (BestCandidatesVendorMessage) _UP3.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
		
		//another UP advertises a worse candidate at ttl0.  The ultrapeers 
		//should not receive anything.
		update[0]=_remote2;
		update[1]=null;
		bcvm = new BestCandidatesVendorMessage(update);
		
		_UP2.handleVendorMessage(bcvm);
		
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		assertTrue(_remote1.isSame(BestCandidates.getCandidates()[1]));
		
		//sleep some time
		
		Thread.sleep(NEW_TIMEOUT+20);
		
		//all ultrapeers should keep the old best message
		
		bcvm = (BestCandidatesVendorMessage) _UP1.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
	
		bcvm = (BestCandidatesVendorMessage) _UP2.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
	
		bcvm = (BestCandidatesVendorMessage) _UP3.getLastSent();
	
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
		
		//however, if _UP1 advertises _remote2, there should be an update.
		bcvm = new BestCandidatesVendorMessage(update);
		
		_UP1.handleVendorMessage(bcvm);
		
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		assertTrue(_remote2.isSame(BestCandidates.getCandidates()[1]));
		
		Thread.sleep(NEW_TIMEOUT+20);
		//all ups should switch to remote2
		
		bcvm = (BestCandidatesVendorMessage) _UP1.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote2.isSame(bcvm.getBestCandidates()[1]));
		
		bcvm = (BestCandidatesVendorMessage) _UP2.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote2.isSame(bcvm.getBestCandidates()[1]));
		
		bcvm = (BestCandidatesVendorMessage) _UP3.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote2.isSame(bcvm.getBestCandidates()[1]));
		
		//lets make UP2 advertise remote1 now.  It should override remote2
		update[0]=_remote1;
		bcvm = new BestCandidatesVendorMessage(update);
		
		_UP2.handleVendorMessage(bcvm);
		
		Thread.sleep(NEW_TIMEOUT+20);
		
		bcvm = (BestCandidatesVendorMessage) _UP1.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
		
		bcvm = (BestCandidatesVendorMessage) _UP2.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
		
		bcvm = (BestCandidatesVendorMessage) _UP3.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote1.isSame(bcvm.getBestCandidates()[1]));
		
		//lets make UP2 die now.  remote2 should be propagated to UP1 & 3.
		List l = new LinkedList();
		l.add(_UP1);
		l.add(_UP3);
		_manager.setInitializedConnections(l);
		assertEquals(2,RouterService.getConnectionManager().getNumInitializedConnections());
		
		BestCandidates.routeFailed(_UP2);
		
		Thread.sleep(NEW_TIMEOUT+20);
		
		bcvm = (BestCandidatesVendorMessage) _UP1.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote2.isSame(bcvm.getBestCandidates()[1]));
		
		bcvm = (BestCandidatesVendorMessage) _UP3.getLastSent();
		
		assertTrue(_leaf1.isSame(bcvm.getBestCandidates()[0]));
		assertTrue(_remote2.isSame(bcvm.getBestCandidates()[1]));
		
	}
	
	/**
	 * 	utility class to represent an UP connection
	 */
	static class UPConn extends ManagedConnectionStub {
		public UPConn(String host, int port) {
			super(host,port);
		}
		public int remoteHostSupportsBestCandidates() {
			return BestCandidatesVendorMessage.VERSION;
		}
		
	/*	public void send(Message m){
			System.out.println(m.getClass());
			super.send(m);
		}/*
		
		public void handleBestCandidatesMessage(BestCandidatesVendorMessage m) throws IOException {
			System.out.println("called handle "+m);
			super.handleBestCandidatesMessage(m);
		}*/
		
		public void handleVendorMessage(VendorMessage vm) {
			super.handleVendorMessage(vm);
		}
		
		public boolean isGoodUltrapeer() {
			return true;
		}
	}
}
