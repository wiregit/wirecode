package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.util.Date;
import java.net.*;

/**
 * This class tests the methods of the <tt>RemoteFileDesc</tt> class.
 */
public final class RemoteFileDescTest extends TestCase {	  

	private byte[] TEST_GUID;

	public RemoteFileDescTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(RemoteFileDescTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		TEST_GUID = GUID.makeGuid();
	}

	/**
	 * Make sure that invalid port values are rejected.
	 */
	public void testConstructorWithInvalidPorts() {
		int[] invalidPorts = new int[2];
		invalidPorts[0] = -1;
		invalidPorts[1] = 66000;
		for(int i=0; i<invalidPorts.length; i++) {
			try {
				RemoteFileDesc rfd = 
					new RemoteFileDesc("www.limewire.org", invalidPorts[i], 
									   10, "test",
									   10, TEST_GUID, 10, false, 3,
									   false, null, null);
				fail("rfd1 should have received an exception for invalid port: "+
					 rfd.getPort());
			} catch(IllegalArgumentException e) {
				// this is expected
			}
		}
	}

	/**
	 * Make sure that valid port values are accepted.
	 */
	public void testConstructorWithValidPorts() {
		int[] validPorts = new int[2];
		validPorts[0] = 10000;
		validPorts[1] = 6000;
		for(int i=0; i<validPorts.length; i++) {
			try {
				RemoteFileDesc rfd = 
					new RemoteFileDesc("www.limewire.org", validPorts[i], 
									   10, "test",
									   10, TEST_GUID, 10, false, 3,
									   false, null, null);
			} catch(IllegalArgumentException e) {
				fail("rfd1 should not have received an exception for valid port: "+
					 validPorts[i]);
				// this is expected
			}
		}
	}
}
