/*
 * This class tests the routing of PromotionRequest VMs.  It tests the following
 * scenarios:
 * 
 * 1. request arrives at an UP but the leaf is no longer in the table at ttl 0 and 1
 * 2. request arrives at an UP but the leaf is no longer connected
 * 3. request arrives at an UP but it has travelled for too long
 * 4. request arrives at an UP from a leaf
 * 5. request arrives at an UP but the requestor ip doesn't match
 * 6. request arrives at an UP and it forwards it to another UP.
 */
package com.limegroup.gnutella;

import com.limegroup.gnutella.upelection.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.settings.*;

import com.sun.java.util.collections.*;
import java.util.Properties;

import java.io.IOException;
import java.net.InetAddress;

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
	static PromotionRequestVendorMessage leafPromoting, wrongLeaf,leafNotBest0,leafNotBest1,leafGone,
					advertiserGone, tooOld, fromLeaf, wrongIp, forwardedLeaf, forwardedUP;
	
	/**
	 * an ultrapeer which will advertise the candidates
	 */
	static ReplyConnection myUP,goneAdvertiser;
	
	/**
	 * a leaf that is set at address 1.2.3.4
	 */
	static Connection aLeaf;
	
	/**
	 * a Reply Handler stub which claims its an ultrapeer.
	 */
	static ReplyHandler UPReplyHandler = new ReplyHandlerStub() {
		public boolean isGoodUltrapeer(){return true;}
	};
	
	/**
	 * a Reply Handler stub which claims its a leaf
	 */
	static ReplyHandler LeafReplyHandler = new ReplyHandlerStub() {
		public boolean isGoodLeaf() {return true;}
	};
	
	/**
	 * please run this test first to set things up.
	 */
	public void testSetSettings() throws Exception {
		
			aLeaf = new Connection("localhost",PORT,
					new LeafHeaders("1.2.3.4:15"),
					new EmptyResponder());
			aLeaf.initialize();
			
			//add an ultrapeer, make it an advertiser for some of the candidates.
			myUP = new ReplyConnection("localhost",PORT,
					new UltrapeerHeaders("1.2.3.6"),
					new EmptyResponder());
			myUP.initialize();
			
			goneAdvertiser = new ReplyConnection("localhost",PORT,
					new UltrapeerHeaders("1.2.3.7"),
					new EmptyResponder());
			
			
			//drain the connections
			aLeaf.receive();aLeaf.receive();
			myUP.receive();myUP.receive();
			
			
			for (int i = 0;i < ULTRAPEER.length;i++) {
				ULTRAPEER[i].receive();ULTRAPEER[i].receive();
			}
		
			
			
			//set up the candidates table, it will be the same for all tests
			
			myCandidate = new Candidate("localhost",PORT,(short)20); //my local candidate needs to be real
			
			candidates[0] = new Candidate("1.2.3.4",15,(short)25);
			candidates[1] = new Candidate("1.2.3.5",15,(short)30);
			
			candidates[0].setAdvertiser(myUP);
			candidates[1].setAdvertiser(goneAdvertiser);
			
			BestCandidates.update(myCandidate);
			BestCandidates.update(candidates);
			
			//then set up the messages
			leafPromoting = new PromotionRequestVendorMessage(
						new QueryReply.IPPortCombo("127.0.0.1",PORT),
						new QueryReply.IPPortCombo("1.2.3.4",15),
						1);
			leafNotBest0 = new PromotionRequestVendorMessage(
						new QueryReply.IPPortCombo("1.2.3.4",15),
						new QueryReply.IPPortCombo("1.2.3.4",15),
						2);
			leafNotBest1 = new PromotionRequestVendorMessage(
						new QueryReply.IPPortCombo("2.2.3.4",15),
						new QueryReply.IPPortCombo("1.2.3.4",15),
						1);
			leafGone = leafPromoting; //Note: disconnect the stub at port PORT before using this.
			
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
			forwardedUP = new PromotionRequestVendorMessage(
					new QueryReply.IPPortCombo("1.2.3.4",15),
					new QueryReply.IPPortCombo("30.24.0.5",PORT),
					0);
		
	}
	
	/**
	 * tests the scenario where a request arrives and is routed to the leaf,
	 * and then a request arrives for a leaf that is no longer
	 * at ttl 0 and 1 respectively
	 */
	public void testLeafNoLongerBest() throws Exception {
		//receive the message for the best leaf at ttl 0 
		//this message contains our best leaf at ttl 1, but it has 
		//travelled too long and we won't be forwarding it.
		
		RouterService.getMessageRouter().handlePromotionRequestVM(leafNotBest0, UPReplyHandler);
		//nothing should be forwarded here.
		try {
			Message m = aLeaf.receive(500);
			fail("received something on the leaf - wrong. " + m.getClass());
		}catch (IOException expected) {}
		
		//receive the message for a best leaf which is not in 
		//either slot on the table.
		//none of the ultrapeer should get it forwarded.
		RouterService.getMessageRouter().handlePromotionRequestVM(leafNotBest1, UPReplyHandler);
		receiveNothingUPs();
	}
	
	/**
	 * tests the scenario where a promotion request arrives for 
	 * a leaf that is in the candidates table at ttl 0, but we have 
	 * lost the connection to that leaf.
	 */
	public void testLeafGone() throws Exception {
		LEAF[0].receive();LEAF[0].receive();
		//first send a promotion message, see if the leaf gets it
		RouterService.getMessageRouter().handlePromotionRequestVM(leafGone, UPReplyHandler);
		Message m =LEAF[0].receive(520);
		
		
		assertEquals(((PromotionRequestVendorMessage)m).getRequestor(), leafGone.getRequestor());
		
		//disconnect the leaf and retry
		LEAF[0].close();
		RouterService.getMessageRouter().handlePromotionRequestVM(leafGone, UPReplyHandler);
		LEAF[0] = aLeaf;
		try {
			LEAF[0].receive(500);
			fail("leaf received something.  bad");
		}catch(IOException expected) {}
		
	}
	
	/**
	 * tests the scenario where a promotion request has travelled too long.
	 */
	public void testTooOld() throws Exception {
		RouterService.getMessageRouter().handlePromotionRequestVM(tooOld, UPReplyHandler);
		receiveNothingUPs();
	}
	
	/**
	 * tests the scenario where a new promotion request is received from an ultrapeer
	 * but the ips do not match.
	 */
	public void testWrongIP() throws Exception {
		RouterService.getMessageRouter().handlePromotionRequestVM(wrongIp, UPReplyHandler);
		receiveNothingUPs();
	}
	
	/**
	 * tests the scenario where a promotion request is forwarded to an UP.
	 * @throws Exception
	 */
	public void testForwardedUP() throws Exception {
		//disconnect all ultrapeers except myUP
		//for (int i =0; i < ULTRAPEER.length;i++)
		//	ULTRAPEER[i].close();
		RouterService.getMessageRouter().handlePromotionRequestVM(forwardedUP, UPReplyHandler);
		
		
		//check if we received a forwarded message at myUP.
		Message m=null;
		
		m = ULTRAPEER[2].receive(2500);
		
		PromotionRequestVendorMessage prvm = (PromotionRequestVendorMessage)m;
		
		//check proper distance
		assertEquals(forwardedUP.getDistance()+1,prvm.getDistance());
		//and other fields
		assertEquals(forwardedUP.getCandidate().getAddress(),
				prvm.getCandidate().getAddress());
		assertEquals(forwardedUP.getRequestor().getAddress(),
				prvm.getRequestor().getAddress());
	}
	
	/**
	 * tests the scenario where a promotion request arrives on a leaf connection.
	 * @throws Exception
	 */
	public void testFromLeaf() throws Exception {
		RouterService.getMessageRouter().handlePromotionRequestVM(fromLeaf, LeafReplyHandler);
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
	 * checks all ultrapeer connections, making sure none of them
	 * received anything.
	 */
	private void receiveNothingUPs() throws Exception{
		Message m;
		try {
			m = myUP.receive(100);
			fail("myUP received something "+m.getClass());
		}catch(IOException ignored) {}
		
		for (int i = 0;i<ULTRAPEER.length;i++) 
			try {
				m =ULTRAPEER[i].receive(100);
				fail("UP "+i+" out of "+ULTRAPEER.length+ " received something: "+m.getClass());
			}catch(IOException e) {}	
	}

	/**
	 * the candidates need ReplyHandlers, but the stub won't do because I need to
	 * be receiving things on them.
	 */
	private class ReplyConnection extends CountingConnection implements ReplyHandler {
		
		public ReplyConnection(String host, int port, Properties headers, EmptyResponder responder) {
			super(host,port,headers,responder);
		}
		
			/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#handlePingReply(com.limegroup.gnutella.messages.PingReply, com.limegroup.gnutella.ReplyHandler)
		 */
		public void handlePingReply(PingReply pingReply, ReplyHandler handler) {
			
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#countDroppedMessage()
		 */
		public void countDroppedMessage() {
			
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#getDomains()
		 */
		public Set getDomains() {
			
			return null;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.util.IpPort#getInetAddress()
		 */
		public InetAddress getInetAddress() throws IllegalStateException {
			
			return super.getInetAddress();
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#getNumMessagesReceived()
		 */
		public int getNumMessagesReceived() {
			return 0;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#handlePushRequest(com.limegroup.gnutella.messages.PushRequest, com.limegroup.gnutella.ReplyHandler)
		 */
		public void handlePushRequest(PushRequest pushRequest,
				ReplyHandler handler) {
			
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#handleQueryReply(com.limegroup.gnutella.messages.QueryReply, com.limegroup.gnutella.ReplyHandler)
		 */
		public void handleQueryReply(QueryReply queryReply, ReplyHandler handler) {

		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#handleUPListVM(com.limegroup.gnutella.messages.vendor.UPListVendorMessage)
		 */
		public void handleUPListVM(UPListVendorMessage m) {
			
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#isKillable()
		 */
		public boolean isKillable() {
			return true;
		}
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.ReplyHandler#isPersonalSpam(com.limegroup.gnutella.messages.Message)
		 */
		public boolean isPersonalSpam(Message m) {
			return false;
		}
}
}
