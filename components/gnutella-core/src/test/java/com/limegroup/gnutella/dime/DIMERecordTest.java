package com.limegroup.gnutella.dime;

import java.io.*;

import junit.framework.Test;

/**
 * Tests for DIMERecord.
 */
public final class DIMERecordTest extends com.limegroup.gnutella.util.BaseTestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public DIMERecordTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DIMERecordTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testStuff() {
	    // do tests.
	}
}