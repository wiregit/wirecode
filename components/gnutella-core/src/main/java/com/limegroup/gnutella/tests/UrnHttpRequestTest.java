package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;

/**
 * This class tests HTTP requests involving URNs, as specified in HUGE v094,
 * utilizing the X-Gnutella-Content-URN header and the 
 * X-Gnutella-Alternate-Location header.
 */
public final class UrnHttpRequestTest extends TestCase {

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

	/**
	 * Tests URN requests on the FileManager.
	 */
	public void testUrnRequests() {
		SettingsManager.instance().setDirectories(new File[] {
			new File(CommonUtils.getCurrentDirectory(), "core/com/limegroup/gnutella/tests")});
		SettingsManager.instance().setExtensions("java");
		FileManager fm = new FileManager();		
		fm.initialize(new ActivityCallbackStub());	   
		try {
			// sleep to let the file manager initialize
			Thread.sleep(4000);
		} catch(InterruptedException e) {
			assertTrue("thread should not have been interrupted: "+e, false);
		}
		assertTrue("FileManager should have loaded files", 4 < fm.getNumFiles());
		for(int i=0; i<fm.getNumFiles(); i++) {
			FileDesc fd = fm.get(i);
			URN urn = fd.getSHA1Urn();
			assertEquals("File indexes should match", i, fm.getFileIndexForUrn(urn));
			assertEquals("FileDescs should match", fd, fm.getFileDescForUrn(urn));

			// first set does not include any requested types
			Set requestedUrnSet0 = new HashSet();
			Set requestedUrnSet1 = new HashSet();
			Set requestedUrnSet2 = new HashSet();
			Set requestedUrnSet3 = new HashSet();
			requestedUrnSet1.add(UrnType.ANY_TYPE);
			requestedUrnSet2.add(UrnType.SHA1);
			requestedUrnSet3.add(UrnType.ANY_TYPE);
			requestedUrnSet3.add(UrnType.SHA1);
			Set[] requestedUrnSets = {requestedUrnSet0, requestedUrnSet1, 
									  requestedUrnSet2, requestedUrnSet3};
			Set queryUrnSet = new HashSet();
			queryUrnSet.add(urn);
			for(int j=0; j<requestedUrnSets.length; j++) {
				QueryRequest qr = new QueryRequest(GUID.makeGuid(), (byte)6, 0, "", "", false, 
												   requestedUrnSets[j], queryUrnSet);
				Response[] responses = fm.query(qr);
				Response testResponse = new Response(fd);
				assertEquals("there should only be one response", 1, responses.length);
				assertEquals("responses should be equal", testResponse, responses[0]);
			}
		}
	}
}
