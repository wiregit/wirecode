package com.limegroup.gnutella.util;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.limewire.concurrent.ManagedThread;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorService;
import org.limewire.setting.SettingsHandler;
import org.limewire.util.BaseTestCase;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.SystemUtils;

import com.limegroup.gnutella.Backend;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeCoreGlue;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.UrnCallback;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UISettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;

@SuppressWarnings("unchecked")
public abstract class LimeTestCase extends BaseTestCase implements ErrorCallback {
    
    protected static File _baseDir;
    protected static File _sharedDir;
    protected static File _savedDir;
    protected static File _incompleteDir;
    protected static File _settingsDir;
    protected static File _xmlDir;
    protected static File _xmlDataDir;
    protected static File _xmlSchemasDir;
    
	/**
	 * Unassigned port for tests to use.
	 */
	protected static final int TEST_PORT = 49000;
    
    /* Flag indicate that we have launched the backend process and should
     * shut it down when we are finished.
     * NOTE. this has to be declared static because a separate TestCase is
     * insantiated for each test.
     */
    private static boolean[] shutdownBackend = new boolean[]{false, false};
    
    /**
     * The base constructor.
     * Nothing should ever be initialized in the constructor.
     * This is because of the way JUnit sets up tests --
     * It first builds a new instance of the class for every possible test,
     * then it runs through those instances, calling the appropriate test.
     * All pre & post initializations that are necessary for every test
     * should be in the new 'preSetUp' and 'postTearDown' methods.
     */    
    public LimeTestCase(String name) {
        super(name);
    }
    
    /**
     * Get test save directory
     */
    public File getSaveDirectory() {
        return _savedDir;
    }
    
    /**
     * Get test shared directory
     */
    public File getSharedDirectory() {
        return _sharedDir;
    }
    
    /**
     * Launches all backend servers.
     *
     * @throws <tt>IOException</tt> if the launching of either
     *  backend fails
     */
    public static void launchAllBackends() throws IOException {
        launchBackend(Backend.BACKEND_PORT);
        launchBackend(Backend.REJECT_PORT);
    }

    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    public static void launchBackend() throws IOException {
        launchBackend(Backend.BACKEND_PORT);
    }
    
    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    private static void launchBackend(int port) throws IOException {
        
        /* If we've already launched the backend, don't try it again */
        int index = (port == Backend.REJECT_PORT ? 1 : 0);
        if (shutdownBackend[index]) return;
        
        /* Otherwise launch one if needed */
        shutdownBackend[index] = Backend.launch(port);
    }
    
    /**
     * Shutdown any backend servers that we started
     * This must be static so LimeTestSuite can call it.
     * (Which implicitly means that shutdownBackend[] must
     *  stay static also.)
     */
    private static void shutdownBackends() {
        for (int ii = 0; ii < 2; ii++) {
            if (shutdownBackend[ii]) Backend.shutdown(ii == 1);
        }
        // Wait a couople seconds for any shutdown error reports.
        try { Thread.sleep(2000); } catch (InterruptedException ex) {}
        Backend.setErrorCallback(null);
    }
    
    /**
     * Called before each test's setUp.
     * Used to determine which thread the test is running in,
     * set up the testing directories, and possibly print
     * debugging information (such as the current test being run)
     * This must also set the ErrorService's callback, so it
     * associates with the correct test object.
     */
    public void preSetUp() throws Exception {
        super.preSetUp();        
        setupUniqueDirectories();
        setupSettings();
        
        // The backend must also have its error callback reset
        // for each test, otherwise it could send errors to a stale
        // TestResult object (one whose test hasn't started or already ended).
        // But, we don't want to let the actual Backend class set it,
        // because that could provide an infinite loop of writing
        // to the socket, then reading it and rewriting it,
        // then reading it and rewriting it, etc...
        if (!(this instanceof Backend) )
            Backend.setErrorCallback(this);
    }
    
    /**
     * Called statically before any settings.
     */
    public static void beforeAllTestsSetUp() throws Throwable {        
        setupUniqueDirectories();
        setupSettings();
        // SystemUtils must pretend to not be loaded, so the idle
        // time isn't counted.
        // For tests that are testing SystemUtils specifically, they can
        // set loaded to true.
        SystemUtils.getIdleTime(); // make it loaded.
        // then unload it.
        PrivilegedAccessor.setValue(SystemUtils.class, "isLoaded", Boolean.FALSE);
    }
    
