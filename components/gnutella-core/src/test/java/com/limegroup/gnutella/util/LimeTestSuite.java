package com.limegroup.gnutella.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.limegroup.gnutella.Backend;
import com.limegroup.gnutella.ErrorCallback;
import com.limegroup.gnutella.ErrorService;

import java.util.*;

/**
 * A modified TestSuite that allows the backends
 * to always be shutdown after tests finished,
 * regardless of halt-on-failure or halt-on-error.
 */
public class LimeTestSuite extends TestSuite implements ErrorCallback {   
    
    private TestResult _testResult = null;
    private boolean _beforeTests = false;
    private static Class _testClass;
    private static final String preTestName = "globalSetUp";
    private static final String postTestName = "globalTearDown";
    
    /**
     * Constructors...
     */
    LimeTestSuite() { super(); }
    LimeTestSuite(Class a, String b) {
        super(a, b);
        _testClass = a;
    }
    LimeTestSuite(Class a) {
        super(a);
        _testClass = a;
    }
    
    /**
     * Allows the test class to be changed.
     * Required when forcing a single test to be run from a TestCase.
     */
    public static void setTestClass(Class cls) {
        _testClass = cls;
    }
         
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
        
        // First try doing the before-tests-setup
        try {
            BaseTestCase.beforeAllTestsSetUp();
        } catch(Throwable t) {
            // If there is an error here, report it,
            // run the after all tests tear down, and exit.
            error(t);
            try {
                BaseTestCase.afterAllTestsTearDown();
            } catch(Throwable t2) {
                error(t2);
            }
            return;
        }
        
        // Then try running the preTest method
        try {
            runStaticMethod(preTestName);
        } catch(TestFailedException tfe) {
            // If it fails, run the post test and exit.
            try {
                runStaticMethod(postTestName);
            } catch(TestFailedException tfe2) {
                // oh well.
            }
            return;
        }        
        
        // Try running all the tests.
        try {
            super.run(result);
        } finally {
            // Regardless of if any fail or not, 
            // always run the last post test & after all tests methods.
            _beforeTests = false;
            ErrorService.setErrorCallback(this);
            Backend.setErrorCallback(this);
            try {
                runStaticMethod(postTestName);
            } catch(TestFailedException tfe) {
                // oh well.
            }
            try {
                BaseTestCase.afterAllTestsTearDown();
            } catch(Throwable t) {
                error(t);
            }                
        }
    }
    
    public void runStaticMethod(String name) throws TestFailedException {
        List methods = null;
        try {
            methods = getAllStaticMethods(_testClass, name);
            if(methods.isEmpty()) return;
        } catch(NoSuchMethodException e) {
            return;
        }

        for(Iterator i = methods.iterator(); i.hasNext(); ) {
            Method m = (Method)i.next();
            if ( !Modifier.isStatic(m.getModifiers()) ) {
                runTest(warning("Method "+name+" must be declared static."),
                        _testResult);
                throw new TestFailedException();
            } else if ( !Modifier.isPublic(m.getModifiers()) ) {
                runTest(warning("Method "+name+" must be declared public."),
                        _testResult);
                throw new TestFailedException();
            } else {
                try {
                    // If this method takes the class parameter, send it
                    if(m.getParameterTypes().length == 1)
                        m.invoke(null, new Object[] { _testClass });
                    // Otherwise use the no-arg invocation
                    else
                        m.invoke(null, new Object[] {});
                } catch (InvocationTargetException e) {
                    runTest(reportError(e.getCause(), _testResult), _testResult);
                    throw new TestFailedException();
                } catch (IllegalAccessException e) {
                    runTest(warning("Cannot access method: "+name, e),
                            _testResult);
                    throw new TestFailedException();
                }                
            }
        }
    }
    
    /**
     * Retrieves all static methods of the specified class (and all subclasses)
     * that are the specified name and either take no parameters or a Class
     * parameter.  The method that takes a Class parameter takes priority
     * if both are found.
     * 
     * Throws NoSuchMethodException if none are found.
     */
	public List getAllStaticMethods(Class entryClass, String methodName)
      throws NoSuchMethodException {
        List methods = new LinkedList();
        Class clazz = entryClass;
        Class[] classes = new Class[] { Class.class };
        while(clazz != null) {
            Method add = null;
            try {
                add = clazz.getDeclaredMethod(methodName, classes);
            } catch(NoSuchMethodException tryAgain) {
                // If nothing with the class parameter, try with none
                try {
                    add = clazz.getDeclaredMethod(methodName, null);
                } catch(NoSuchMethodException ignored) {}
            }
            
            // If we found a method, add it to the beginning of the list
            if(add != null)
                methods.add(0, add);

            // Try again with a superclass.
            clazz = clazz.getSuperclass();
        }
        
        if(methods.isEmpty())
            throw new NoSuchMethodException("Invalid method: " + methodName);

        return methods;
    }    
    
    /**
     * Stub for error(Throwable, String)
     */
    public void error(Throwable ex) {
        error(ex, null);
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
    public void error(Throwable ex, String detail) {
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
    		    if ( thrown instanceof AssertionFailedError )
    		        result.addFailure(this, (AssertionFailedError)thrown);
    		    else
    			    result.addError(this, thrown);
    		}
    	};
    }
    
	/**
	 * Returns a test which will fail and log a warning message.
	 */
	private static Test warning(final String message) {
		return new TestCase("warning") {
			protected void runTest() {
				fail(message);
			}
		};
	}
	
	/**
	 * Returns a test which will fail and log a warning message.
	 */
	private static Test warning(final String message, final Throwable thrown) {
		return new BaseTestCase("warning") {
		    public void preSetup() {}
		    public void postTearDown() {}
			protected void runTest() {			    
				fail(message, thrown);
			}
		};
	}
	
	private class TestFailedException extends Exception {
	    TestFailedException() { super(); }
	}
}