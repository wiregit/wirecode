package com.limegroup.gnutella;
import junit.framework.Test;

import com.limegroup.gnutella.util.*;

import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.handshaking.*;

/**
 * Test for the functionalit of the LimeVerifier class, and of the code in
 * ManagedConnection which parses the version.
 */
public class LimeVerifierTest extends BaseTestCase {

	static NotSendingConnection _conn = new NotSendingConnection("asdf");
	static NotRemovingStub _manager = new NotRemovingStub();
	
	static NotSendingConnection c1,c2,c3,c4,c5,c6;
	
	static Message _testMessage;

	public LimeVerifierTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(LimeVerifierTest.class);
	}
	
	public static void globalSetUp() { 
		try{
			
			//lower the intervals
			PrivilegedAccessor.setValue(LimeVerifier.class,"INITIAL_INTERVAL", new Integer(300));
			PrivilegedAccessor.setValue(LimeVerifier.class,"RESPONSE_TIME", new Integer(300));
			
			//put a stub connection manager
			PrivilegedAccessor.setValue(RouterService.class, "manager", _manager);
			
			//get the test message, in case it changes
			_testMessage = (Message) PrivilegedAccessor.getValue(LimeVerifier.class, "_testMessage");
			
			c1 = new NotSendingConnection("LimeWire4.0.0.0.0.0.0.0.0.0.0");
			c2 = new NotSendingConnection("M0rphe0e0eus12");
			c3 = new NotSendingConnection("");
			c4 = new NotSendingConnection("LimeWire/4.0.3    ");
			c5 =
				new NotSendingConnection("LimeWire            3.9.great-gregorio's-secret-patched-version");
		
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void setUp() {
		_conn._lastSent=null;
		_manager._called=false;
	}
	
	/**
	 * registers a connection as suspect and then clears it.
	 */
	public void testRegisterAndClear() throws Exception {
		LimeVerifier.registerSuspect(_conn);
		
		//wait a timeout
		Thread.sleep(400);
		
		//a message should have been sent on the connection
		assertEquals(_testMessage,_conn._lastSent);
		
		//clear the suspect
		LimeVerifier.clearSuspect(_conn);
		
		//wait some time
		Thread.sleep(600);
		
		assertFalse(_manager._called);
	}
	
	
	/**
	 * tests the removal of a connection that has failed to 
	 * authenticate itself
	 */
	public void testRemoveFake() throws Exception {
		LimeVerifier.registerSuspect(_conn);
		
		//wait a timeout
		Thread.sleep(400);
		
		//a message should have been sent on the connection
		assertEquals(_testMessage,_conn._lastSent);
		
		//wait some more time
		Thread.sleep(400);
		
		assertTrue(_manager._called);
	}
	
	/**
	 * tests whether the call is issued only to the correct Limewire Version
	 */
	public void testVersionParsing() throws Exception {
		
		c1.invokeCheck();
		Thread.sleep(400);
		assertNull(c1._lastSent);
		
		c2.invokeCheck();
		Thread.sleep(400);
		assertNull(c1._lastSent);
		
		c3.invokeCheck();
		Thread.sleep(400);
		assertNull(c1._lastSent);
		
		c4.invokeCheck();
		Thread.sleep(400);
		assertEquals(_testMessage,c4._lastSent);
		LimeVerifier.clearSuspect(c4);
		
		c5.invokeCheck();
		Thread.sleep(400);
		assertEquals(_testMessage,c5._lastSent);
		LimeVerifier.clearSuspect(c5);
		
		
	}
	
	static class NotRemovingStub extends ConnectionManagerStub {
		public boolean _called = false;
		public void remove(ManagedConnection c) {
			_called = true;
		}
	}
	
	static class NotSendingConnection extends ManagedConnectionStub {
		Message _lastSent;
		public void send(Message m) {
			_lastSent = m;
		}
		
		
		public NotSendingConnection(String version) {
			
			HEADERS_READ.setProperty(HeaderNames.USER_AGENT,version);
		}
		
		public void invokeCheck() throws Exception{
			PrivilegedAccessor.invokeMethod(this,"verifyLimeWire", new Object[0]);
		}
	}
	
}
