package com.limegroup.gnutella;

import java.io.File;
import java.io.InterruptedIOException;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.Iterator;

/**
 *  Common code to test an Ultrapeer.  Allows you to control how many 
 *  Ultrapeer and Leaf connections you maintain.  Also allows you to control
 *  their QRP tables.
 *
 *  Standard setup has the following settings:
 *  * Blocks all addresses, whitelists 127.*.*.* and 18.239.0.* .
 *  * Node is in Ultrapeer mode with GWebCache and Watchdog off.
 *  * Sharing 2 files - berkeley.txt and susheel.txt
 *  * Max number of leaf connections is 4, max number of UP connections is 3.
 *
 *  You MUST implement the following methods: getActivityCallback, numLeaves,
 *                                            numUPs, setUpQRPTables
 *  You CAN  implement the following methods: setSettings
 */
public abstract class ServerSideTestCase extends BaseTestCase {
    
    /**
     * Simple IP so a blank one isn't used.
     */
    protected static final byte[] IP = new byte[] { 1, 1, 1, 1};

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    protected static final int PORT = 6667;

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.  Subclasses can change this in
     * setSettings().
	 */
    protected static int TIMEOUT = 2000;

    /**
     * Leaf connections to the Ultrapeer.
     */
    protected static Connection LEAF[];

    /**
     * Ultrapeer connections to the Ultrapeer.
     */
    protected static Connection ULTRAPEER[];

    private static ActivityCallback callback;
    protected static ActivityCallback getCallback() {
        return callback;
    }

	/**
	 * The central Ultrapeer used in the test.
	 */
	protected static RouterService ROUTER_SERVICE;

    public ServerSideTestCase(String name) {
        super(name);
    }
    
	private static void buildConnections() throws Exception {
        for (int i = 0; i < LEAF.length; i++) {
            LEAF[i] = new Connection("localhost", PORT, 
                                     new LeafHeaders("localhost"),
                                     new EmptyResponder()
                                     );
            assertTrue(LEAF[i].isOpen());
        }
        
        for (int i = 0; i < ULTRAPEER.length; i++) {
            ULTRAPEER[i] = new Connection("localhost", PORT,
                                          new UltrapeerHeaders("localhost"),
                                          new EmptyResponder()
                                          );
            assertTrue(ULTRAPEER[i].isOpen());
        }
    }

    public static void setSettings() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
        ConnectionSettings.PORT.setValue(PORT);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir        
        CommonUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        CommonUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(33);
		ConnectionSettings.NUM_CONNECTIONS.setValue(33);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
    }

	public static void globalSetUp(Class callingClass) throws Exception {
        // calls all doSettings() for me and my children
        PrivilegedAccessor.invokeAllStaticMethods(callingClass, "setSettings",
                                                  null);
        callback=
        (ActivityCallback)PrivilegedAccessor.invokeMethod(callingClass,
                                                          "getActivityCallback",
                                                          null);
        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());

		ROUTER_SERVICE = new RouterService(callback);
		ROUTER_SERVICE.start();
		ROUTER_SERVICE.clearHostCatcher();
		ROUTER_SERVICE.connect();	
        assertEquals("unexpected port", PORT, 
					 ConnectionSettings.PORT.getValue());
        // set up ultrapeer stuff
        Integer numUPs = (Integer)PrivilegedAccessor.invokeMethod(callingClass,
                                                                 "numUPs", null);
        if ((numUPs.intValue() < 1) || (numUPs.intValue() > 30))
            throw new IllegalArgumentException("Bad value for numUPs!!!");
        ULTRAPEER = new Connection[numUPs.intValue()];

        Integer numLs = (Integer)PrivilegedAccessor.invokeMethod(callingClass,
                                                                 "numLeaves", 
                                                                 null);
        if ((numLs.intValue() < 1) || (numLs.intValue() > 30))
            throw new IllegalArgumentException("Bad value for numLs!!!");
        LEAF = new Connection[numLs.intValue()];

		connect(callingClass);
	}

    
    public void setUp() throws Exception {
        // calls all doSettings() for me and my children
        PrivilegedAccessor.invokeAllStaticMethods(this.getClass(), "setSettings",
                                                  null);
        for (int i = 0; i < ULTRAPEER.length; i++) {
            assertTrue("should be open, index = " + i, ULTRAPEER[i].isOpen());
            assertTrue("should be up -> up, index = " + i,
                       ULTRAPEER[i].isSupernodeSupernodeConnection());
        }
        for (int i = 0; i < LEAF.length; i++) {
            assertTrue("should be open, index = " + i, LEAF[i].isOpen());
            assertTrue("should be up -> up, index = " + i,
                       LEAF[i].isClientSupernodeConnection());
        }
    }


	public static void globalTearDown() throws Exception {
		ROUTER_SERVICE.disconnect();
		sleep();
        for (int i = 0; i < LEAF.length; i++)
            LEAF[i].close();
        for (int i = 0; i < ULTRAPEER.length; i++)
            ULTRAPEER[i].close();
		sleep();
	}

	protected static void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}

	/**
	 * Drains all messages 
	 */
 	protected static void drainAll() throws Exception {
        for (int i = 0; i < ULTRAPEER.length; i++)
            if(ULTRAPEER[i].isOpen()) 
                drain(ULTRAPEER[i]);
        for (int i = 0; i < LEAF.length; i++)
            if(LEAF[i].isOpen()) 
                drain(LEAF[i]);
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private static void connect(Class callingClass) throws Exception {
		buildConnections();
        // init connections
        for (int i = ULTRAPEER.length-1; i > -1; i--)
            ULTRAPEER[i].initialize();
        for (int i = 0; i < LEAF.length; i++)
            LEAF[i].initialize();

        for (int i = 0; i < ULTRAPEER.length; i++)
            assertTrue(ULTRAPEER[i].isOpen());
        for (int i = 0; i < LEAF.length; i++)
            assertTrue(LEAF[i].isOpen());

        // set up QRP tables for the child
        PrivilegedAccessor.invokeAllStaticMethods(callingClass, "setUpQRPTables",
                                                  null);

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		//sleep();
		drainAll();
		sleep();
    }

}