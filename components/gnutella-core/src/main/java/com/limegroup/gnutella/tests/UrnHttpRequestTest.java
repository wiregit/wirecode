package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.net.*;
import java.util.*;

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

		RouterService rs = new RouterService(callback, router, _fileManager, 
											 new ServerAuthenticator());

	}

	/**
	 * Test requests by URN.
	 */
	public void testHttpUrnRequest() {
		for(int i=0; i<_fileManager.getNumFiles(); i++) {
			FileDesc fd = _fileManager.get(i);
			String request = "GET /uri-res/N2R?"+fd.getSHA1Urn().httpStringValue()+
			" HTTP/1.1\r\n\r\n";
			sendRequestThatShouldSucceed(request, fd);
		}
	}

	/**
	 * Tests requests that follow the traditional "get" syntax to make sure that 
	 * the X-Gnutella-Content-URN header is always returned.
	 */
	public void testTraditionalGetForReturnedUrn() {
		for(int i=0; i<_fileManager.getNumFiles(); i++) {
			FileDesc fd = _fileManager.get(i);
			String request = "GET /get/"+fd.getIndex()+"/"+fd.getName()+" HTTP/1.1\r\n"+
			HTTPHeaderName.CONTENT_URN.httpStringValue()+": "+fd.getSHA1Urn()+"\r\n\r\n";
			sendRequestThatShouldSucceed(request, fd);
		}				
	}

	/**
	 * Tests requests that follow the traditional "get" syntax but that also include
	 * the X-Gnutella-Content-URN header.  In these requests, both the URN and the file
	 * name and index are correct, so a valid result is expected.
	 */
	public void testTraditionalGetWithContentUrn() {
		for(int i=0; i<_fileManager.getNumFiles(); i++) {
			FileDesc fd = _fileManager.get(i);
			sendRequestThatShouldSucceed("GET /get/"+fd.getIndex()+"/"+fd.getName()+ 
										 " HTTP/1.1\r\n\r\n", fd);
		}
	}

	/**
	 * Tests get requests that follow the traditional Gnutella get format and that
	 * include an invalid content URN header -- these should fail with error code
	 * 404.
	 */
	public void testTraditionalGetWithInvalidContentUrn() {
		for(int i=0; i<_fileManager.getNumFiles(); i++) {
			FileDesc fd = _fileManager.get(i);
			String request = "GET /get/"+fd.getIndex()+"/"+fd.getName()+" HTTP/1.1\r\n"+
			HTTPHeaderName.CONTENT_URN.httpStringValue()+": "+
			"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"+"\r\n\r\n";
			sendRequestThatShouldFail(request, fd);
		}				
	}

	/**
	 * Sends an HTTP request that should succeed and send back all of the
	 * expected headers.
	 */
	private void sendRequestThatShouldSucceed(String request, FileDesc fd) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(request.getBytes());
			Socket sock = new TestSocket(new ByteArrayInputStream(baos.toByteArray()));
			_uploadManager.acceptUpload(sock);
			String reply = sock.getOutputStream().toString();
			StringTokenizer st = new StringTokenizer(reply, "\r\n");
			boolean contentUrnHeaderPresent = false;
			while(st.hasMoreTokens()) {
				String curString = st.nextToken();
				if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(curString)) {
					continue;
				} else if(HTTPHeaderName.CONTENT_URN.matchesStartOfString(curString)) {
					URN curUrn = null;
					try {
						curUrn = URNFactory.createUrnFromContentUrnHttpHeader(curString);
					} catch(IOException e) {
						assertTrue("unexpeced exception: "+e, false);
					}
					assertEquals(HTTPHeaderName.CONTENT_URN.toString()+"s should be equal",
								 fd.getSHA1Urn(), curUrn);
					contentUrnHeaderPresent = true;
				} else if(HTTPHeaderName.CONTENT_RANGE.matchesStartOfString(curString)) {
					continue;
				} else if(HTTPHeaderName.CONTENT_TYPE.matchesStartOfString(curString)) {
					continue;
				} else if(HTTPHeaderName.CONTENT_LENGTH.matchesStartOfString(curString)) { 
					String value = HTTPUtils.extractHeaderValue(curString);
					assertEquals("sizes should match", (int)fd.getSize(), 
								 Integer.parseInt(value));						
				} else if(HTTPHeaderName.SERVER.matchesStartOfString(curString)) {
				continue;
				}					
			}
			assertTrue("content URN header should always be reported:\r\n"+
					   fd+"\r\n"+
					   "reply: "+reply,
					   contentUrnHeaderPresent);
		} catch(IOException e) {
			assertTrue("unexpected exception: "+e, false);
		}
	}

	/**
	 * Sends an HTTP request that should fail if everything is working 
	 * correctly.
	 */
	private void sendRequestThatShouldFail(String request, FileDesc fd) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(request.getBytes());
			Socket sock = new TestSocket(new ByteArrayInputStream(baos.toByteArray()));
			_uploadManager.acceptUpload(sock);
			String reply = sock.getOutputStream().toString();
			StringTokenizer st = new StringTokenizer(reply, "\r\n");
			boolean sent404 = false;
			while(st.hasMoreTokens()) {
				String curString = st.nextToken();
				if(curString.equals("HTTP/1.1 404 Not Found")) {
					sent404 = true;
				}
			}
			assertTrue("404 status should have been sent", sent404);
		} catch(IOException e) {
			assertTrue("unexpected exception: "+e, false);
		}
	}


	/**
	 * Helper class that allows us to control the InputStream returned from
	 * a dummy socket.
	 */
	private static class TestSocket extends Socket {
		
		InputStream INPUT_STREAM;
		OutputStream OUTPUT_STREAM;

		private TestSocket(InputStream is) {
			INPUT_STREAM = is;
			OUTPUT_STREAM = new ByteArrayOutputStream();
		}

		public InputStream getInputStream() {
			return INPUT_STREAM;
		}

		public OutputStream getOutputStream() {
			return OUTPUT_STREAM;
		}
		
		public InetAddress getInetAddress() {
			try {
				return InetAddress.getLocalHost();
			} catch(UnknownHostException e) {
				return null;
			}
		}
	}
}
