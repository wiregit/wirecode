package com.limegroup.gnutella.tests;

import junit.framework.*;

/**
 * This class tests the suite of tests for the classes that implement 
 * HUGE v0.94.
 */
public final class HugeTestSuite {
	
	/**
	 * Runs the suite of HUGE v0.94 tests.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Run the suite of tests on HUGE classes.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite("Huge 0.94 Tests");
		suite.addTest(UrnTest.suite());
		suite.addTest(AlternateLocationTest.suite());
		suite.addTest(UrnFactoryTest.suite());
		suite.addTest(FileDescTester.suite());
		suite.addTest(AlternateLocationCollectionTest.suite());
		suite.addTest(UrnCacheTest.suite());
		suite.addTest(QueryRequestTest.suite());
		suite.addTest(QueryReplyTest.suite());
		suite.addTest(ResponseTest.suite());
		suite.addTest(UrnHttpRequestTest.suite());
		suite.addTest(UrnRequestTest.suite());
		suite.addTest(HttpUtilsTest.suite());
		return suite;
	}
}
