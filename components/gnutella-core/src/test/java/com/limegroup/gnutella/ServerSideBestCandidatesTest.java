/*
 * This class tests the serverside functionality of the Best Candidates propagation.
 * The actual table management is tested in a unit test, so this a black box test
 */
package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;

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
		RouterService.schedule (r, 4000, 300);
		
	}
	
	public static void setSettings() throws Exception {
		ServerSideTestCase.setSettings();
		

		
		//connect the second leaf
		_secondLeaf = new CountingConnection("localhost",PORT,
				new LeafHeaders("somewhere.else"),
				new EmptyResponder());
		assertTrue(_secondLeaf.isOpen());
		
	}
	
	public static Test suite() {
        return buildTestSuite(ServerSideBestCandidatesTest.class);
    }   
	
	/**
	 * tests simple propagation.  In 100 ms the the propagator should send update
	 * messages to the ultrapeer connections.
	 */
	public void testPropagation() throws Exception {
		
		Message m;
		
		//drain the capabilities and support vms on all UPs
		for (int i = 0;i<ULTRAPEER.length;i++) {
			m = ULTRAPEER[i].receive(120);
			m = ULTRAPEER[i].receive(120);
		}
		
		//sleep some time, let the leaf connection age a little
		try {Thread.sleep(7100);}catch(InterruptedException iex){}
		
		
		//make sure each UP gets at least one message
		for (int i=0;i<ULTRAPEER.length;i++) {
			try {
				
				m = ULTRAPEER[i].receive(120);
				if (m instanceof BestCandidatesVendorMessage)
					continue;
				else 
					fail("wrong type of message received at ultrapeer "+i+" out of "+ULTRAPEER.length);
				
			}catch(IOException notExpected) {
				fail("ultrapeer "+i+" out of  "+ULTRAPEER.length+" should have received at least one message");
			}
		}
		
	}
}
