package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.net.*;

/**
 * This class tests HTTP requests involving URNs, as specified in HUGE v094,
 * utilizing the X-Gnutella-Content-URN header and the 
 * X-Gnutella-Alternate-Location header.
 */
public final class UrnHttpRequestTest extends TestCase {

	/**
	 * FileManager instance to test against.
	 */
	private FileManager _fileManager;

	/**
	 * Test shared directory.
	 */
	private File _testDir;

	/**
	 * UploadManager for testing the requests.
	 */
	private UploadManager _uploadManager;

	/**
	 * Constructs a new test instance.
	 */
	public UrnHttpRequestTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(UrnHttpRequestTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		_testDir = new File(CommonUtils.getCurrentDirectory(), 
							"gui/com/limegroup/gnutella/gui/images");		
		// this is necessary to avoid getting multiple responses for 
		// given queries
		File flagsDir = new File(_testDir, "flags");
		File[] files = flagsDir.listFiles();
		for(int i=0; i<files.length; i++) {
			if(!files[i].isFile()) continue;
			assertTrue("delete needs to succeed", files[i].delete());
		}
		
		// this is necessary to avoid getting multiple responses for
		// given queries
  		File searchingFile = new File(_testDir, "searching.gif");
		searchingFile.delete();


		SettingsManager.instance().setDirectories(new File[] {_testDir});
		SettingsManager.instance().setExtensions("gif");
		ActivityCallback callback = new ActivityCallbackStub();
		_fileManager = new MetaFileManager();		
		_fileManager.initialize(callback);	
		MessageRouter router = new StandardMessageRouter(callback, _fileManager);
		_uploadManager = new UploadManager(callback, router, _fileManager);
		try {
			// sleep to let the file manager initialize
			Thread.sleep(4000);
		} catch(InterruptedException e) {
			assertTrue("thread should not have been interrupted: "+e, false);
		}
		assertTrue("FileManager should have loaded files", 
				   4 < _fileManager.getNumFiles());

	}

	/**
	 * Test requests by URN.
	 */
	public void testHttpUrnRequest() {
		
	}

	/**
	 * Helper class that allows us to control the InputStream returned from
	 * a dummy socket.
	 */
	private static class TestSocket extends Socket {
		
		InputStream INPUT_STREAM;

		public TestSocket() {
			byte[] bytes = new byte[300];
			INPUT_STREAM = new ByteArrayInputStream(bytes);
		}
		public InputStream getInputStream() {
			return INPUT_STREAM;
		}
	}
}
