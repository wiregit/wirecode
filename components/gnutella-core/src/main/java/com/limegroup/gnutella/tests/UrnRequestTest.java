package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;

/**
 * This class tests Gnutella requests involving URNs (as specified in HUGE v094)
 * to make sure that they bahave as expected.
 */
public final class UrnRequestTest extends TestCase {

	/**
	 * FileManager instance to test against.
	 */
	private FileManager _fileManager;

	/**
	 * Test shared directory.
	 */
	private File _testDir;

	/**
	 * Constructs a new test instance.
	 */
	public UrnRequestTest(String name) {
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
		_fileManager = new MetaFileManager();		
		_fileManager.initialize(new ActivityCallbackStub());	
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
	 * Tests URN requests on the FileManager.
	 */
	public void testUrnRequests() {
		for(int i=0; i<_fileManager.getNumFiles(); i++) {
			FileDesc fd = _fileManager.get(i);
			Response testResponse = new Response(fd);
			URN urn = fd.getSHA1Urn();
			assertEquals("File indexes should match", i, 
						 _fileManager.getFileIndexForUrn(urn));
			assertEquals("FileDescs should match", fd, 
						 _fileManager.getFileDescForUrn(urn));
			
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
				QueryRequest qr = new QueryRequest(GUID.makeGuid(), (byte)6, 0, "", "", 
												   false, requestedUrnSets[j], 
												   queryUrnSet);
				Response[] responses = _fileManager.query(qr);
				assertEquals("there should only be one response", 1, responses.length);
				assertEquals("responses should be equal", testResponse, responses[0]);		
			}
		}
	}

	/**
	 * Tests sending request that do not explicitly request any URNs -- traditional
	 * requests -- to make sure that they do return URNs in their responses.
	 */
	public void testThatUrnsAreReturnedWhenNotRequested() {
		for(int i=0; i<_fileManager.getNumFiles(); i++) {
			FileDesc fd = _fileManager.get(i);
			Response testResponse = new Response(fd);
			URN urn = fd.getSHA1Urn();
			QueryRequest qr = new QueryRequest(GUID.makeGuid(), (byte)6, 0, 
											   fd.getName(), "", false, null, null);
			Response[] responses = _fileManager.query(qr);	
			assertEquals("responses should be equal", testResponse, responses[0]);
			Set urnSet = responses[0].getUrns();
			URN[] responseUrns = (URN[])urnSet.toArray(new URN[0]);
			// this is just a sanity check
			assertEquals("urns should be equal", urn, responseUrns[0]);		
		}
	}
}
