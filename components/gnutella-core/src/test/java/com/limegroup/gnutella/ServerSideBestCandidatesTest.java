/*
 * This class tests the serverside functionality of the Best Candidates propagation.
 * The actual table management is tested in a unit test, so this a black box test
 * 
 * Since there are hardcoded timeouts in several places which influence the decision whether
 * a given leaf is a candidate or not, please do not change the order of the tests.  
 */
package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.upelection.*;

import junit.framework.Test;

import java.io.IOException;

public class ServerSideBestCandidatesTest extends ServerSideTestCase {
	
	
	
	public ServerSideBestCandidatesTest(String name){
		super(name);
	}
	
	private static CountingConnection _secondLeaf;
	
	private static ActivityCallback getActivityCallback() {
		return new ActivityCallbackStub();
	}
	
	private static void setUpQRPTables() {
		//nothing
	}
	
	/**
	 * initialize only one leaf on startup.  It will become the best candidate
	 */
	private static Integer numLeaves() {
		return new Integer(1);
	}
	
	/**
	 * a couple of ultrapeers
	 */
	private static Integer numUPs() {
		return new Integer(3);
	}
	public static void globalSetUp() throws Exception {
		
		//schedule a second propagator with shorter delay values.
		Class advertiser = PrivilegedAccessor.getClass(MessageRouter.class,"CandidateAdvertiser");
		Runnable r = (Runnable)PrivilegedAccessor.invokeConstructor(advertiser,new Object[0],null);
		RouterService.schedule (r, 4000, 500);

	}
	
	public static void setSettings() throws Exception {
		ServerSideTestCase.setSettings();
		PrivilegedAccessor.setValue(Candidate.class,"MINUTE",new Integer(1));
		
	}
	
	public static Test suite() {
        return buildTestSuite(ServerSideBestCandidatesTest.class);
    }   
	
	/**
	 * tests simple propagation.  In 100 ms the the propagator should send update
	 * messages to the ultrapeer connections.
	 */
	public void testPropagation() throws Exception {
		

		
		drainInitialVMs();
		
		//sleep some time, let the leaf connection age a little
		try {Thread.sleep(7100);}catch(InterruptedException iex){}
		
		BestCandidatesVendorMessage bcvm = readMessage();
		
		//compare the received message with a Candidate derived from the originally connected leaf
		Candidate expectedCandidate = new Candidate(LEAF[0]);
		
		Candidate []receivedUpdate = bcvm.getBestCandidates();
		
		assertEquals(expectedCandidate.getInetAddress(),receivedUpdate[0].getInetAddress());
		
		//the best candidate at ttl 2 should be null
		assertNull(receivedUpdate[1]);
		
		drainInitialVMs();
	}
	
	
	/**
	 * the current best candidate goes offline, so the next update should contain
	 * the second best leaf.
	 */
	public void testChangingMind() throws Exception {
		drainInitialVMs();
		//connect the second leaf
		_secondLeaf = new CountingConnection("localhost",PORT,
				new LeafHeaders("somewhere.else"),
				new EmptyResponder());
		assertTrue(_secondLeaf.isOpen());
		//connect the second leaf now.
		_secondLeaf.initialize();
		
		//wait some time
		try {Thread.sleep(2100);}catch(InterruptedException iex){}
		
		//close the leaf
		LEAF[0].close();
		
		//make sure the second leaf is open and initialized
		assertTrue(_secondLeaf.isOpen());
		assertTrue(_secondLeaf.isInitialized());
		
		//wait some more time
		try {Thread.sleep(3000);}catch(InterruptedException iex){}
		
		BestCandidatesVendorMessage bcvm = readMessage();
		Candidate []update = bcvm.getBestCandidates();
		
		//verify that there is still only one update
		assertNull(update[1]);
		
		//and that it is equivalent to the new leaf
		Candidate expectedCandidate = new Candidate(_secondLeaf);
		
		assertEquals(expectedCandidate.getInetAddress(),update[0].getInetAddress());
		
		//close the second leaf
		_secondLeaf.close();
		//_secondLeaf.initialize();
		
		//re-init the first leaf to prepare for next test
		LEAF[0] = new Connection("localhost", PORT, 
                new LeafHeaders("localhost"),
                new EmptyResponder()
                );
		assertTrue(LEAF[0].isOpen());
		LEAF[0].initialize();
		
		//drain the ultrapeers
		drainInitialVMs();
	}
	
