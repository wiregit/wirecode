package com.limegroup.gnutella.util;

import junit.framework.*;
import junit.extensions.*;
import java.io.*;

public final class IOUtilsTest extends TestCase {

	/**
	 * Constructs a new <tt>IOUtilsTest</tt> with the specified name.
	 */
	public IOUtilsTest(String name) {
		super(name);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Runs this suite of tests.
	 */
	public static Test suite() {
		return new TestSuite(IOUtilsTest.class);
	}

	/**
	 * Tests the readWord method.
	 */
	public void testIOUtilsReadWord() {
		String firstWord = "GET";
		String test0 = firstWord+" /get/0/file.txt";
		InputStream stream0 = new ByteArrayInputStream(test0.getBytes());
		try {
			String result = IOUtils.readWord(stream0, 3);
			assertEquals("result should equal first word", result, firstWord);
		} catch(IOException e) {
			fail("unexpected exception: "+e);
		}


		InputStream stream1 = new ByteArrayInputStream(test0.getBytes());
		try {
			String result = IOUtils.readWord(stream1, 4);
			assertEquals("result should equal first word", result, firstWord);
		} catch(IOException e) {
			fail("unexpected exception: "+e);
		}
	}
}
