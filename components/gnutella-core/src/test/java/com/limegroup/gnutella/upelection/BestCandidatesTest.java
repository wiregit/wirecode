/*
 * Tests the list of best candidates at 0, 1 and 2 hops.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import junit.framework.Test;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.messages.vendor.BestCandidatesVendorMessage;

import java.net.*;


public class BestCandidatesTest extends BaseTestCase {
	
	
	
	private static BestCandidates _bestCandidates;
	
	//couple of candidates
	private static RemoteCandidate goodCandidate, badCandidate, mediocreCandidate,
		veryGoodCandidate,veryBadCandidate;
	
	//couple of advertisers
	private static UPStubConn advertiser2, advertiser3;
	private static ManagedConnectionStub advertiser1;
	
	//couple of good leaf candidates
	private static Connection goodLeaf, betterLeaf, bestLeaf;
	
	private static CandidateAdvertiserStub _advertiserThread;
	
	public BestCandidatesTest(String name){
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(BestCandidatesTest.class);
    }
	
	protected void setUp() {

		
		//in the set up, replace the advertising thread with a stub.
		
		Candidate [] array = null;
		try {
			_bestCandidates = (BestCandidates) 
				PrivilegedAccessor.getValue(BestCandidates.class,"instance");
			PrivilegedAccessor.setValue(_bestCandidates,"_best", new Candidate[3]);
			
			array = (Candidate []) PrivilegedAccessor.getValue(_bestCandidates, "_best");
			
			CandidateAdvertiser oldAdvertiser = (CandidateAdvertiser)
				PrivilegedAccessor.getValue(_bestCandidates,"_advertiser");
			oldAdvertiser.cancel();
			
			_advertiserThread = new CandidateAdvertiserStub();
			PrivilegedAccessor.setValue(_bestCandidates,"_advertiser", _advertiserThread);
			
			//also set some stubs in routerservice.
			PrivilegedAccessor.setValue(RouterService.class,"router",new MessageRouterStub());
			PrivilegedAccessor.setValue(RouterService.class,"callback",new ActivityCallbackStub());
			
		}catch(Exception what) {
			fail("could not (re)set the candidates table",what);
		}
		
		//create the set of test candidates
		try {
			goodCandidate = new RemoteCandidate("1.2.3.4",15,(short)20); //20 minutes uptime
			mediocreCandidate = new RemoteCandidate("1.2.3.5",15,(short)15); //15 mins
			badCandidate = new RemoteCandidate("1.2.3.6",15,(short)10); //10 mins
			veryGoodCandidate = new RemoteCandidate("1.2.3.7",15,(short)100);
			veryBadCandidate = new RemoteCandidate("1.2.3.8",15,(short)-1);
		}catch (UnknownHostException ieh) {
			fail("find better test ip addresses",ieh);
		}
		
		
		//make some false advertisers
		
		advertiser1 = new ManagedConnectionStub("127.0.0.1",2000); //this is localhost.
		
		Candidate [] remoteCandidates = new Candidate[2];
		remoteCandidates[0]=badCandidate;
		remoteCandidates[1]=veryGoodCandidate;
		
		advertiser2 = new UPStubConn(remoteCandidates,"127.0.0.2",2000);
		
		remoteCandidates = new Candidate[2];
		remoteCandidates[0]=veryBadCandidate;
		remoteCandidates[1]=goodCandidate;
		advertiser3 = new UPStubConn(remoteCandidates,"127.0.0.3",2000);
		
		//and some good leafs
		goodLeaf = new GoodLeafCandidate("1.2.3.4",1234,(short)10, 20);
		betterLeaf = new GoodLeafCandidate("1.2.3.5",1234,(short)10, 10);
		bestLeaf = new GoodLeafCandidate("1.2.3.6",1234,(short)20,20);
		
		try {
			advertiser1.initialize();advertiser2.initialize();advertiser3.initialize();
			goodLeaf.initialize();betterLeaf.initialize();bestLeaf.initialize();
		}catch(Exception tough){}
		
		//associate them with the best and the worst
		mediocreCandidate.setAdvertiser(advertiser1);
		
		badCandidate.setAdvertiser(advertiser2);
		veryGoodCandidate.setAdvertiser(advertiser2);
		
		goodCandidate.setAdvertiser(advertiser3);
		veryBadCandidate.setAdvertiser(advertiser3);
		
		
		//put the mediocre candidate as our best candidate at ttl 0
		array[0]=mediocreCandidate;
		
		//test if we have only one candidate
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNull(_bestCandidates.getCandidates()[1]);
		assertNull(_bestCandidates.getCandidates()[2]);
	}
	
	/**
	 * an ultrapeer advertises a single candidate which is worse than our current best.
	 * Since we have no best candidate at ttl 1 we should still enter it in the table.
	 */
	public void testUpdateSingleCandidate() throws Exception {
		propagateChange();
		
		assertTrue(_advertiserThread.getMsg().getBestCandidates()[0].isSame(mediocreCandidate));
		assertNull(_advertiserThread.getMsg().getBestCandidates()[1]);
	
		assertTrue(mediocreCandidate.isSame(_bestCandidates.getBest()));
		
		//update with the worse candidate at ttl 1.  It should enter the table but
		//the best overall candidate should still be our previous candidate.
		Candidate []update = new RemoteCandidate[2];
		update[0] =badCandidate;
		update[1] =null;
		
		_bestCandidates.update(update);
		
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNull(_bestCandidates.getCandidates()[2]);
		
		assertTrue(mediocreCandidate.isSame(_bestCandidates.getBest()));
		assertTrue(mediocreCandidate.isSame(_bestCandidates.getCandidates()[0]));
		assertTrue(badCandidate.isSame(_bestCandidates.getCandidates()[1]));
		
		
		assertTrue(mediocreCandidate.isSame(_advertiserThread.getMsg().getBestCandidates()[0]));
		assertTrue(badCandidate.isSame(_advertiserThread.getMsg().getBestCandidates()[1]));
		
		//now update with the best candidate at ttl 1.  Our mediocre candidate should 
		//still be in the table, but the bad candidate on ttl 1 should be removed.
		
		update[0]=goodCandidate;
		
		_bestCandidates.update(update);
		
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNull(_bestCandidates.getCandidates()[2]);
		
		assertTrue(mediocreCandidate.isSame(_bestCandidates.getCandidates()[0]));
		assertTrue(goodCandidate.isSame(_bestCandidates.getCandidates()[1]));
		assertTrue(goodCandidate.isSame(_bestCandidates.getBest()));
		
		assertTrue(mediocreCandidate.isSame(_advertiserThread.getMsg().getBestCandidates()[0]));
		assertTrue(goodCandidate.isSame(_advertiserThread.getMsg().getBestCandidates()[1]));
		
		//now try to update again with a not-so-good candidate.  the table should not be changed.
		update[0]=mediocreCandidate;
		
		_bestCandidates.update(update);
		
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNull(_bestCandidates.getCandidates()[2]);
		
		assertTrue(mediocreCandidate.isSame(_bestCandidates.getCandidates()[0]));
		assertTrue(goodCandidate.isSame(_bestCandidates.getCandidates()[1]));
		assertTrue(goodCandidate.isSame(_bestCandidates.getBest()));
		assertTrue(mediocreCandidate.isSame(_advertiserThread.getMsg().getBestCandidates()[0]));
		assertTrue(goodCandidate.isSame(_advertiserThread.getMsg().getBestCandidates()[1]));
		
	}
	
	/**
	 * tests the simple getBest scenario
	 */
	public void testGetBest() throws Exception {
		Comparator comparator = new CandidatePriorityComparator();
		assertEquals(0,comparator.compare(null,null));
		
		
		//put the worst guy at ttl 1 and the best guy at ttl 2
		RemoteCandidate [] update = new RemoteCandidate [2];
		update[0]=badCandidate;
		update[1]=goodCandidate;
		
		_bestCandidates.update(update);
		
		//all ranges should be full
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNotNull(_bestCandidates.getCandidates()[2]);
		
		//and the best one should be at ttl 2
		
		assertEquals(_bestCandidates.getBest().getInetAddress(),
				_bestCandidates.getCandidates()[2].getInetAddress());
	}
	
	/**
	 * updates the candidates table with the same candidate at ttl 1 2 and 3.
	 * then it repeats the process with a better candidate.
	 */
	public void testUpdateSameCandidate() throws Exception{
		
		RemoteCandidate []update = new RemoteCandidate[2];
		update[0]= mediocreCandidate;
		update[1]=update[0];
		
		_bestCandidates.update(update);
		
		//all ranges should be full
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNotNull(_bestCandidates.getCandidates()[2]);
		
		//and the best candidate should be the same as all others
		Candidate best = _bestCandidates.getBest();
		assertEquals(best.getInetAddress(),
				_bestCandidates.getCandidates()[0].getInetAddress());
		assertEquals(best.getInetAddress(),
				_bestCandidates.getCandidates()[1].getInetAddress());
		assertEquals(best.getInetAddress(),
				_bestCandidates.getCandidates()[2].getInetAddress());
		
		//now update the ttl 1 and 2 candidates to a better guy
		update[0]=goodCandidate;
		update[1]=update[0];
		
		//and the best candidate should be identical for ttl 1 and 2
		best = _bestCandidates.getBest();
		assertEquals(best.getInetAddress(),
				_bestCandidates.getCandidates()[1].getInetAddress());
		assertEquals(best.getInetAddress(),
				_bestCandidates.getCandidates()[2].getInetAddress());
	}
	
	
	/**
	 * tests the scenario where a host changes his mind about his best candidate with
	 * a worse candidate.  It can happen if the current best candidate goes offline.
	 */
	public void testChangedMind() throws Exception {
		
		
		//give myself a bad candidate
		_bestCandidates.getCandidates()[0] = badCandidate;
		propagateChange();
		
		//advertise candidate at ttl 1 from advertiser1
		goodCandidate.setAdvertiser(advertiser1);
		
		RemoteCandidate [] update = new RemoteCandidate[2];
		update[0] = goodCandidate;
		
		_bestCandidates.update(update);
		
		assertEquals(goodCandidate.getInetAddress(),_bestCandidates.getBest().getInetAddress());
		
		//now the same advertiser changes his mind about his best candidate
		mediocreCandidate.setAdvertiser(advertiser1);
		
		update[0]=mediocreCandidate;
		
		_bestCandidates.update(update);
		
		//the new best candidate should be mediocreCandidate.
		assertTrue(mediocreCandidate.isSame(_bestCandidates.getBest()));
	}
	
	/**
	 * tests the purging of the candidate table.
	 */
	public void testPurge() throws Exception {
		
		//first lets update the table with something
		Candidate [] update = new Candidate[2];
		update[0]=goodCandidate;
		update[1]=badCandidate;
		
		_bestCandidates.update(update);
		Candidate []updated = _bestCandidates.getCandidates();
		assertNotNull(updated[0]);
		assertNotNull(updated[1]);
		assertNotNull(updated[2]);
		
		//then make the value in the advertiser null
		_advertiserThread.setMsg(null);
		assertNull(_advertiserThread.getMsg());
		
		//purge and update the advertiser
		_bestCandidates.purge();
		propagateChange();
		
		updated = _bestCandidates.getCandidates();
		assertNull(updated[0]);
		assertNull(updated[1]);
		assertNull(updated[2]);
		assertNull(_advertiserThread.getMsg());
	}
	
	/**
	 * tests the call to the initialize() method.
	 */
	public void testInitialize() throws Exception {
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNull(_bestCandidates.getCandidates()[1]);
		assertNull(_bestCandidates.getCandidates()[2]);
		
		assertTrue(_bestCandidates.getBest().isSame(mediocreCandidate));
		
		Candidate [] update = new Candidate[2];
		update[0]=goodCandidate;
		update[1]=veryGoodCandidate;
		_bestCandidates.update(update);
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNotNull(_bestCandidates.getCandidates()[2]);
		
		//add leaf conns to connection manager
		ConnectionManager manager = RouterService.getConnectionManager();
		List l = new LinkedList();
		l.add(betterLeaf);
		l.add(bestLeaf);
		l.add(goodLeaf);
		PrivilegedAccessor.setValue(manager,"_initializedClientConnections",l);
		
		// after this call we should have only one candidate and it should be
		// bestLeaf
		_bestCandidates.initialize();
		
		assertNotNull(_bestCandidates.getCandidates()[0]);
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNotNull(_bestCandidates.getCandidates()[2]);
		
		assertTrue(_bestCandidates.getBest().isSame(veryGoodCandidate));
		
	}
	
	/**
	 * tests the functionality of the getRoute method
	 */
	public void testGetRoute() throws Exception {
		
		//create an update
		Candidate [] update = new Candidate[2];
		update[0]=goodCandidate;
		update[1]=badCandidate;
		
		_bestCandidates.update(update);
		
		//1 an endpoint we don't have in the table and an insane ttl
		IpPort ip = new IpPortPair("9.9.9.9",20);
		
		Connection ret = _bestCandidates.getRoute(ip,5);
		
		assertNull(ret);
		
		//2 an endpoint we have, but at negative ttl
		
		ip = goodCandidate;
		ret = _bestCandidates.getRoute(ip,-2);
		
		assertNull(ret);
		
		//3 an endpoint we have, but not at the request ttl
		
		ret = _bestCandidates.getRoute(ip,0);
		
		assertNull(ret);
		
		//4 an endpoint we have and it is within the request ttl
		
		ret = _bestCandidates.getRoute(ip,2);
		
		assertNotNull(ret);
		assertTrue(goodCandidate.getAdvertiser().isSame(ret));
		
		//5 one more we have, at ttl 0
		ip = mediocreCandidate;
		ret = _bestCandidates.getRoute(ip,0);
		
		assertNotNull(ret);
		assertTrue(mediocreCandidate.getAdvertiser().isSame(ret));
		
	}
	/**
	 * tests the election of a new connection once the route fails.
	 */
	public void testRouteFailedToLeaf() throws Exception {
		
		//first add only one of the good leafs to the connection manager.
		
		ConnectionManager manager = RouterService.getConnectionManager();
		List newCons = new LinkedList();
		newCons.add(goodLeaf);
		
		//also add a bad leaf.
		newCons.add(new ManagedConnectionStub("2.2.2.2",1000));
		PrivilegedAccessor.setValue(manager, "_initializedClientConnections",newCons);
		
		assertEquals(2,manager.getNumInitializedClientConnections());
		
		assertTrue(mediocreCandidate.equals(_bestCandidates.getBest()));
		assertTrue(mediocreCandidate.equals(_bestCandidates.getCandidates()[0]));
		//now fail our best candidate. sigh
		_bestCandidates.routeFailed(mediocreCandidate.getAdvertiser());
		
		//at this point, the BestCandidate class should have checked all our leaf connections
		//and should have elected the new leaf.
		assertTrue(goodLeaf.isSame(_bestCandidates.getBest()));
		
		//lets add the two better leaves now.
		newCons.add(betterLeaf);
		newCons.add(bestLeaf);
		PrivilegedAccessor.setValue(manager, "_initializedClientConnections",newCons);
		assertEquals(4, manager.getNumInitializedClientConnections());
		
		PrivilegedAccessor.setValue(manager,"_bestCandidates",_bestCandidates);
		
		//calling ConnectionManager.remove should trigger an update.

		//PrivilegedAccessor.setValue(RouterService.class,"callback",new ActivityCallbackStub());
		manager.remove((ManagedConnection)goodLeaf);
		
		assertEquals(3,manager.getNumInitializedClientConnections());
		
		//after the update, the best leaf should be selected.
		assertTrue(bestLeaf.isSame(_bestCandidates.getBest()));
		
		//lets remove the best leaf, the better one shoudl remain
		manager.remove((ManagedConnection)bestLeaf);
		
		assertTrue(betterLeaf.isSame(_bestCandidates.getBest()));
		
	}
	
	/**
	 * tests the case where a route fails to an UP connection which has
	 * advertised a candidate.
	 */
	public void testRouteFailedToUP() throws Exception {
		
		//make sure I have somebody at ttl 1 and 2
		Candidate [] update = new Candidate[2];
		update[0]=goodCandidate;
		update[1]=badCandidate;
		
		_bestCandidates.update(update);
		
		assertNotNull(_bestCandidates.getCandidates()[1]);
		assertNotNull(_bestCandidates.getCandidates()[2]);
		
		//connect the advertisers to the connection manager.
		List list = new LinkedList();
		list.add(advertiser2);
		list.add(advertiser3);
		
		ConnectionManager manager = RouterService.getConnectionManager();
		
		PrivilegedAccessor.setValue(manager,"_initializedConnections",list);
		
		//////////////////////////////
		// start the test
		//////////////////////////////
		
		//* *  * * * *    ROUTING TABLE:
		//
		//NAME:				TTL 1			TTL 2
		//advertiser2      badCandidate		veryGoodCandidate
		//advertiser3      veryBadCandidate goodCandidate

		//CURRRENT STATE: TTL1: goodCandidate  TTL2: badCandidate
		
		//so, after closing advertiser 2 we should :
		
		manager.remove(advertiser2);
		PrivilegedAccessor.setValue(manager,"_initializedConnections",list);
		
		//the candidate at ttl1 should not change because we do not check it.
		assertTrue(goodCandidate.isSame(_bestCandidates.getCandidates()[1]));
		
		//at ttl2 we had goodCandidate.. so now we have it twice ;)
		assertTrue(goodCandidate.isSame(_bestCandidates.getCandidates()[2]));
		
		//CURRRENT STATE: TTL1: goodCandidate  TTL2: goodCandidate.
		
		//both are advertised by advertiser3, so removing that will simply put the
		//values from advertiser2.
		
		manager.remove(advertiser3);
		PrivilegedAccessor.setValue(manager,"_initializedConnections",list);
		
		assertTrue(badCandidate.isSame(_bestCandidates.getCandidates()[1]));
		assertTrue(veryGoodCandidate.isSame(_bestCandidates.getCandidates()[2]));
		
		//CURRRENT STATE: TTL1: badCandidate  TTL2: veryGoodCandidate.
		
		//both of these candidates are better than the ones advertiser3 has.
		//lets edit the table directly.
		update[1]=null;
		_bestCandidates.update(update);
		
		assertTrue(goodCandidate.isSame(_bestCandidates.getCandidates()[1]));
		assertTrue(veryGoodCandidate.isSame(_bestCandidates.getCandidates()[2]));
		
		//CURRENT STATE: TTL1: goodCandidate  TTL2: veryGoodCandidate
		
		//if we remove advertiser3, TTL1 should be replaced with VeryBadCandidate.
		manager.remove(advertiser3);
		PrivilegedAccessor.setValue(manager,"_initializedConnections",list);
		
		assertTrue(badCandidate.isSame(_bestCandidates.getCandidates()[1]));
		assertTrue(veryGoodCandidate.isSame(_bestCandidates.getCandidates()[2]));
		//CURRENT STATE: TTL1: badCandidate TTL2: veryGoodCandidate
		
	}
	/**
	 * uses PrivilegedAccessor to call this method from _bestCandidates
	 */
	private static void propagateChange() throws Exception{
		PrivilegedAccessor.invokeMethod(
				_bestCandidates,"propagateChange", new Object[0]);
	}
	
	
	private static void dumpRoutingTable(Candidate [] table) {
		System.out.println(table[1].getAddress()+ " advertised by "+table[1].getAdvertiser());
		System.out.println(table[2].getAddress()+ " advertised by "+table[2].getAdvertiser());
	}
	
	/**
	 * a class which mimics a ManagedConnection which has remembered its advertisers.
	 */
	static class UPStubConn extends ManagedConnectionStub {
		final Candidate [] _candidates;
		
		public UPStubConn(Candidate [] cands) {
			super();
			_candidates = cands;
		}
		
		public UPStubConn(Candidate [] cands, String host, int port) {
			super(host,port);
			_candidates = cands;
		}
		public Candidate[] getCandidates() {
			return _candidates;
		}
		
		
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#isSupernodeClientConnection()
		 */
		public boolean isSupernodeClientConnection() {
			return false;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.Connection#isSupernodeSupernodeConnection()
		 */
		public boolean isSupernodeSupernodeConnection() {
			return true;
		}
	}
}
