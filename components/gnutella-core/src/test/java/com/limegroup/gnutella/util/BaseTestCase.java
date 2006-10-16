package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.limegroup.gnutella.Backend;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ErrorCallback;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.UrnCallback;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SettingsHandler;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;

public abstract class BaseTestCase extends AssertComparisons implements ErrorCallback {
    
    protected static File _baseDir;
    protected static File _sharedDir;
    protected static File _savedDir;
    protected static File _incompleteDir;
    protected static File _settingsDir;
    protected static File _xmlDir;
    protected static File _xmlDataDir;
    protected static File _xmlSchemasDir;
    protected static Class _testClass;
    private   static Timer _testKillerTimer = new Timer(true);
    protected static String _currentTestName;
    protected Thread _testThread;
    protected TestResult _testResult;
    protected TimerTask _testKiller;
    protected long _startTimeForTest;

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
    public BaseTestCase(String name) {
        super(name);
        _testClass = getClass();
    }
    
    /**
     * Build a test suite containing all of the test methods in the given class
     * @param cls The test class (must be subclassed from TestCase)
     * @return <tt>TestSuite</tt> object that can be returned by suite method
     */
    public static TestSuite buildSingleTestSuite(Class cls) {
        _testClass = cls;
        
        String method = System.getProperty("junit.test.method");
        if(method != null) {
            method = method.trim();
            if(!"".equals(method) && !"${method}".equals(method)) {
                
                StringTokenizer st = new StringTokenizer(method, ",");
                List l = new LinkedList();
                while(st.hasMoreTokens())
                    l.add(st.nextToken());
                
                String[] tests = (String[])l.toArray(new String[l.size()]);
                return buildTestSuite(cls, tests);
            }
        }
        return new LimeTestSuite(cls);
    }
    
    public static TestSuite buildTestSuite(Class cls) {
        TestSuite suite = buildSingleTestSuite(cls);
        String timesP = System.getProperty("junit.test.times","1");
        int times = 1 ;
        try {
            times = Integer.parseInt(timesP);
        } catch (NumberFormatException ignored ){}
        
        List tests = new LinkedList();
        for (Enumeration e = suite.tests();e.hasMoreElements();) 
            tests.add(e.nextElement());
        
        while (times-- > 1) {
            for (Iterator iter = tests.iterator(); iter.hasNext();) 
                suite.addTest((Test) iter.next());
        }
        
        // add a warning if we are running individual tests
        if (!System.getProperty("junit.test.method","${method}").equals("${method}"))
            suite.addTest(warning("Warning - Full test suite has not been run."));
        
        return suite;
    }
    
    /**
     * Build a test suite containing a single test from a specificed test class
     * @param cls The test class (must be subclassed from TestCase)
     * @param test The name of the test method in cls to be run
     * @return <tt>TestSuite</tt> object that can be returned by suite method
     */
    public static TestSuite buildTestSuite(Class cls, String test) {
        _testClass = cls;
        return buildTestSuite(cls, new String[]{test});
    }
    
