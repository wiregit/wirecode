package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
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
public final class UrnHttpRequestTest extends com.limegroup.gnutella.util.BaseTestCase {

	private static final RouterService ROUTER_SERVICE = 
		new RouterService(new ActivityCallbackStub());	   


	private static final String STATUS_503 = "HTTP/1.1 503 Service Unavailable";
	private static final String STATUS_404 = "HTTP/1.1 404 Not Found";


	/**
	 * Constructs a new test instance.
	 */
	public UrnHttpRequestTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UrnHttpRequestTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {

		if(ROUTER_SERVICE.isStarted()) return;

		final File TEMP_DIR = new File("temp");
		TEMP_DIR.mkdirs();
		TEMP_DIR.deleteOnExit();
		SettingsManager.instance().setDirectories(new File[] {TEMP_DIR});
		SettingsManager.instance().setExtensions("tmp");
		String dirString = "com/limegroup/gnutella";
		File testDir = CommonUtils.getResourceFile(dirString);
		assertTrue("could not find the images directory", testDir.isDirectory());
		File[] files = testDir.listFiles();

        if ( files != null ) {
    		for(int i=0; i<files.length; i++) {
    			if(!files[i].isFile()) continue;
    			CommonUtils.copyResourceFile(dirString+"/"+files[i].getName(), 
											 new File(TEMP_DIR, files[i].getName() + ".tmp"));
    		}		
        }

		ROUTER_SERVICE.start();

		try {
			// sleep to let the file manager initialize
			Thread.sleep(4000);
		} catch(InterruptedException e) {
			fail("thread should not have been interrupted", e);
		}
		assertGreaterThan("FileManager should have loaded files", 
				   4, RouterService.getFileManager().getNumFiles());

	}

	/**
	 * Tests requests that follow the traditional "get" syntax to make sure that 
	 * the X-Gnutella-Content-URN header is always returned.
	 */
	public void testLimitReachedRequests()  throws Exception {
		int maxUploads = SettingsManager.instance().getMaxUploads();
		SettingsManager.instance().setMaxUploads(0);
		for(int i=0; i<RouterService.getFileManager().getNumFiles(); i++) {
			FileDesc fd = RouterService.getFileManager().get(i);
			String request = "/get/"+fd.getIndex()+"/"+fd.getName()+" HTTP/1.1\r\n"+
				HTTPHeaderName.GNUTELLA_CONTENT_URN.httpStringValue()+": "+
				fd.getSHA1Urn()+"\r\n\r\n";


			sendRequestThatShouldFail(HTTPRequestMethod.GET, request, fd, STATUS_503);
			//sendRequestThatShouldFail(HTTPRequestMethod.HEAD, request, fd, STATUS_503);
		}				
		SettingsManager.instance().setMaxUploads(maxUploads);
	}



	/**
	 * Test requests by URN.
	 */
	public void testHttpUrnRequest() throws Exception {
		for(int i=0; i<RouterService.getFileManager().getNumFiles(); i++) {
			FileDesc fd = RouterService.getFileManager().get(i);
			String request = "/uri-res/N2R?"+fd.getSHA1Urn().httpStringValue()+
			" HTTP/1.1\r\n\r\n";
			sendRequestThatShouldSucceed(HTTPRequestMethod.GET, request, fd);
			sendRequestThatShouldSucceed(HTTPRequestMethod.HEAD, request, fd);
		}
	}
	
	/**
	 * Test requests by URN that came from LimeWire 2.8.6.
	 */
	public void testMalformedHttpUrnRequest() throws Exception {
		for(int i=0; i<RouterService.getFileManager().getNumFiles(); i++) {
			FileDesc fd = RouterService.getFileManager().get(i);
			String request = "//uri-res/N2R?"+fd.getSHA1Urn().httpStringValue()+
			" HTTP/1.1\r\n\r\n";
			sendRequestThatShouldSucceed(HTTPRequestMethod.GET, request, fd);
			sendRequestThatShouldSucceed(HTTPRequestMethod.HEAD, request, fd);
		}
	}	

	/**
	 * Tests requests that follow the traditional "get" syntax to make sure that 
	 * the X-Gnutella-Content-URN header is always returned.
	 */
	public void testTraditionalGetForReturnedUrn() throws Exception {
		for(int i=0; i<RouterService.getFileManager().getNumFiles(); i++) {
			FileDesc fd = RouterService.getFileManager().get(i);
			String request = "/get/"+fd.getIndex()+"/"+fd.getName()+" HTTP/1.1\r\n"+
			HTTPHeaderName.GNUTELLA_CONTENT_URN.httpStringValue()+": "+fd.getSHA1Urn()+"\r\n\r\n";
			sendRequestThatShouldSucceed(HTTPRequestMethod.GET, request, fd);
			sendRequestThatShouldSucceed(HTTPRequestMethod.HEAD, request, fd);
		}				
	}