    /**
     * Called after each test's tearDown.
     * Used to remove directories and possibly other things.
     */
    public void postTearDown() {
        cleanFiles(_baseDir, false);
        super.postTearDown();
    }
    
    /**
     * Runs after all tests are completed.
     */
    public static void afterAllTestsTearDown() throws Throwable {
        cleanFiles(_baseDir, true);
        shutdownBackends();
    }
    
    /**
     * Sets up settings to a pristine environment for this test.
     * Ensures that no settings are saved.
     */
    public static void setupSettings() throws Exception{
        SettingsHandler.setShouldSave(false);
        SettingsHandler.revertToDefault();
        ConnectionSettings.FILTER_CLASS_C.setValue(false);
        ConnectionSettings.DISABLE_UPNP.setValue(true);
        ConnectionSettings.ALLOW_DUPLICATE.setValue(true);
        ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.setValue(true);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        SearchSettings.ENABLE_SPAM_FILTER.setValue(false);
        SharingSettings.setSaveDirectory(_savedDir);
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(false);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(false);
        if(!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance())
            UISettings.PRELOAD_NATIVE_ICONS.setValue(false);
        _incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
        setSharedDirectories( new File[] { _sharedDir } );
        LimeCoreGlue.install();
    }
    
    /**
     * Creates a new directory prepended by the given name.
     */
    public static File createNewBaseDirectory(String name) throws Exception {
        File t = getTestDirectory();
        File f = new File(t, name);
        
        int append = 1;
        while ( f.exists() ) {
            f = new File(t, name + "_" + append);
            append++;
        }
        
        return f.getCanonicalFile();
    }
    
    /**
     * Sets this test up to have unique directories.
     */
    public static void setupUniqueDirectories() throws Exception {
        
        if( _baseDir == null ) {
            _baseDir = createNewBaseDirectory( _testClass.getName() );
        }
        _savedDir = new File(_baseDir, "saved");
        _sharedDir = new File(_baseDir, "shared");
        _settingsDir = new File(_baseDir, "settings");
        _xmlDir = new File(_settingsDir, "xml");
        _xmlDataDir = new File(_xmlDir, "data");
        _xmlSchemasDir = new File(_xmlDir, "schemas");

        _baseDir.mkdirs();
        _savedDir.mkdirs();
        _sharedDir.mkdirs();
        _settingsDir.mkdirs();
        _xmlDir.mkdirs();
        _xmlDataDir.mkdirs();
        _xmlSchemasDir.mkdirs();
        
        // set the settings directory, then immediately change it.
        LimeCoreGlue.preinstall();
        PrivilegedAccessor.setValue(CommonUtils.class,
                                    "settingsDirectory",
                                    _settingsDir);

        File f = getRootDir();

        // Expand the xml.war file.
        File xmlWar = new File(f, "gui/xml.war");
        assertTrue(xmlWar.exists());
        Expand.expandFile(xmlWar, _settingsDir);
        //make sure it'll delete even if something odd happens.

        // Expand the update.ver file.
        File updateVer = new File(f, "gui/update.ver");
        assertTrue(updateVer.exists());
        Expand.expandFile(updateVer, _settingsDir);

        _baseDir.deleteOnExit();
    }
    
    /**
     * Get tests directory from a marker resource file.
     */
    public static File getTestDirectory() throws Exception {
        return new File(getRootDir(), "testData");
    }   
    
    public static File getGUIDir() throws Exception {
        return new File(getRootDir(), "gui");
    }
    
    public static File getCoreDir() throws Exception {
        return new File(getRootDir(), "core");        
    }
    
    public static File getRootDir() throws Exception {
        // Get a marker file.
        File f = CommonUtils.getResourceFile("com/limegroup/gnutella/Backend.java");
        f = f.getCanonicalFile();
                 //gnutella       // limegroup    // com         // tests       // .
        return f.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();        
    }
    
