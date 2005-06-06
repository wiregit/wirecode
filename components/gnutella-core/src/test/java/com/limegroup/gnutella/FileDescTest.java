package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Test the public methods of the <tt>FileDesc</tt> class.
 */
public final class FileDescTest extends com.limegroup.gnutella.util.BaseTestCase {


	private final String[] uncontainedURNStrings = {
		"urn:sha1:CLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:XLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:FLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:DLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:SLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:ALSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:QLSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:WLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:ELSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	};

	private Set _uncontainedUrnSet;

	private File[] _fileArray;
	private FileDesc[] _fileDescArray;

	public FileDescTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(FileDescTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() throws Exception {		
		_uncontainedUrnSet = new HashSet();
		for(int i=0; i<10; i++) {
			try {
				_uncontainedUrnSet.add(URN.createSHA1Urn(uncontainedURNStrings[i]));
			} catch(IOException e) {
				assertTrue("unexpected exception "+e+"\r\n"+
						   uncontainedURNStrings[i], false);
			}
		}

		File curDir = CommonUtils.getCurrentDirectory();
		File parDir = curDir.getParentFile();
		File[] allFiles = parDir.listFiles();
		List fileList = new LinkedList();
		for(int i=0; i<allFiles.length; i++) {
			if(allFiles[i].isFile()) {
				fileList.add(allFiles[i]);
			}
		}
		_fileArray = (File[])fileList.toArray(new File[0]);
		_fileDescArray = new FileDesc[_fileArray.length];
		for(int i=0; i<_fileArray.length; i++) {
			Set urns = FileDesc.calculateAndCacheURN(_fileArray[i]);
			_fileDescArray[i] = new FileDesc(_fileArray[i], urns, i);
		}
	}

	/**
	 * Tests the FileDesc construcotor.
	 */
	public void testConstructor() throws Exception {
		File file = CommonUtils.getResourceFile("build.xml");

		try {
			Set urns = FileDesc.calculateAndCacheURN(file);
			new FileDesc(null, urns, 0);
			fail("null values should not be permitted for FileDesc "+
				 "constructor");
		} catch(NullPointerException e) {
		}		
	}

	/**
	 * Tests the calculateAndCacheURN method
	 */
	public void testCalculateAndCacheURN() throws Exception {
		File file = new File(getSaveDirectory(), "nonexistentfile");
		if(file.exists()) file.delete();
		try {
			FileDesc.calculateAndCacheURN(file);
			fail("nonexistent files should not be permitted");
		} catch(IOException e) {
		}		

		try {
			FileDesc.calculateAndCacheURN(null);
			fail("null files should not be permitted");
		} catch(NullPointerException e) {
		}		
	}

	/**
	 * Tests the containsUrn method that returns whether or not the 
	 * <tt>FileDesc</tt> contains the specified URN.
	 */
//  	public void testContainsUrn() {
//  		for(int i=0; i<_fileDescArray.length; i++) {
//  			Iterator iter0 = _containedUrnSet.iterator();
//  			while(iter0.hasNext()) {
//  				URN urn = (URN)iter0.next();
//  				assertTrue("The FileDesc should contain the URN: "+urn,
//  						   _fileDescArray[i].containsUrn(urn));
//  			}
			
//  			Iterator iter1 = _uncontainedUrnSet.iterator();
//  			while(iter1.hasNext()) {
//  				URN urn = (URN)iter1.next();
//  				assertTrue("The FileDesc should not contain the URN: "+urn,
//  						   !_fileDescArray[i].containsUrn(urn));
//  			}
//  		}
//  	}


	/**
	 * Tests the method for getting the SHA1 URN from the FileDesc.
	 */
//  	public void testGetSHA1Urn() {
//  		URN firstUrn = null;
//  		for(int i=0; i<_fileDescArray.length; i++) {
//  			URN urn = _fileDescArray[i].getSHA1Urn();
//  			if(firstUrn == null) firstUrn = urn;			
//  			assertTrue("These urns should be equal", firstUrn.equals(urn));
//  		}		
//  	}
}