	/**
	 * tests advertising at ttl2.  What happens:
	 * 1. a new ultrapeer connects
	 * 2. it advertises its ttl 0 and ttl 1 candidates
	 * 3. ULTRAPEER[0-3] should receive advertisements with the local leaf 
	 * and the one advertised at ttl 0 by the new guy at ttl 1
	 * 
	 * This test also checks whether the advertiser is assigned properly. 
	 * 
	 */
	public void testFullAdvertisement() throws Exception {
		drainInitialVMs();
		//create the new UP connection
		CountingConnection newUP = new CountingConnection("localhost",PORT,
				new UltrapeerHeaders("1.2.3.4"), new EmptyResponder());
		
		newUP.initialize();
		
		//wait some time (also necessary for the re-initialized leaf to age a little)
		try {Thread.sleep(2600);}catch(InterruptedException iex){}
		
		//drain the new UP from the two vendor messages
		Message tmp = newUP.receive();
		tmp = newUP.receive();
		
		//create a VendorMessage with the new candidates.  
		//first, create new Candidate []
		
		Candidate [] update = new Candidate [2];
		update[0] = new Candidate("1.2.3.4",15,(short)20);
		update[1] = new Candidate("1.2.3.4",15,(short)25);
		
		BestCandidatesVendorMessage bcvm = new BestCandidatesVendorMessage(update);
		
		//then send this message on the new ultrapeer connection
		newUP.send(bcvm);
		newUP.flush();
		
		//wait some more time
		try {Thread.sleep(2600);}catch(InterruptedException iex){}
		
		//check whether the ultrapeer received and parsed it properly
		//and whether it assigned the advertiser
		Candidate [] ourCandidates = BestCandidates.getCandidates();
		
		assertNotNull(ourCandidates[0]);
		assertNotNull(ourCandidates[1]);
		assertNotNull(ourCandidates[2]);
		
		assertEquals(ourCandidates[1].getInetAddress(), update[0].getInetAddress());
		assertEquals(ourCandidates[2].getInetAddress(), update[1].getInetAddress());
		
		assertEquals(ourCandidates[1].getAdvertiser().getInetAddress(), 
					newUP.getInetAddress());
		assertEquals(ourCandidates[2].getAdvertiser().getInetAddress(), 
				newUP.getInetAddress());
		
		//sleep some more?
		try {Thread.sleep(1000);}catch(InterruptedException iex){}
		//now check whether the other ultrapeers received an updated message
		bcvm = readMessage();
		
		Candidate [] newUpdate = bcvm.getBestCandidates();
		
		//both slots should be full
		assertEquals(2,newUpdate.length);
		assertNotNull(newUpdate[0]);
		assertNotNull(newUpdate[1]);
		
		//this should contain LEAF[0] at slot 0 and one of the new leafs at slot 1
		assertEquals(LEAF[0].getInetAddress(), newUpdate[0].getInetAddress());
		assertEquals(update[0].getInetAddress(), newUpdate[1].getInetAddress());
		
		//close the new ultrapeer, leave the leaf connected
		newUP.close();
		
	}
	
	
	private void drainInitialVMs() throws Exception {
		Message m;
		//drain the capabilities and support vms on all UPs
		try {
			while(true)
				for (int i = 0;i<ULTRAPEER.length;i++) {
					m = ULTRAPEER[i].receive(60);
				}
		}catch(IOException iox){}
	}
	
	/**
	 * reads a VM on each ultrapeer and checks if its the right kind
	 */
	private BestCandidatesVendorMessage readMessage() throws Exception {
		Message m=null;
		//make sure each UP gets at least one message
		for (int i=0;i<ULTRAPEER.length;i++) {
			
			properMessage: {
				try {
				
					while(true) {
						m = ULTRAPEER[i].receive(120);
						if (m instanceof BestCandidatesVendorMessage)
							break properMessage;
						else if (m instanceof PingRequest)
							continue;
						else 
							fail("wrong type of message received at ultrapeer "+i+" out of "+ULTRAPEER.length+
									" message type is "+m.getClass());
					}
				
				}catch(IOException notExpected) {
					fail("ultrapeer "+i+" out of  "+ULTRAPEER.length+" should have received at least one message");
				}
			} //end of properMessage block
		}
		return (BestCandidatesVendorMessage)m;
		
	}
	
}
