/*
 * This class tests the routing of PromotionRequest VMs.  It tests the following
 * scenarios:
 * 
 * 1. request arrives at a leaf which initiates the promotion process
 * 2. request arrives at a wrong leaf
 * 3. request arrives at an UP but the leaf is no longer in the table
 * 4. request arrives at an UP but the leaf is no longer connected
 * 5. request arrives at an UP but the advertiser is no longer up
 * 6. request arrives at an UP but it has travelled for too long
 * 7. request arrives at an UP from a leaf
 * 8. request arrives at an UP but the requestor ip doesn't match
 * 9. request arrives at an UP and it forwards it to leaf
 * 10. request arrives at an UP and it forwards it to another UP.
 */
package com.limegroup.gnutella;

import com.limegroup.gnutella.upelection.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.settings.*;

import junit.framework.Test;

public class ServerSidePromotionRequestTest extends ServerSideTestCase {
	
	/**
	 * some initial candidates to put in our BestCandidates table
	 */
	static Candidate[] candidates = new Candidate[2];
	static Candidate myCandidate;
	
	/**
	 * the different messages used in the tests.
	 */
	static PromotionRequestVendorMessage leafPromoting, wrongLeaf,leafNotBest,leafGone,
					advertiserGone, tooOld, fromLeaf, wrongIp, forwardedLeaf, forwardedUP;
	
	/**
	 * an ultrapeer which will advertise the candidates
	 */
	static CountingConnection myUP;
	
	public static void globalSetUp() throws Exception {
		//do nothing
		
	}
	
	/**
	 * please run this test first to set things up.
	 */
	public void testSetSettings() throws Exception {
	
			//add an ultrapeer, make it an advertiser for some of the candidates.
			myUP = new CountingConnection("localhost",PORT,
						new UltrapeerHeaders("1.2.3.4"),
						new EmptyResponder());
			myUP.initialize();
		
			myUP.send(leafPromoting);
		
			
			
			//set up the candidates table, it will be the same for all tests
			
			myCandidate = new Candidate("localhost",PORT,(short)20); //my local candidate needs to be real
			
			candidates[0] = new Candidate("1.2.3.4",15,(short)25);
			candidates[1] = new Candidate("1.2.3.5",15,(short)30);
			
			BestCandidates.update(myCandidate);
			BestCandidates.update(candidates);
			
			//then set up the messages
			leafPromoting = new PromotionRequestVendorMessage(
						new QueryReply.IPPortCombo("127.0.0.1",PORT),
						new QueryReply.IPPortCombo("1.2.3.4",15),
						2);
			wrongLeaf = new PromotionRequestVendorMessage(
						new QueryReply.IPPortCombo("2.2.3.4",15),
						new QueryReply.IPPortCombo("1.2.3.4",15),
						1);
			leafNotBest = wrongLeaf;
			leafGone = leafPromoting; //Note: disconnect the stub at port PORT before using this.
			advertiserGone = new PromotionRequestVendorMessage(
						new QueryReply.IPPortCombo("1.2.3.4",15), //don't forget to set advertiser for this one
						new QueryReply.IPPortCombo("1.2.3.4",15),
						1);
			tooOld = new PromotionRequestVendorMessage(
					new QueryReply.IPPortCombo("2.2.3.4",15),
					new QueryReply.IPPortCombo("1.2.3.4",15),
					3);
			fromLeaf = new PromotionRequestVendorMessage(
					new QueryReply.IPPortCombo("2.2.3.4",15),
					new QueryReply.IPPortCombo("localhost",PORT),
					1);
			wrongIp = new PromotionRequestVendorMessage(
					new QueryReply.IPPortCombo("127.0.0.1",PORT),
					new QueryReply.IPPortCombo("1.2.3.4",15),
					0);
			forwardedLeaf = new PromotionRequestVendorMessage( //reconnect the stub at PORT
					new QueryReply.IPPortCombo("localhost",PORT),
					new QueryReply.IPPortCombo("1.2.3.4",15),
					2);
			forwardedUP = advertiserGone; //don't forget to reconnect the advertiser
				
		
	}
	
	public static Test suite() {
        return buildTestSuite(ServerSidePromotionRequestTest.class);
    }   
	
	public ServerSidePromotionRequestTest(String name) {
		super(name);
	}
	
	private static ActivityCallback getActivityCallback() {
		return new ActivityCallbackStub();
	}
	
	private static void setUpQRPTables() {
		//nothing
	}
	
	/**
	 * only one leaf needed
	 */
	private static Integer numLeaves() {
		return new Integer(2);
	}
	
	/**
	 * a couple of ultrapeers
	 */
	private static Integer numUPs() {
		return new Integer(3);
	}
	
	/**
	 * tests the scenario where a PromotionRequest arrives at a leaf and it
	 * promotes itself.
	 */
	public void testLeafPromoting() throws Exception {
		//first make sure we are a leaf
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		assertFalse(RouterService.isSupernode());

	}
}
