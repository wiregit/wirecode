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

import junit.framework.Test;

public class LivePromotionTest extends ServerSideTestCase {
	
	public static final int LIVE_PORT=6346;
	
	public LivePromotionTest(String name) {
		super(name);
	}
	
	public static Integer numUPs() {
		return new Integer(1);
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
				System.out.println("initializing " + c);
			}
			public void connectionInitialized(Connection c) {
				if (c.getPort()==6346)
					System.out.println("initialized "+c);
					synchronized(connectLock){
						connectLock.notify();
					}
			}
			
			public void connectionClosed(Connection c) {
				System.out.println("closed "+c);
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
	
	//Don't run this as part of suite, but do uncomment when running it.
	public static Test suite() {
        return buildTestSuite(LivePromotionTest.class);
    }  
	
	public void testSetUP(){}
	/**
	 * issues a promotion request to a live node running on localhost as a leaf.
	 */
	public void testPromotion() throws Exception {
		
		//first close all other leafs and UPs
		ConnectionManager manager = RouterService.getConnectionManager();
		for (int i=0; i < LEAF.length;i++)
			LEAF[i].close();
		for (int i=0; i < ULTRAPEER.length;i++)
			ULTRAPEER[i].close();
		
		
		
		//wait until the leaf connects to us.
		
		synchronized(connectLock) {
			connectLock.wait();
		}
			
		Connection c = (Connection)manager.getInitializedClientConnections().get(0);
		System.out.println("connected to me is " +c.getAddress());
		//at this point we should have one live leaf connected to us
		assertEquals(1,manager.getNumInitializedClientConnections());
		try {Thread.sleep(3000);}catch(InterruptedException iox){}
		
		//create a message and send it over.
		_msg = new PromotionRequestVendorMessage (
				new QueryReply.IPPortCombo("127.0.0.1",6346),
				new QueryReply.IPPortCombo("127.0.0.1",6667),
				0);
		
		//register an UDP acker before sending
		RouterService.getMessageRouter().registerMessageListener(
				new GUID(_msg.getGUID()), new PromotionACKer("127.0.0.1",6346,false));
		
		System.out.println("about to send message");
		
		c.send(_msg);
		c.flush();
		
		//wait some time
		synchronized(connectLock){
			connectLock.wait();
		}
		
		//at this point the candidate should have connected back to us, claiming to be an up
		assertGreaterThan(0,manager.getNumConnections());
		assertEquals(0,manager.getNumInitializedClientConnections());
		assertGreaterThan(0,manager.getNumInitializedConnections());
		
		
		c = (Connection)manager.getInitializedConnections().get(0);
		assertTrue(c.isSupernodeSupernodeConnection());
		
		//	wait more time
		synchronized(connectLock){
			connectLock.wait();
		}
		
	}
	
}
