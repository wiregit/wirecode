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
	 * Tests that alternate locations with the same SHA1 values can be
	 * added to a <tt>FileDesc</tt>.
	 */
	public void testAddingAlternateLocations() throws Exception{
		File file = CommonUtils.getResourceFile("build.xml");
		Set urns = FileDesc.calculateAndCacheURN(file);
		FileDesc fd = new FileDesc(file, urns, 0);
		URN sha1 = fd.getSHA1Urn();
		URL sha1Url = new URL("http", "60.23.35.10", 6346, 
							  "/uri-res/N2R?"+sha1.httpStringValue());
		AlternateLocation loc =  
			HugeTestUtils.create(sha1Url);
		fd.add(loc);
	}

	/**
	 * Tests that alternate locations with the different SHA1 values cannot be
	 * added to a <tt>FileDesc</tt>.
	 */
	public void testAddingDifferentAlternateLocations() throws Exception {
		File file = CommonUtils.getResourceFile("build.xml");
		try {
			Set urns = FileDesc.calculateAndCacheURN(file);
			FileDesc fd = new FileDesc(file, urns, 0);
			URN sha1 = fd.getSHA1Urn();
  			URL sha1Url = new URL("http", "60.23.35.10", 6346, 
								  HTTPConstants.URI_RES_N2R+
								  HugeTestUtils.URNS[0]);
			AlternateLocation loc =  
				HugeTestUtils.create(sha1Url);
			assertNotNull("should not be null", loc.getSHA1Urn());
			fd.add(loc);
			fail("should not have accepted location: "+loc+"when our sha1 is: "+sha1);
		} catch(IllegalArgumentException e) {
		} catch(IOException e) {
        }
	}
	
	/**
	 * tests whether FileDesc properly stores direct and push alternate locations.
	 */
	public void testAltLocSeparation () throws Exception {
	    SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
		File file = CommonUtils.getResourceFile("build.xml");

		Set urns = FileDesc.calculateAndCacheURN(file);
		FileDesc fd = new FileDesc(file, urns, 0);
		URN sha1 = fd.getSHA1Urn();
		GUID clientGUID1 = new GUID(GUID.makeGuid());
		GUID clientGUID2 = new GUID(GUID.makeGuid());

		//create some direct altlocs
		AlternateLocation d1 = AlternateLocation.create("1.1.1.1:1",sha1);
		AlternateLocation d2 = AlternateLocation.create("2.2.2.2:2",sha1);
		
		//and some push ones.
		AlternateLocation p1 = 
			AlternateLocation.create(clientGUID1.toHexString()+";1.1.1.1:1",sha1);
	    ((PushAltLoc)p1).updateProxies(true);
		AlternateLocation p2 = 
			AlternateLocation.create(clientGUID2.toHexString()+";2.2.2.2:2",sha1);
        ((PushAltLoc)p2).updateProxies(true);
		
		assertEquals(0,fd.getAltLocsSize());
		
		//add one of each type
		fd.add(d1);
		fd.add(p1);
		
		assertEquals(2,fd.getAltLocsSize());
		assertEquals(1,fd.getPushAlternateLocationCollection().getAltLocsSize());
		assertEquals(1,fd.getAlternateLocationCollection().getAltLocsSize());
		
		//add one, remove one
		fd.add(d2);
		fd.remove(p1);fd.remove(p1);  //this will not remove the altloc - it still has proxies
		
		
		assertEquals(3,fd.getAltLocsSize());
		assertEquals(1,fd.getPushAlternateLocationCollection().getAltLocsSize());
		assertEquals(2,fd.getAlternateLocationCollection().getAltLocsSize());
		
		// remove all the proxies for one of the altlocs
		PushEndpoint.overwriteProxies(clientGUID1.bytes(),Collections.EMPTY_SET);
		fd.remove(p1);
		
		assertEquals(2,fd.getAltLocsSize());
		assertEquals(0,fd.getPushAlternateLocationCollection().getAltLocsSize());
		assertEquals(2,fd.getAlternateLocationCollection().getAltLocsSize());
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
