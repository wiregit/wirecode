package com.limegroup.gnutella.util;

import junit.framework.*;

import com.limegroup.gnutella.ErrorCallback;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.Backend;

/**
 * A modified TestSuite that allows the backends
 * to always be shutdown after tests finished,
 * regardless of halt-on-failure or halt-on-error.
 */
public class LimeTestSuite extends TestSuite implements ErrorCallback {   
    
    private TestResult _testResult = null;
    private boolean _beforeTests = false;
    
    /**
     * Constructors...
     */
    LimeTestSuite() { super(); }
    LimeTestSuite(Class a, String b) { super(a, b); }
    LimeTestSuite(Class a) { super(a); }
         
    /**
     * Modified run.
     * Before & after tests are run, it sets the error callback
     * to itself to catch any stray errors.
     */
    public void run(TestResult result) {
        _beforeTests = true;
        _testResult = result;
        ErrorService.setErrorCallback(this);
        Backend.setErrorCallback(this);
        try {
            super.run(result);
        } finally {
            _beforeTests = false;
            ErrorService.setErrorCallback(this);
            Backend.setErrorCallback(this);
            BaseTestCase.afterAllTestsTearDown();
        }
    }
    
    /**
     * The error service callback.
     *
     * This does a somewhat unusual thing to report the error.
     * Instead of just appending it to the _testResult, it creates
     * a new TestCase and has that fail.
     * This has the advantage of reporting the error in a different
     * 'test', ensuring that people aren't confused as to how the error
     * was caused.
     */
    public void error(Throwable ex) {
        if ( _testResult != null )
            runTest(reportError(ex, _testResult), _testResult);
        else
            ex.printStackTrace();
    }
    
    /**
     * Returns a test which will fail and log a warning message.
     * Modified from JUnit's TestSuite.java's warning method.
     * Note that it does not have to extend BaseTestCase, just TestCase.
     * BaseTestCase would add needless complexity to an otherwise
     * simple error report.
     */
    private Test reportError(final Throwable thrown, final TestResult result) {
        String testName = _beforeTests ?
            "LimeTestSuite - Before Test Errors" :
            "LimeTestSuite - After Test Errors";
    	return new TestCase(testName) {
    		protected void runTest() {
    			result.addError(this, thrown);
    		}
    	};
    }    
}