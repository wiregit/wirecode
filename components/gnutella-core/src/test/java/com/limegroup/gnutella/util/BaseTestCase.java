package com.limegroup.gnutella.util;

import junit.framework.*;

import java.io.*;

import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.*;

public class BaseTestCase extends AssertComparisons implements ErrorCallback {
    
    protected static File _baseDir;
    protected static File _sharedDir;
    protected static File _savedDir;
    protected static File _incompleteDir;
    protected static File _settingsDir;
    protected static File _xmlDir;
    protected static File _xmlDataDir;
    protected static File _xmlSchemasDir;
    protected static Class _testClass;
    protected Thread _testThread;
    protected TestResult _testResult;

	/**
	 * Unassigned port for tests to use.
	 */
	protected static final int PORT = 49000;
    
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
    public static TestSuite buildTestSuite(Class cls) {
        _testClass = cls;
        return new LimeTestSuite(cls);
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
            suite.addTest(suite.createTest(cls, tests[ii]));
        }
        suite.addTest(warning("Warning - Full test suite has not been run."));
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
        launchBackend(Backend.PORT);
        launchBackend(Backend.REJECT_PORT);
    }

    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    public static void launchBackend() throws IOException {
        launchBackend(Backend.PORT);
    }
    
    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    public static void launchBackend(int port) throws IOException {
        
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
    static void shutdownBackends() {
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
        String testName = getName();
        System.out.println("Running test: " + testName);
        assertNotNull(testName);
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
        setupSettings();
        setupUniqueDirectories();
        
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
        setupSettings();
        setupUniqueDirectories();        
    }
    
    /**
     * Called after each test's tearDown.
     * Used to remove directories and possibly other things.
     */
    public void postTearDown() {
        cleanFiles(_baseDir, false);
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
    public static void setupSettings() {
        SettingsHandler.setShouldSave(false);
        SettingsHandler.revertToDefault();
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
     * Get tests directory from a marker resource file.
     */
    public static File getTestDirectory() throws Exception {
        File f =
            CommonUtils.getResourceFile("com/limegroup/gnutella/Backend.java");
        f = f.getCanonicalFile(); // make it a full (not relative) path
        
                 //gnutella       // limegroup    // com         // tests
        f = f.getParentFile().getParentFile().getParentFile().getParentFile();
        return new File(f, "testData");
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

        SharingSettings.setSaveDirectory(_savedDir);
        _incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
        SharingSettings.setDirectories( new File[] { _sharedDir } );

        // copy over the necessary schemas to the schemas dir
        File audioSchema = 
            CommonUtils.getResourceFile("lib/xml/schemas/audio.xsd");
        File videoSchema =
            CommonUtils.getResourceFile("lib/xml/schemas/video.xsd");
        assertTrue(audioSchema.exists());
        assertTrue(videoSchema.exists());
        assertTrue(_xmlSchemasDir.canWrite() && _xmlSchemasDir.exists());
        assertTrue(CommonUtils.copy(audioSchema, new File(_xmlSchemasDir,
                                                          "audio.xsd")));
        assertTrue(CommonUtils.copy(videoSchema, new File(_xmlSchemasDir,
                                                          "video.xsd")));

        //make sure it'll delete even if something odd happens.
        _baseDir.deleteOnExit();
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
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
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
    private static final int TIMEOUT = 2000;

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
                Message m = c.receive(timeout);
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
        return true;
    }
    
    /**
     * Returns the first message of the expected type, ignoring
     * RouteTableMessages and PingRequests.
     */
    public static Message getFirstMessageOfType(Connection c, Class type) {
        return getFirstMessageOfType(c, type, TIMEOUT);
    }

    public static Message getFirstMessageOfType(Connection c,
                                                Class type,
                                                int timeout) {
        for(int i = 0; i < 100; i++) {
            if(!c.isOpen())
                return null;

            try {
                Message m = c.receive(timeout);
                if (m instanceof RouteTableMessage)
                    ;
                else if (m instanceof PingRequest)
                    ;
                else if (type.isInstance(m))
                    return m;
                else
                    return null;  // this is usually an error....
                i = 0;
            } catch (InterruptedIOException ie) {
                return null;
            } catch (BadPacketException e) {
                // ignore...
            } catch (IOException ioe) {
                // ignore....
            }
        }
        return null;
    }
    
    public static QueryRequest getFirstQueryRequest(Connection c) {
        return getFirstQueryRequest(c, TIMEOUT);
    }
    
    public static QueryRequest getFirstQueryRequest(Connection c, int tout) {
        return (QueryRequest)getFirstMessageOfType(c, 
                                    QueryRequest.class, tout);
    }
    
    public static QueryReply getFirstQueryReply(Connection c) {
        return getFirstQueryReply(c, TIMEOUT);
    }
    
    public static QueryReply getFirstQueryReply(Connection c, int tout) {
        return (QueryReply)getFirstMessageOfType(c, QueryReply.class, tout);
    }
}       

