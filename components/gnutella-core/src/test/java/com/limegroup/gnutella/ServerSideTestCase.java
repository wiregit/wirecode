package com.limegroup.gnutella;

import java.io.File;
import java.net.InetAddress;
import java.util.Properties;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 *  Common code to test an Ultrapeer.  Allows you to control how many 
 *  Ultrapeer and Leaf connections you maintain.  Also allows you to control
 *  their QRP tables.
 *
 *  Standard setup has the following settings:
 *  * Blocks all addresses, whitelists 127.*.*.* and your local IP .
 *  * Node is in Ultrapeer mode with Watchdog off.
 *  * Sharing 2 files - berkeley.txt and susheel.txt
 *  * Max number of leaf connections is 4, max number of UP connections is 3.
 *
 *  You MUST implement the following methods: getActivityCallback, numLeaves,
 *                                            numUPs, setUpQRPTables
 *  You CAN  implement the following methods: setSettings
 */
public abstract class ServerSideTestCase extends LimeTestCase {

    /**
     * Simple IP so a blank one isn't used.
     */
    protected final byte[] IP = new byte[] { 1, 1, 1, 1};

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    protected final int PORT = 6667;

    /**
     * Leaf connections to the Ultrapeer.
     */
    protected BlockingConnection LEAF[];

    /**
     * Ultrapeer connections to the Ultrapeer.
     */
    protected BlockingConnection ULTRAPEER[];

    private LifecycleManager lifecycleManager;

    private ConnectionServices connectionServices;

    protected HeadersFactory headersFactory;

    protected BlockingConnectionFactory blockingConnectionFactory;
    
    protected Injector injector;

	/**
	 * The central Ultrapeer used in the test.
	 */
	//protected static RouterService ROUTER_SERVICE;

    public ServerSideTestCase(String name) {
        super(name);
    }
    
	private void buildConnections() throws Exception {
        for (int i = 0; i < LEAF.length; i++) {
            LEAF[i] = blockingConnectionFactory.createConnection("localhost", PORT);
            assertTrue(LEAF[i].isOpen());
        }
        
        for (int i = 0; i < ULTRAPEER.length; i++) {
            ULTRAPEER[i] = blockingConnectionFactory.createConnection("localhost", PORT);
            assertTrue(ULTRAPEER[i].isOpen());
        }
    }

    public final void doSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*", localIP});
        ConnectionSettings.PORT.setValue(PORT);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir        
        FileUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        FileUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(33);
		ConnectionSettings.NUM_CONNECTIONS.setValue(33);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        
        setSettings();
    }

	/**
	 * Can be overridden in subclasses.
	 */
	public void setSettings() throws Exception {
	}
	
	/**
	 * Can be overriden in subclasses. Returns 2 by default.
	 */
	public int getNumberOfUltrapeers() {
	    return 2;
	}
	
	/**
     * Can be overriden in subclasses. Returns 2 by default.
     */
	public int getNumberOfLeafpeers() {
	    return 2;
	}
	
	/**
	 * Can be overridden in subclasses.
	 * @throws Exception 
	 */
	public void setUpQRPTables() throws Exception {
	    
	}
    
    public void setUp() throws Exception {
        setUp(LimeTestUtils.createInjector());
    }
    
    public void setUp(Injector injector) throws Exception {
        doSettings();
        
        this.injector = injector;
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        headersFactory = injector.getInstance(HeadersFactory.class);
        blockingConnectionFactory = injector.getInstance(BlockingConnectionFactory.class);

        assertEquals("unexpected port", PORT, ConnectionSettings.PORT.getValue());

        lifecycleManager.start();

        connectionServices.connect();
        
        assertEquals("unexpected port", PORT, ConnectionSettings.PORT.getValue());

        // set up ultrapeer stuff
        int numUPs = getNumberOfUltrapeers();
        if (numUPs < 0 || numUPs > 30)
            throw new IllegalArgumentException("Bad value for numUPs!!!");
        
        ULTRAPEER = new BlockingConnection[numUPs];

        int numLs =  getNumberOfLeafpeers();
        if (numLs < 0 || numLs > 30)
            throw new IllegalArgumentException("Bad value for numLs!!!");
        LEAF = new BlockingConnection[numLs];

        connect();
        
        for (int i = 0; i < ULTRAPEER.length; i++) {
            assertTrue("should be open, index = " + i, ULTRAPEER[i].isOpen());
            assertTrue("should be up -> up, index = " + i,
                       ULTRAPEER[i].getConnectionCapabilities().isSupernodeSupernodeConnection());
        }
        for (int i = 0; i < LEAF.length; i++) {
            assertTrue("should be open, index = " + i, LEAF[i].isOpen());
            assertTrue("should be up -> up, index = " + i,
                       LEAF[i].getConnectionCapabilities().isClientSupernodeConnection());
        }
    }


	public void tearDown() throws Exception {
	    // there was an error in setup, return so we can see the actual exception
	    if (connectionServices == null)
	        return;
	    
	    connectionServices.disconnect();
		sleep();
        for (int i = 0; i < LEAF.length; i++)
            LEAF[i].close();
        for (int i = 0; i < ULTRAPEER.length; i++)
            ULTRAPEER[i].close();
		lifecycleManager.shutdown();
	}

	protected void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}

	/**
	 * Drains all messages 
	 */
 	protected void drainAll() throws Exception {
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
    private void connect() throws Exception {
		buildConnections();
        // init connections
        for (int i = ULTRAPEER.length-1; i > -1; i--)
            ULTRAPEER[i].initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        for (int i = 0; i < LEAF.length; i++)
            LEAF[i].initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);

        for (int i = 0; i < ULTRAPEER.length; i++)
            assertTrue(ULTRAPEER[i].isOpen());
        for (int i = 0; i < LEAF.length; i++)
            assertTrue(LEAF[i].isOpen());

        // set up QRP tables for the child
        setUpQRPTables();
    }
    
    /** Builds a conenction with default headers */
    protected BlockingConnection createLeafConnection() throws Exception {
        return createConnection(headersFactory.createLeafHeaders("localhost"));
    }
    
    /** Builds an ultrapeer connection with default headers */
    protected BlockingConnection createUltrapeerConnection() throws Exception {
        return createConnection(headersFactory.createUltrapeerHeaders("localhost"));
    }
    
    /** Builds a single connection with the given headers. */
    protected BlockingConnection createConnection(Properties headers) throws Exception {
        BlockingConnection c = blockingConnectionFactory.createConnection("localhost", PORT);
        c.initialize(headers, new EmptyResponder(), 1000);
        assertTrue(c.isOpen());
        return c;
    }
}
