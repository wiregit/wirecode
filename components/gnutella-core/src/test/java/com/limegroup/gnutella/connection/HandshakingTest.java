package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.handshaking.*;
import junit.framework.*;
import com.limegroup.gnutella.util.*;
import java.io.*;
import java.util.*;


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
		ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
		//ConnectionSettings.REMOVE_ENABLED.setValue(false);
		ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);
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
		int limit = ConnectionManager.ULTRAPEER_CONNECTIONS+1;
		for(int i=0; i<limit; i++) {
			try {
				Connection curConn = connect();

				CONNECTIONS.add(curConn);				

				sleep(200);

				if(i == 5) {
					// extra check to make sure these connections are 
					// really active
					curConn.receive(6000);
				}

				if(i == ConnectionManager.ULTRAPEER_CONNECTIONS) {

					// this should throw an IOException because the connection 
					// should  really be closed
					curConn.receive(6000);
					fail("accepted beyond max connections");
				}
			} catch(IOException e) {
				if(i < ConnectionManager.ULTRAPEER_CONNECTIONS) {
					e.printStackTrace();
					fail("unexpected exception: "+e);
				}
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
			new Connection("localhost", PORT, 
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
			e.printStackTrace();
			fail("unexpected exception: "+e);
		}

	}

	/**
	 * Creates another connection to the backend.
	 */
	private Connection connect() throws Exception {
		Connection conn = 
			new Connection("localhost", PORT, 
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
			super("localhost", PORT, 
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
		

		/*
		public void initialize() 
			throws IOException, NoGnutellaOkException, BadHandshakeException {

			System.out.println("TestConnection::inititialize"); 
			Socket socket = Sockets.connect("localhost", Backend.PORT, 0);
			try {
				PrivilegedAccessor.setValue(this, "_socket", socket);
			} catch(Exception e) {
				fail("could not initialize test", e);
			}		
			
			try {
				Object[] args = new Object[1];
				args[0] = CONNECT_STRING;
				System.out.println("TestConnection::initialize:"+args[0]); 
				PrivilegedAccessor.invokeMethod(this, "sendString", args);
			} catch(InvocationTargetException e) {
				throw new IOException("exception from initialize");
			} catch(Exception e) {
				e.printStackTrace();
				fail("unexpected exception", e);
			}

			try {
				Object[] args = new Object[1];
				args[0] = CONNECT_STRING;
				System.out.println("TestConnection::initialize:"+args[0]); 

				args[0] = ULTRAPEER_PROPS;
				PrivilegedAccessor.invokeMethod(this, "sendHeaders", args);

				PrivilegedAccessor.invokeMethod(this, "concludeOutgoingHandshake", 
												new Class[0]);
			} catch(InvocationTargetException e) {
				throw new IOException("exception from initialize");
			} catch(Exception e) {
				e.printStackTrace();
				fail("unexpected exception", e);
			}
		}
		*/
	}

	/**
	 * 
	 */
	//private static class ZeroPointFourHandshake 
	//extends AuthenticationHandshakeResponder {
		
	//}
}
