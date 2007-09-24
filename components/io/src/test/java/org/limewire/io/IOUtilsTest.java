package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public final class IOUtilsTest extends BaseTestCase {

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
		return buildTestSuite(IOUtilsTest.class);
	}

	/**
	 * Tests the readWord method.
	 */
	public void testIOUtilsReadWord() throws Exception {
		String firstWord = "GET";
		String test0 = firstWord+" /get/0/file.txt";
		InputStream stream0 = new ByteArrayInputStream(test0.getBytes());
		String result = IOUtils.readWord(stream0, 3);
		assertEquals("result should equal first word", result, firstWord);


		InputStream stream1 = new ByteArrayInputStream(test0.getBytes());
		result = IOUtils.readWord(stream1, 4);
		assertEquals("result should equal first word", result, firstWord);
	}
}
