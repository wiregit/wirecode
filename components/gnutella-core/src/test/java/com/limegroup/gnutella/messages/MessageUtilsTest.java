package com.limegroup.gnutella.messages;

import junit.framework.*;
import junit.extensions.*;
import java.io.*;

public final class MessageUtilsTest extends TestCase {
	

	public MessageUtilsTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(MessageUtilsTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	public void testMessageUtilsPortCheck() {
		int port = -1;
		assertTrue("port should not be valid", !MessageUtils.isValidPort(port));
		port = 99999999;
		assertTrue("port should not be valid", !MessageUtils.isValidPort(port));
		port = 20;
		assertTrue("port should be valid", MessageUtils.isValidPort(port));
	}
}
