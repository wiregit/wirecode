package com.limegroup.gnutella.util;

import junit.framework.*;

import java.io.File;

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
    public void preSetUp() throws Throwable {
        _testThread = Thread.currentThread();
        ErrorService.setErrorCallback(this);
        setupSettings();
        setupUniqueDirectories();
        
        System.out.println("Running test: " + getName() );
    }
    
    /**
     * Called after each test's tearDown.
     * Used to remove directories and possibly other things.
     */
    public void postTearDown() throws Throwable {
        if ( _baseDir != null )
            cleanFiles(_baseDir);
    }
    
    /**
     * Sets up settings to a pristine environment for this test.
     * Ensures that no settings are saved.
     */
    public void setupSettings() throws Throwable  {
        SettingsManager.instance(); // initialize SettingsManager
        AbstractSettings.setShouldSave(false);
        AbstractSettings.revertToDefault();
    }
    
    /**
     * Sets this test up to have unique directories.
     */
    public void setupUniqueDirectories() throws Throwable {
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
            throw new RuntimeException(ex);
        }
    }
}

