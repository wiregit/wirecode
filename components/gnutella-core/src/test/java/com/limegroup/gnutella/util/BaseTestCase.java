package com.limegroup.gnutella.util;

import junit.framework.*;

import java.io.*;

import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;

public class BaseTestCase extends TestCase implements ErrorCallback {
    
    protected File _baseDir;
    protected File _sharedDir;
    protected File _savedDir;
    protected File _incompleteDir;
    protected File _settingsDir;
    protected Thread _testThread;
    protected TestResult _testResult;
    
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
    }
    
    /**
     * Build a test suite containing all of the test methods in the given class
     * @param cls The test class (must be subclassed from TestCase)
     * @return <tt>TestSuite</tt> object that can be returned by suite method
     */
    public static TestSuite buildTestSuite(Class cls) {
        return new TestSuite(cls);
    }
    
    /**
     * Build a test suite containing a single test from a specificed test class
     * @param cls The test class (must be subclassed from TestCase)
     * @param test The name of the test method in cls to be run
     * @return <tt>TestSuite</tt> object that can be returned by suite method
     */
    public static TestSuite buildTestSuite(Class cls, String test) {
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
        TestSuite suite = new TestSuite();
        for (int ii = 0; ii < tests.length; ii++) {
            suite.addTest(suite.createTest(cls, tests[ii]));
        }
        suite.addTest(suite.createTest(cls, "incompleteTest"));
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
    protected void cleanFiles(File dir) {
        File[] files = dir.listFiles();
        for(int i=0; i< files.length; i++) {
            if ( files[i].isDirectory() ) {
                cleanFiles(files[i]);
            } else {
                files[i].delete();
            }
        }
        dir.delete();
    }
    
    /**
     * Launches all backend servers.
     *
     * @throws <tt>IOException</tt> if the launching of either
     *  backend fails
     */
    public void launchAllBackends() throws IOException {
        launchBackend(true);
        launchBackend(false);
    }

    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    public void launchBackend() throws IOException {
        launchBackend(false);
    }
    
    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    public void launchBackend(boolean reject) throws IOException {
        
        /* If we've already launched the backend, don't try it again */
        int index = (reject ? 1 : 0);
        if (shutdownBackend[index]) return;

        /* Try to set up a error callback listener on the backend.
         * This will fail if some other test class has grabbed it first,
         * so don't get too excited about a failure.  Not that we do this
         * first so it will catch any errors reported by Backend startup
         */
        try {
            Backend.setErrorCallback(this);
        } catch (IOException ex) {
            System.out.println("Could not establish Backend sever listener:" +
                               ex.getMessage());
        }
        
        /* Otherwise luanch one if needed */
        shutdownBackend[index] = Backend.launch(reject);
    }

    /* Dummy test method defined so we can clean things up after all of the
     * supeclass test methods have been called
     */
    public void testBaseTestCleanup() {
        shutdownBackends();
    }
    
    /* Not a real test, but if buildTestSuite is called to build a subset of
     * the complete test suite, it will automatically include this test to 
     * warn the user that all tests have not been run
     */
    public void incompleteTest() {
        shutdownBackends();
        fail("Warning - full test suite has not been run");
    }
    
    /** SHutdown any backend servers that we started */
    private void shutdownBackends() {
        for (int ii = 0; ii < 2; ii++) {
            if (shutdownBackend[ii]) Backend.shutdown(ii == 1);
        }
        // Wait a couople seconds for any shutdown error reports.
        try { Thread.sleep(2000); } catch (InterruptedException ex) {}
        try { Backend.setErrorCallback(null); } catch (IOException ex) {}
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
        /* If the test is one of our bookeeping tests, calling the setup and
         * teardown methods is a waste of resources.   Worse, some test classes
         * will actually fail because of sloppily written race conditions.
         */
        String testName = getName();
        // System.out.println("runBare:" + testName);
        assertNotNull(testName);
        if (testName.equals("testBaseTestCleanup") ||
            testName.equals("incompleteTest")) {
            runTest();
        } 
        else {
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
    }
    
    /**
     * Called after each test's tearDown.
     * Used to remove directories and possibly other things.
     */
    public void postTearDown() {
        if ( _baseDir != null )
            cleanFiles(_baseDir);
    }
    
    /**
     * Sets up settings to a pristine environment for this test.
     * Ensures that no settings are saved.
     */
    public void setupSettings() {
        SettingsManager.instance(); // initialize SettingsManager
        AbstractSettings.setShouldSave(false);
        AbstractSettings.revertToDefault();
    }
    
    /**
     * Sets this test up to have unique directories.
     */
    public void setupUniqueDirectories() throws Exception {
        _baseDir = new File( this.getClass().getName() + "_" + hashCode() );
        _savedDir = new File(_baseDir, "saved");
        _sharedDir = new File(_baseDir, "shared");
        _settingsDir = new File(_baseDir, "settings");
        
        _baseDir.mkdirs();
        _savedDir.mkdirs();
        _sharedDir.mkdirs();
        _settingsDir.mkdirs();
        
        SettingsManager settings = SettingsManager.instance();
        settings.setSaveDirectory(_savedDir);
        _incompleteDir = settings.getIncompleteDirectory();
        settings.setDirectories( new File[] { _sharedDir } );
        PrivilegedAccessor.setValue(CommonUtils.class,
                                    "SETTINGS_DIRECTORY",
                                    _settingsDir);
        
        //make sure it'll delete even if something odd happens.
        _baseDir.deleteOnExit();
    }
    
    /**
     * Sets standard settings for a pristine test environment.
     */
    public static void setStandardSettings() {
        SettingsManager settings = SettingsManager.instance();
        AbstractSettings.revertToDefault();
		settings.setExtensions("tmp");
		ConnectionSettings.KEEP_ALIVE.setValue(4);
		SearchSettings.GUESS_ENABLED.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.USE_GWEBCACHE.setValue(false);        
        settings.setBannedIps(new String[] {"*.*.*.*"});
        settings.setAllowedIps(new String[] {"127.*.*.*"});        
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
            
    
    /* 
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
    public void error(Throwable ex) {
        if ( _testThread != Thread.currentThread() ) {
            _testResult.addError(this, ex);
            _testThread.interrupt();
        } else {
            fail("ErrorService callback error", ex);
        }
    }
}

