package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.http.*;
import junit.framework.*;
import junit.extensions.*;

/**
 * Tests the HTTPUtils class.
 */
public final class HttpUtilsTest extends TestCase {

	/**
	 * Constructs a new HttpUtilsTest.
	 */
	public HttpUtilsTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(HttpUtilsTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Tests the method to extract a header value from an HTTP header.
	 */
	public void testExtractHeaderValue() {
		String value = "value";
		String[] headers = {
			HTTPHeaderName.CONTENT_RANGE+":"+value,
			HTTPHeaderName.CONTENT_RANGE+": "+value,
			HTTPHeaderName.CONTENT_LENGTH+":  "+value,
			HTTPHeaderName.CONTENT_TYPE+":  "+value
		};
		for(int i=0; i<headers.length; i++) {
			String curValue = HTTPUtils.extractHeaderValue(headers[i]);
			assertEquals("values should be equal", value, curValue);
		}
	}
}
