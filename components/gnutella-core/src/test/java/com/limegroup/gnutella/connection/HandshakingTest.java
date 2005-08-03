package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;


/**
 * This class tests LimeWire Gnutella connection handshaking.
 */
public final class HandshakingTest extends BaseTestCase {

    public static final String CRLF="\r\n";

    public static final String GNUTELLA_CONNECT_04 = 
		"GNUTELLA CONNECT/0.4";

    public static final String GNUTELLA_CONNECT_06 =
		"GNUTELLA CONNECT/0.6";

	private static final RouterService ROUTER_SERVICE =
		new RouterService(new ActivityCallbackStub());

	private static Connection _currentConnection;

	/**
	 * List of connections for general use by the tests.
	 */
	private static final List CONNECTIONS = new LinkedList();

	public HandshakingTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(HandshakingTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	public void setUp() throws Exception {
		//launchBackend();
		setStandardSettings();
		ConnectionSettings.PORT.setValue(TEST_PORT);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
		//ConnectionSettings.REMOVE_ENABLED.setValue(false);
		ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
		//ConnectionSettings.NUM_CONNECTIONS.setValue(1);
		//RouterService rs = new RouterService(new ActivityCallbackStub());
		//rs.start();		
		RouterService.clearHostCatcher();
		ROUTER_SERVICE.start();
	}

	public void tearDown() {
		if(_currentConnection != null) {
			_currentConnection.close();
		}
	}
	
	/**
	 * Tests the connection limit to make sure only a limited number
	 * of connections are allowed.
	 */
	public void testConnectionLimit() throws Exception {
		int limit = ConnectionSettings.NUM_CONNECTIONS.getValue()+1;
		for(int i=0; i<limit; i++) {
			try {
				Connection curConn = connect();

				CONNECTIONS.add(curConn);

				sleep(200);
				setPreferredConnectons();

				if(i == 5) {
					// extra check to make sure these connections are 
					// really active
					curConn.receive(6000);
				}

				if(i == ConnectionSettings.NUM_CONNECTIONS.getValue()) {
					// this should throw an IOException because the connection 
					// should  really be closed
					curConn.receive(6000);
					fail("accepted beyond max connections");
				}
			} catch(IOException e) {
				if(i < ConnectionSettings.NUM_CONNECTIONS.getValue())
					fail("allowed only: " + i, e);
				// otherwise, we expected the exception
			}
		}

		clear(CONNECTIONS);
	}


	/**
	 * Tests that a standard connection with all standard LimeWire
	 * settings is accepted.
	 */
	public void testNormalConnectionAccepted() throws Exception {
		_currentConnection = 
			new Connection("localhost", TEST_PORT, 
						   new UltrapeerHeaders("localhost"),
						   new UltrapeerResponder());
		_currentConnection.initialize();

		_currentConnection.close();

		// double check that test connection is working correctly --
		// make sure it send the normal connect sequence correctly
		_currentConnection = new TestConnection();
		_currentConnection.initialize();
	}

	/**
	 * Tests that connections using old-style Gnutella 0.4 connect
	 * strings are rejected.
	 */
	public void test04Rejected() {
		_currentConnection = new TestConnection(GNUTELLA_CONNECT_04);
		try {
			_currentConnection.initialize();
			fail("connection should not have been accepted");
		} catch(Exception e) {
		}
	}

	/**
	 * Test to make sure that random connection strings are rejected.
	 */
	public void testRandomRejected() {
		_currentConnection = new TestConnection("GNUTELLA");
		try {
			_currentConnection.initialize();
			fail("connection should not have been accepted");
		} catch(Exception e) {
		}

		_currentConnection = new TestConnection("CONNECT");
		try {
			_currentConnection.initialize();
			fail("connection should not have been accepted");
		} catch(Exception e) {
		}

		_currentConnection = new TestConnection("RANDOM");
		try {
			_currentConnection.initialize();
			fail("connection should not have been accepted");
		} catch(Exception e) {
		}
	}

	
	/**
	 * Sleeps for the specified number of milliseconds.
	 */
	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch(InterruptedException e) {
			fail(e);
		}

	}

	/**
	 * Creates another connection to the backend.
	 */
	private Connection connect() throws Exception {
		Connection conn = 
			new Connection("localhost", TEST_PORT, 
						   new UltrapeerHeaders("localhost"),
						   new UltrapeerResponder());
		conn.initialize();		
		return conn;
	}

	/**
	 * Closes every connection in the specified collection and closes it.
	 */
	private void clear(Collection col) {
		Iterator iter = col.iterator();
		while(iter.hasNext()) {
			Connection curConn = (Connection)iter.next();
			curConn.close();
		}
		col.clear();		
	}

	private static final class UltrapeerResponder implements HandshakeResponder {
		public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) 
			throws IOException {
			return HandshakeResponse.createResponse(new Properties());
		}		
        
        public void setLocalePreferencing(boolean b) {}
	}

	/**
	 * Class for test connections, using different connect strings and connection
	 * headers.
	 */
	private static class TestConnection extends Connection {
		
		/**
		 * Constant for the connect string to use to connect.
		 */
		private final String CONNECT_STRING;

		private static final Properties ULTRAPEER_PROPS =
			new UltrapeerHeaders("localhost");

		TestConnection() {
			this(GNUTELLA_CONNECT_06);
		}

		TestConnection(String connectString) {
			super("localhost", TEST_PORT, 
				  ULTRAPEER_PROPS,
				  new UltrapeerResponder());		
			CONNECT_STRING = connectString;

			try {
				PrivilegedAccessor.setValue(this, "GNUTELLA_CONNECT_06", 
                    CONNECT_STRING);
			} catch(Exception e) {
				fail("could not initialize test", e);
			}		
		}
	}
	
	/**
	 * Enforces that the preferred connections are set in ConnectionManager.
	 * This will always be done on a system that supports idle time, but we
	 * need to enforce that its done on other systems (such as Linux).
	 */
	private void setPreferredConnectons() throws Exception {
	    PrivilegedAccessor.invokeMethod(RouterService.getConnectionManager(), 
	        "setPreferredConnections", null);
    }
}
