/*
 * This test connects to a live core and issues a promotion request.
 * the core should connect back to us first through an UDP ping, and then
 * as an UP.
 * 
 * its not part of the suite because it will be hard to automate.
 */
package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.upelection.*;

import java.net.*;
import com.sun.java.util.collections.*;

import junit.framework.Test;

public class LivePromotionTest extends ServerSideTestCase {
	
	public static final int LIVE_PORT=6346;
	
	public LivePromotionTest(String name) {
		super(name);
	}
	
	public static Integer numUPs() {
		return new Integer(30);
	}
	
	public static Integer numLeaves() {
		return new Integer(1);
	}
	
	public static void setUpQRPTables() {
		//nothing
	}
	
	
	static PromotionRequestVendorMessage _msg;
	
	static Object connectLock = new Object();
	
	static DatagramSocket _acker;
	
	public static ActivityCallback getActivityCallback() {
		return new ActivityCallbackStub() {
			public void connectionInitializing(Connection c) {
				if (c.getPort()==6346 ||
						c.getPort()==6370)
					System.out.println("initializing " + c);
			}
			public void connectionInitialized(Connection c) {
				if (c.getPort()==6346 ||
					c.getPort()==6370) {
					System.out.println("initialized "+c + " X-up "+
					  c.isSupernodeConnection());
					
					synchronized(connectLock){
						connectLock.notify();
					}
				}
			}
			
			public void connectionClosed(Connection c) {
				if (c.getPort()==6346 ||
						c.getPort()==6370)
					System.out.println("closed "+c);
			}
			
			//just for the heck of it
			public void handleQueryString(String query) {
				System.out.println("received query "+query);
			}
			
		};
	}
	
	public static void setSettings() throws Exception{
		//nothing
		ServerSideTestCase.setSettings();
	}
	
	public static void globalSetUp () throws Exception {
		//nothing again
	}
	
	//Don't run this as part of automated suite, but do uncomment when running it manually.
	public static Test suite() {
       return buildTestSuite(LivePromotionTest.class);
    }  
	
	public void testSetUP(){}
	/**
	 * issues a promotion request to a live node running on localhost as a leaf.
	 */
	public void testPromotion() throws Exception {
		
		//first close all other leafs 
		ConnectionManager manager = RouterService.getConnectionManager();
		for (int i=0; i < LEAF.length;i++)
			LEAF[i].close();
		//for (int i=0; i < ULTRAPEER.length;i++)
		//	ULTRAPEER[i].close();
		
		assertEquals(0,RouterService.getNumFreeLimeWireNonLeafSlots());
		
		//wait until the leaf connects to us.
		
		synchronized(connectLock) {
			connectLock.wait();
		}
		
		Connection c = (Connection)manager.getInitializedClientConnections().get(0);
		System.out.println("connected to me is " +c.getInetAddress());
		//at this point we should have one live leaf connected to us
		assertEquals(1,manager.getNumInitializedClientConnections());
		
		
		//sleep some time, let it age
		try {Thread.sleep(3000);}catch(InterruptedException iox){}
		
		//make it our best candidate
		BestCandidates.update(
				new Candidate((Connection)manager.getInitializedClientConnections().get(0)));
		

		RouterService.getPromotionManager().requestPromotion();
		
		System.out.println("sent promotion request, waiting");

		
		//wait to get an incoming connection
		synchronized(connectLock){
			connectLock.wait();
		}
		
		//at this point the candidate should have connected back to us, claiming to be an up
		assertGreaterThanOrEquals(30,manager.getNumInitializedConnections());
		System.out.println("entering long wait");
		//wait more time for connection to occur
		Thread.sleep(30000);
		System.out.println("exiting test");
		
	}
	
}