    /**
     * Sets standard settings for a pristine test environment.
     */
    public static void setStandardSettings() {
        SettingsHandler.revertToDefault();
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("tmp");
		ConnectionSettings.NUM_CONNECTIONS.setValue(4);
		SearchSettings.GUESS_ENABLED.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        try {
            FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                    new String[] {"127.*.*.*",InetAddress.getLocalHost().getHostAddress()});
        }catch(UnknownHostException bad) {
            fail(bad);
        }
    }
    
    
    ///////////////////////// Useful Testing Methods //////////////////////////
    public static void setSharedDirectories(File[] dirs) {
        Set set = new HashSet(Arrays.asList(dirs));
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(set);
    }
    
    public static Set calculateAndCacheURN(File f) throws Exception {
        final Set myUrns = new HashSet(1);
        UrnCallback blocker = new UrnCallback() {
            public void urnsCalculated(File file, Set urns) {
                synchronized(myUrns) {
                    myUrns.addAll(urns);
                    myUrns.notify();
                }
            }
            
            public boolean isOwner(Object o) {
                return false;
            }
        };
        
        synchronized(myUrns) {
            UrnCache.instance().calculateAndCacheUrns(f, blocker);
            if(myUrns.isEmpty()) // only wait if it didn't fill immediately.
                myUrns.wait(3000);
        }
        
        return myUrns;
    }
    
    
    private static final int TIMEOUT = 2000;
    
    /**
     * Sends a pong through the connection to keep it alive.
     */
    public static void keepAlive(Connection c) throws IOException {
        PingReply pr = PingReply.create(GUID.makeGuid(), (byte)1);
        c.send(pr);
        c.flush();
    }
    
    /**
     * Sends a pong through all connections to keep them alive.
     */
    public static void keepAllAlive(Connection[] cs) throws IOException {
        for(int i = 0; i < cs.length; i++) {
            PingReply pr = PingReply.create(GUID.makeGuid(), (byte)1);
            cs[i].send(pr);
            cs[i].flush();
        }
    }    

    /** 
	 * Tries to receive any outstanding messages on c 
	 *
     * @return <tt>true</tt> if this got a message, otherwise <tt>false</tt>
	 */
    public static boolean drain(Connection c) throws IOException {
        return drain(c, TIMEOUT);
    }
    
    public static boolean drain(Connection c, int timeout) throws IOException {
        if(!c.isOpen())
            return false;

        boolean ret=false;
        for(int i = 0; i < 100; i++) {
            try {
                c.receive(timeout);
                ret = true;
                i = 0;
            } catch (InterruptedIOException e) {
				// we read a null message or received another 
				// InterruptedIOException, which means a messages was not 
				// received
                return ret;
            } catch (BadPacketException e) {
                // ignore...
            }
        }
        return ret;
    }
    
    /**
     * Tries to drain all messages from the array of connections.
     */
 	public static void drainAll(Connection[] conns) throws Exception {
 	    drainAll(conns, TIMEOUT);
 	}
    
    /**
     * drains all messages from the given connections simultaneously.
     */
    public static void drainAllParallel(final Connection [] conns) {
        Thread []r = new Thread[conns.length];
        for (int i = 0; i < conns.length; i++) {
            final int index = i;
            r[i] = ThreadExecutor.newManagedThread(new Runnable() {
                public void run() {
                    try {
                        drain(conns[index],TIMEOUT);
                    } catch (Exception bad) {
                        ErrorService.error(bad);
                    }
                }
            });
            r[i].start();
        }
        
        for (int i = 0; i < r.length; i++) {
            try {
                r[i].join();
            } catch (InterruptedException ignored) {}
        }
    }
 	
 	public static void drainAll(Connection[] cs, int tout) throws IOException {
        for (int i = 0; i < cs.length; i++) {
            if (cs[i].isOpen())
                drain(cs[i], tout);
        }
 	}
 	
 	/**
 	 * Returns true if no messages beside expected ones (such as QRP, Pings)
 	 * were received.
 	 */
 	public static boolean noUnexpectedMessages(Connection c) {
 	    return noUnexpectedMessages(c, TIMEOUT);
 	}
 	
 	public static boolean noUnexpectedMessages(Connection c, int timeout) {
        for(int i = 0; i < 100; i++) {
            if(!c.isOpen())
                return true;
            try {
                Message m = c.receive(timeout);
                if (m instanceof RouteTableMessage)
                    ;
                else if (m instanceof PingRequest)
                    ;
                else // we should never get any other sort of message...
                    return false;
                i = 0;
            } catch (InterruptedIOException ie) {
                return true;
            } catch (BadPacketException e) {
                // ignore....
            } catch (IOException ioe) {
                // ignore....
            }
        }
        throw new RuntimeException("No IIOE or Message after 100 iterations");
    }
    
    /**
     * Returns the first message of the expected type, ignoring
     * RouteTableMessages and PingRequests.
     */
    public static <T extends Message> T getFirstMessageOfType(Connection c, Class<T> type) {
        return getFirstMessageOfType(c, type, TIMEOUT);
    }

    public static <T extends Message> T getFirstMessageOfType(Connection c,
                                                Class<T> type,
                                                int timeout) {
        for(int i = 0; i < 100; i++) {
            if(!c.isOpen()){
                //System.out.println(c + " is not open");
                return null;
            }

            try {
                Message m = c.receive(timeout);
                //System.out.println("m: " + m + ", class: " + m.getClass());
                if (m instanceof RouteTableMessage)
                    ;
                else if (m instanceof PingRequest)
                    ;
                else if (type.isInstance(m))
                    return (T)m;
                else
                    return null;  // this is usually an error....
                i = 0;
            } catch (InterruptedIOException ie) {
                //ie.printStackTrace();
                return null;
            } catch (BadPacketException e) {
               // e.printStackTrace();
                // ignore...
            } catch (IOException ioe) {
                //ioe.printStackTrace();
                // ignore....
            }
        }
        throw new RuntimeException("No IIOE or Message after 100 iterations");
    }
    
    public static <T extends Message> T getFirstInstanceOfMessageType(Connection c,
                          Class<T> type) throws BadPacketException {
        return getFirstInstanceOfMessageType(c, type, TIMEOUT);
    }

    public static <T extends Message> T getFirstInstanceOfMessageType(Connection c,
               Class<T> type, int timeout) throws BadPacketException {
        for(int i = 0; i < 200; i++) {
            if(!c.isOpen()){
                //System.out.println(c + " is not open");
                return null;
            }

            try {
                Message m = c.receive(timeout);
                //System.out.println("m: " + m + ", class: " + m.getClass());
                if (type.isInstance(m))
                    return (T)m;
                i = 0;
            } catch (InterruptedIOException ie) {
//                ie.printStackTrace();
                return null;            
            } catch (IOException iox) {
                //ignore iox
            }
        }
        throw new RuntimeException("No IIOE or Message after 100 iterations");
    }
    
    /**
     * @return the first message of type <pre>type</pre>.  Read messages within
     * the time out, so it's possible to wait upto almost 2 * timeout for this
     * method to return
     */
    public static <T extends Message> T getFirstInstanceOfMessage(Socket socket, Class<T> type, 
                           int timeout) throws IOException, BadPacketException {
        int oldTimeout = socket.getSoTimeout();
        try {
        for(int i=0; i<200; i++) { 
            if(socket.isClosed())
                return null;
            try {
                socket.setSoTimeout(timeout);
                Message m=MessageFactory.read(socket.getInputStream(), Network.TCP);
                if(type.isInstance(m))
                    return (T)m;
                else if(m == null) //interruptedIOException thrown
                    return null;                    
                i=0;
            } catch(InterruptedIOException iiox) {
                return null; 
            } 
        }
        } finally { //before we return reset the so-timeout
            socket.setSoTimeout(oldTimeout);
        }
        return null;
    }

    public static QueryRequest getFirstQueryRequest(Connection c) {
        return getFirstQueryRequest(c, TIMEOUT);
    }
    
    public static QueryRequest getFirstQueryRequest(Connection c, int tout) {
        return getFirstMessageOfType(c, QueryRequest.class, tout);
    }
    
    public static QueryReply getFirstQueryReply(Connection c) {
        return getFirstQueryReply(c, TIMEOUT);
    }
    
    public static QueryReply getFirstQueryReply(Connection c, int tout) {
        return getFirstMessageOfType(c, QueryReply.class, tout);
    }
    
    public static void failIfAnyArrive(final Connection []connections, final Class type) 
    throws Exception {
        Thread [] drainers = new ManagedThread[connections.length];
        for (int i = 0; i < connections.length; i++) {
            final int index = i;
            drainers[i] = ThreadExecutor.newManagedThread(new Runnable() {
                public void run() {
                    try {
                        Message m = 
                            getFirstInstanceOfMessageType(connections[index],type);
                        assertNull(m);
                    } catch (BadPacketException bad) {
                        fail(bad);
                    }
                }
            });
            drainers[i].start();
        }
        for(int i = 0;i < drainers.length;i++)
            drainers[i].join();
    }
}       

