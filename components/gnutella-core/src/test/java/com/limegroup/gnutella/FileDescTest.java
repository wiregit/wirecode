package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.net.*;

/**
 * Test the public methods of the <tt>FileDesc</tt> class.
 */
public final class FileDescTest extends TestCase {

	private final String [] containedURNStrings = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:PLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:PLSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	};

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
		return new TestSuite(FileDescTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {		
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
	public void testConstructor() {
		File file = new File("build.xml");

		try {
			Set urns = FileDesc.calculateAndCacheURN(file);
			FileDesc fd = new FileDesc(null, urns, 0);
			fail("null values should not be permitted for FileDesc "+
				 "constructor");
		} catch(NullPointerException e) {
		}		
	}

	/**
	 * Tests the calculateAndCacheURN method
	 */
	public void testCalculateAndCacheURN() {
		File file = new File("nonexistentfile");
		if(file.exists()) file.delete();
		try {
			Set urns = FileDesc.calculateAndCacheURN(file);
			fail("nonexistent files should not be permitted");
		} catch(IllegalArgumentException e) {
		}		

		try {
			Set urns = FileDesc.calculateAndCacheURN(null);
			fail("null files should not be permitted");
		} catch(NullPointerException e) {
		}		
	}


	/**
	 * Tests that alternate locations with the same SHA1 values can be
	 * added to a <tt>FileDesc</tt>.
	 */
	public void testAddingAlternateLocations() {
		File file = new File("build.xml");
		try {
			Set urns = FileDesc.calculateAndCacheURN(file);
			FileDesc fd = new FileDesc(file, urns, 0);
			URN sha1 = fd.getSHA1Urn();
  			URL sha1Url = new URL("http", "60.23.35.10", 6346, 
								  "/uri-res/N2R?"+sha1.httpStringValue());
			AlternateLocation loc =  
				AlternateLocation.createAlternateLocation(sha1Url);
			fd.addAlternateLocation(loc);
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
			fail("unexpected exception: "+e);
		} catch(MalformedURLException e) {
			e.printStackTrace();
			fail("unexpected exception: "+e);
		}
	}

	/**
	 * Tests that alternate locations with the different SHA1 values cannot be
	 * added to a <tt>FileDesc</tt>.
	 */
	public void testAddingDifferentAlternateLocations() {
		File file = new File("build.xml");
		try {
			Set urns = FileDesc.calculateAndCacheURN(file);
			FileDesc fd = new FileDesc(file, urns, 0);
			URN sha1 = fd.getSHA1Urn();
  			URL sha1Url = new URL("http", "60.23.35.10", 6346, 
								  HTTPConstants.URI_RES_N2R+
								  HugeTestUtils.URNS[0]);
			AlternateLocation loc =  
				AlternateLocation.createAlternateLocation(sha1Url);
			assertNotNull("should not be null", loc.getSHA1Urn());
			fd.addAlternateLocation(loc);
			fail("should not have accepted location: "+loc+"when our sha1 is: "+sha1);
		} catch(IllegalArgumentException e) {
		} catch(MalformedURLException e) {
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