    /**
     * Build a test suite containing a set of tests from a specificed test class
     * @param cls The test class (must be subclassed from TestCase)
     * @param test Array containing the names of the test methods in cls to be 
     * run
     * @return <tt>TestSuite</tt> object that can be returned by suite method
     */
    public static TestSuite buildTestSuite(Class cls, String[] tests) {
        _testClass = cls;
        TestSuite suite = new LimeTestSuite();
        LimeTestSuite.setTestClass(cls);
        for (int ii = 0; ii < tests.length; ii++) {
            if (!tests[ii].startsWith("test"))
                tests[ii]="test"+tests[ii];
            suite.addTest(TestSuite.createTest(cls, tests[ii]));
        }
        return suite;
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
     * Recursively delete a directory.
     */
    protected static void cleanFiles(File dir, boolean deleteDirs) {
        if ( dir == null ) return;
        
        File[] files = dir.listFiles();
        for(int i=0; i< files.length; i++) {
            if ( files[i].isDirectory() ) {
                cleanFiles(files[i], deleteDirs);
            } else {
                files[i].delete();
            }
        }
        if ( deleteDirs ) dir.delete();
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
    
    /*
     * This is modified to run 'preSetUp' and 'postTearDown' as methods
     * which all tests will run, regardless of their implementation
     * (or lack of) of setUp and tearDown.
     *
     * It is also modified so that if setUp throws something, tearDown
     * will still be run.
     *
	 */
	public void runBare() throws Throwable {
        _currentTestName = getName();
        System.out.println("Running test: " + _currentTestName);
        assertNotNull(_currentTestName);
        try {
            preSetUp();
            setUp();
            runTest();
        } finally {
            try {
                tearDown();
            } finally {
                postTearDown();
            }
        }
    }
    
    /**
     * Intercepted to allow us to get a handle to the test result, so we can 
     * add errors from the ErrorService callback (giving us errors that were
     * triggered from outside of the test thread).
     */
    public void run(TestResult result) {
        _testResult = result;
        super.run(result);
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
        _testThread = Thread.currentThread();
        ErrorService.setErrorCallback(this);
        
        setupUniqueDirectories();
        setupSettings();
        setupTestTimer();
        
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
        // SystemUtils must pretend to not be loaded, so the idle
        // time isn't counted.
        // For tests that are testing SystemUtils specifically, they can
        // set loaded to true.
        SystemUtils.getIdleTime(); // make it loaded.
        // then unload it.
        PrivilegedAccessor.setValue(SystemUtils.class, "isLoaded", Boolean.FALSE);
        
        setupUniqueDirectories();
        setupSettings();
    }
    
    /**
     * Called after each test's tearDown.
     * Used to remove directories and possibly other things.
     */
    public void postTearDown() {
        cleanFiles(_baseDir, false);
        stopTestTimer();
    }
    
    /**
     * Runs after all tests are completed.
     */
    public static void afterAllTestsTearDown() throws Throwable {
        cleanFiles(_baseDir, true);
        shutdownBackends();
    }
    
    /**
     * Sets up the TimerTask to kill the running test after a certian amount of time.
     */
    private final void setupTestTimer() {
        _startTimeForTest = System.currentTimeMillis();
        _testKiller = new TimerTask() {
            public void run() {
                long now = System.currentTimeMillis();
                error(new RuntimeException("Stalled!  Took " +
                                    (now - _startTimeForTest) + " ms."),
                      "Test Took Too Long");
            }
        };
        // kill in a bit.
        _testKillerTimer.schedule(_testKiller, 7 * 60 * 1000);
    }
    
    /**
     * Stops the test timer since the test finished.
     */
    private final void stopTestTimer() {
        _testKiller.cancel();
        _testKiller = null;
    }
    
    /**
     * Sets up settings to a pristine environment for this test.
     * Ensures that no settings are saved.
     */
    public static void setupSettings() throws Exception{
        SettingsHandler.setShouldSave(false);
        SettingsHandler.revertToDefault();
        ConnectionSettings.DISABLE_UPNP.setValue(true);
        ConnectionSettings.ALLOW_DUPLICATE.setValue(true);
        ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.setValue(true);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        SearchSettings.ENABLE_SPAM_FILTER.setValue(false);
        SharingSettings.setSaveDirectory(_savedDir);
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(false);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(false);
        _incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
        setSharedDirectories( new File[] { _sharedDir } );
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
        
        PrivilegedAccessor.setValue(CommonUtils.class,
                                    "SETTINGS_DIRECTORY",
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

    /**
     * Fails the test with an AssertionFailedError and another
     * error as the root cause.
     */
    static public void fail(Throwable e) {
        fail(null, e);
    }
    
    /**
     * Fails the test with an AssertionFailedError and another
     * error as the root cause, with a message.
     */
    static public void fail(String message, Throwable e) {
        throw new UnexpectedExceptionError(message, e);
    }
            
    
    /**
     * Stub for error(Throwable, String)
     */
    public void error(Throwable ex) {
        error(ex, null);
    }
    
    /** 
     * This is the callback from ErrorService, and why we implement
     * ErrorCallback.
     *
     * It is used to catch errors that may or may not be inside of the
     * test thread.  If it is in the thread, we can just rethrow the
     * error, and the test will fail as normal. If it is outside of the
     * thread, we want the test results to remember the error, but we 
     * must allow the test to continue as normal, possibly succeeding, 
     * failing or erroring.
     *
     * Note that while the XML formatter can easily handle the case of
     * multiple failures/errors in a single test, the XML->HTML converter
     * doesn't do that good of a job.  It correctly lists the amount of
     * errors/failures, but it will only write the last one as the status
     * of the test, and will also only write the last one as the
     * message/stacktrace.
     */
    public void error(Throwable ex, String detail) {
        ex = new UnexpectedExceptionError(detail, ex); // remember the detail & stack trace of the ErrorService.
        if ( _testThread != Thread.currentThread() ) {
            _testResult.addError(this, ex);
            _testThread.interrupt();
        } else {
            fail("ErrorService callback error", ex);
        }
    }
    
    /**
     * Returns a test which will fail and log a warning message.
     * Copied from JUnit's TestSuite.java
     * Note that it does not have to extend BaseTestCase, just TestCase.
     * BaseTestCase would add needless complexity to an otherwise
     * simple failure message.
     */
    private static Test warning(final String message) {
    	return new TestCase("warning") {
    		protected void runTest() {
    			fail(message);
    		}
    	};
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
            r[i] = new ManagedThread() {
                public void managedRun() {
                    try {
                        drain(conns[index],TIMEOUT);
                    } catch (Exception bad) {
                        ErrorService.error(bad);
                    }
                }
            };
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
                Message m=MessageFactory.read(socket.getInputStream(), Message.N_TCP);
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
            drainers[i] = new ManagedThread() {
                public void managedRun() {
                    try {
                        Message m = 
                            getFirstInstanceOfMessageType(connections[index],type);
                        assertNull(m);
                    } catch (BadPacketException bad) {
                        fail(bad);
                }
            }};
            drainers[i].start();
        }
        for(int i = 0;i < drainers.length;i++)
            drainers[i].join();
    }
}       