	/**
	 * Tests requests that follow the traditional "get" syntax but that also include
	 * the X-Gnutella-Content-URN header.  In these requests, both the URN and the file
	 * name and index are correct, so a valid result is expected.
	 */
	public void testTraditionalGetWithContentUrn() throws Exception {
		for(int i=0; i<RouterService.getFileManager().getNumFiles(); i++) {
			FileDesc fd = RouterService.getFileManager().get(i);
			sendRequestThatShouldSucceed(HTTPRequestMethod.GET, 
										 "/get/"+fd.getIndex()+"/"+
										 fd.getName()+ 
										 " HTTP/1.1\r\n\r\n", fd);
			sendRequestThatShouldSucceed(HTTPRequestMethod.HEAD, 
										 "/get/"+fd.getIndex()+"/"+
										 fd.getName()+ 
										 " HTTP/1.1\r\n\r\n", fd);
		}
	}

	/**
	 * Tests get requests that follow the traditional Gnutella get format and that
	 * include an invalid content URN header -- these should fail with error code
	 * 404.
	 */
	public void testTraditionalGetWithInvalidContentUrn() throws Exception {
		for(int i=0; i<RouterService.getFileManager().getNumFiles(); i++) {
			FileDesc fd = RouterService.getFileManager().get(i);
			String request = "/get/"+fd.getIndex()+"/"+fd.getName()+" HTTP/1.1\r\n"+
			HTTPHeaderName.GNUTELLA_CONTENT_URN.httpStringValue()+": "+
			"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"+"\r\n\r\n";
			sendRequestThatShouldFail(HTTPRequestMethod.GET, request, fd, STATUS_404);
			sendRequestThatShouldFail(HTTPRequestMethod.HEAD, request, fd, STATUS_404);
		}				
	}

	/**
	 * Tests to make sure that invalid traditional Gnutella get requests with
	 * matching X-Gnutella-Content-URN header values also fail with 404.
	 */
	public void testInvalidTraditionalGetWithValidContentUrn() throws Exception  {
		for(int i=0; i<RouterService.getFileManager().getNumFiles(); i++) {
			FileDesc fd = RouterService.getFileManager().get(i);
			String request = "/get/"+fd.getIndex()+"/"+fd.getName()+"invalid"+" HTTP/1.1\r\n"+
			HTTPHeaderName.GNUTELLA_CONTENT_URN.httpStringValue()+": "+fd.getSHA1Urn();
			sendRequestThatShouldFail(HTTPRequestMethod.GET, request, fd, STATUS_404);
			sendRequestThatShouldFail(HTTPRequestMethod.HEAD, request, fd, STATUS_404);
		}				
	}


	/**
	 * Sends an HTTP request that should succeed and send back all of the
	 * expected headers.
	 */
	private void sendRequestThatShouldSucceed(HTTPRequestMethod method, String request, 
											  FileDesc fd) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(request.getBytes());
		Socket sock = new TestSocket(new ByteArrayInputStream(baos.toByteArray()));
		RouterService.getUploadManager().acceptUpload(method, sock);
		String reply = sock.getOutputStream().toString();
		StringTokenizer st = new StringTokenizer(reply, "\r\n");
		boolean contentUrnHeaderPresent = false;
		boolean OKPresent = false;
		assertTrue("HTTP response headers should be present: "+fd, st.countTokens()>0);
		while(st.hasMoreTokens()) {
			String curString = st.nextToken();
			if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(curString)) {
				continue;
			} else if(HTTPHeaderName.GNUTELLA_CONTENT_URN.matchesStartOfString(curString)) {
				URN curUrn = null;
				try {
					String tmpString = HTTPUtils.extractHeaderValue(curString);
					curUrn = URN.createSHA1Urn(tmpString);
				} catch(IOException e) {
					assertTrue("unexpected exception: "+e, false);
				}
				assertEquals(HTTPHeaderName.GNUTELLA_CONTENT_URN.toString()+
							 "s should be equal for "+fd,
							 fd.getSHA1Urn(), curUrn);
				contentUrnHeaderPresent = true;
			} else if(HTTPHeaderName.CONTENT_RANGE.matchesStartOfString(curString)) {
				continue;
			} else if(HTTPHeaderName.CONTENT_TYPE.matchesStartOfString(curString)) {
				continue;
			} else if(HTTPHeaderName.CONTENT_LENGTH.matchesStartOfString(curString)) { 
				String value = HTTPUtils.extractHeaderValue(curString);
				assertEquals("sizes should match for "+fd, (int)fd.getSize(), 
							 Integer.parseInt(value));						
			} else if(HTTPHeaderName.SERVER.matchesStartOfString(curString)) {
				continue;
			} else if(curString.equals("HTTP/1.1 200 OK")) {
				OKPresent = true;
			}		
		}
		assertTrue("HTTP/1.1 200 OK should have been returned: "+fd, OKPresent);
		assertTrue("content URN header should always be reported:\r\n"+
				   fd+"\r\n"+
				   "reply: "+reply,
				   contentUrnHeaderPresent);
	}

	/**
	 * Sends an HTTP request that should fail if everything is working 
	 * correctly.
	 */
	private void sendRequestThatShouldFail(HTTPRequestMethod method, String request, 
										   FileDesc fd, String error) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(request.getBytes());
		Socket sock = new TestSocket(new ByteArrayInputStream(baos.toByteArray()));
		RouterService.getUploadManager().acceptUpload(method, sock);
		String reply = sock.getOutputStream().toString();
		StringTokenizer st = new StringTokenizer(reply, "\r\n");
		boolean sentExpectedError = false;
		String curString = st.nextToken().trim();
		assertEquals("received unexpected HTTP response", error, curString);
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
