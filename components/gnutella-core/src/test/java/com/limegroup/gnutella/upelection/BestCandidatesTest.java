/*
 * Tests the list of best candidates at 0, 1 and 2 hops.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import junit.framework.Test;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.stubs.*;

import java.net.*;


public class BestCandidatesTest extends BaseTestCase {
	
	
	

	
	//couple of candidates
	private static Candidate goodCandidate, badCandidate, mediocreCandidate;
	
	//couple of advertisers
	private static ReplyHandler advertiser1, advertiser2;
	
	public BestCandidatesTest(String name){
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(BestCandidatesTest.class);
    }
	
	protected void setUp() {
		
		try {
			BestCandidates instance = (BestCandidates) PrivilegedAccessor.getValue(BestCandidates.class,"instance");
			PrivilegedAccessor.setValue(instance,"_best", new Candidate[3]);
		}catch(Exception what) {
			fail("could not (re)set the candidates table",what);
		}
		
		//create the set of test candidates
		try {
			goodCandidate = new Candidate("1.2.3.4",15,(short)20); //20 minutes uptime
			mediocreCandidate = new Candidate("1.2.3.5",15,(short)15); //15 mins
			badCandidate = new Candidate("1.2.3.6",15,(short)10); //10 mins
		}catch (UnknownHostException ieh) {
			fail("find better test ip addresses",ieh);
		}
		
		//then pick the best, mediocre and worst
		
		
		//put the mediocre candidate as our best candidate at ttl 0
		BestCandidates.update(mediocreCandidate);
		
		//make two false advertisers
		advertiser1 = new ReplyHandlerStub();
		
		advertiser2 = new ReplyHandlerStub() {
			public InetAddress getInetAddress() {
				try{
					return InetAddress.getByName("1.2.3.4");
				}catch (UnknownHostException comeOn){
					fail("failed to initialize the advertiser.  fix!",comeOn);
					return null;
				}
			}
		};
		
		//associate them with the best and the worst
		goodCandidate.setAdvertiser(advertiser1);
		badCandidate.setAdvertiser(advertiser2);
		
		//associate more as needed
		
		//test if we have only one candidate
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNull(BestCandidates.getCandidates()[1]);
		assertNull(BestCandidates.getCandidates()[2]);
	}
	
	/**
	 * an ultrapeer advertises a single candidate which is worse than our current best.
	 * Since we have no best candidate at ttl 1 we should still enter it in the table.
	 */
	public void testUpdateSingleCandidate() throws Exception {
		//reset the table.
		setUp();
		
	
		assertEquals(mediocreCandidate.getInetAddress(), BestCandidates.getBest().getInetAddress());
		
		//update with the worse candidate at ttl 1.  It should enter the table but
		//the best overall candidate should still be our previous candidate.
		Candidate []update = new Candidate[2];
		update[0] =badCandidate;
		update[1] =null;
		
		BestCandidates.update(update);
		
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		assertNull(BestCandidates.getCandidates()[2]);
		
		assertEquals(mediocreCandidate.getInetAddress(), BestCandidates.getBest().getInetAddress());
		
		//now update with the best candidate at ttl 1.  Our mediocre candidate should 
		//still be in the table, but the bad candidate on ttl 1 should be removed.
		
		update[0]=goodCandidate;
		
		BestCandidates.update(update);
		
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		assertNull(BestCandidates.getCandidates()[2]);
		
		assertEquals(goodCandidate.getInetAddress(),BestCandidates.getCandidates()[1].getInetAddress());
		assertEquals(goodCandidate.getInetAddress(), BestCandidates.getBest().getInetAddress());
		
		//now try to update again with a not-so-good candidate.  the table should not be changed.
		
		update[0]=mediocreCandidate;
		
		BestCandidates.update(update);
		
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		assertNull(BestCandidates.getCandidates()[2]);
		
		assertEquals(goodCandidate.getInetAddress(),BestCandidates.getCandidates()[1].getInetAddress());
		assertEquals(goodCandidate.getInetAddress(), BestCandidates.getBest().getInetAddress());
		
	}
	
	/**
	 * tests the simple getBest scenario
	 */
	public void testGetBest() throws Exception {
		setUp();
		
		//put the worst guy at ttl 1 and the best guy at ttl 2
		Candidate [] update = new Candidate [2];
		update[0]=badCandidate;
		update[1]=goodCandidate;
		
		BestCandidates.update(update);
		
		//all ranges should be full
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		assertNotNull(BestCandidates.getCandidates()[2]);
		
		//and the best one should be at ttl 2
		
		assertEquals(BestCandidates.getBest().getInetAddress(),
				BestCandidates.getCandidates()[2].getInetAddress());
	}
	
	/**
	 * updates the candidates table with the same candidate at ttl 1 2 and 3.
	 * then it repeats the process with a better candidate.
	 */
	public void testUpdateSameCandidate() throws Exception{
		setUp();
		Candidate []update = new Candidate[2];
		update[0]= mediocreCandidate;
		update[1]=update[0];
		
		BestCandidates.update(update);
		
		//all ranges should be full
		assertNotNull(BestCandidates.getCandidates()[0]);
		assertNotNull(BestCandidates.getCandidates()[1]);
		assertNotNull(BestCandidates.getCandidates()[2]);
		
		//and the best candidate should be the same as all others
		Candidate best = BestCandidates.getBest();
		assertEquals(best.getInetAddress(),
				BestCandidates.getCandidates()[0].getInetAddress());
		assertEquals(best.getInetAddress(),
				BestCandidates.getCandidates()[1].getInetAddress());
		assertEquals(best.getInetAddress(),
				BestCandidates.getCandidates()[2].getInetAddress());
		
		//now update the ttl 1 and 2 candidates to a better guy
		update[0]=goodCandidate;
		update[1]=update[0];
		
		//and the best candidate should be identical for ttl 1 and 2
		best = BestCandidates.getBest();
		assertEquals(best.getInetAddress(),
				BestCandidates.getCandidates()[1].getInetAddress());
		assertEquals(best.getInetAddress(),
				BestCandidates.getCandidates()[2].getInetAddress());
	}
	
	
	/**
	 * tests the scenario where a host changes his mind about his best candidate with
	 * a worse candidate.  It can happen if the current best candidate goes offline.
	 */
	public void testChangedMind() throws Exception {
		setUp();
		
		//give myself a bad candidate
		BestCandidates.update(badCandidate);
		
		//advertise candidate at ttl 1 from advertiser1
		goodCandidate.setAdvertiser(advertiser1);
		
		Candidate [] update = new Candidate[2];
		update[0] = goodCandidate;
		
		BestCandidates.update(update);
		
		assertEquals(goodCandidate.getInetAddress(),BestCandidates.getBest().getInetAddress());
		
		//now the same advertiser changes his mind about his best candidate
		mediocreCandidate.setAdvertiser(advertiser1);
		
		update[0]=mediocreCandidate;
		
		BestCandidates.update(update);
		
		//the new best candidate should be mediocreCandidate.
		assertEquals(mediocreCandidate.getInetAddress(),BestCandidates.getBest().getInetAddress());
	}
	
	
}
