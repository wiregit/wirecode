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
	
	/**
	 * a bunch of serialized data from my gnutella.net file, not sorted.
	 * the sorting will hapen in setUp().
	 */
	private static String []_candidateStats = new String[]{
			"216.153.136.123:6348,70111,1079389102735,1079710622581;1079389106473",
			"68.97.79.238:6348,23351,1079386720998,1079710622640;1079389012834",
			"24.217.171.18:6346,21186,1079389102341,1079710622488;1079389105292",
			"152.7.50.197:6348,15461,1079389011512,1079710152228;1079389020781",
			"216.80.122.62:6346,6156,1079122767776,1079389013555;1079131814346",
			"67.8.163.72:6346,1483,1079131813732,1079389012339;1079131820399",
			"24.102.60.110:6346,513,1079392562917,1079392566666"};
	
	private static SortedSet _candidates;
	
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
		_candidates = new TreeSet(Candidate.priorityComparator());
		try {
			for (int i = 0;i<_candidateStats.length;i++)
				_candidates.add(new Candidate(_candidateStats[i]));
		}catch(java.text.ParseException bad) {
			fail("test data invalid",bad);
		}
		
		//then pick the best, mediocre and worst
		goodCandidate = (Candidate)_candidates.last();
		mediocreCandidate = (Candidate)
			_candidates.toArray()[(int)(_candidateStats.length/2)];
		badCandidate = (Candidate)_candidates.first();
		
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
	
	
	
}
