package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;

/**
 * Test the public methods of the <tt>FileDesc</tt> class.
 */
public final class FileDescTester extends TestCase {

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
		"urn:sha1:PLSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:bitprint:XLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567",
		"urn:Bitprint:RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567"
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
		"urn:sha1:ELSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:bitprint:XLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		             "RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567",
		"urn:Bitprint:TLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		             "YLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567"
	};

	private Set _containedUrnSet;
	private Set _uncontainedUrnSet;

	private File[] _fileArray;
	private FileDesc[] _fileDescArray;

	public FileDescTester(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(FileDescTester.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		_containedUrnSet = new HashSet();
		for(int i=0; i<10; i++) {
			try {
				_containedUrnSet.add(URNFactory.createUrn(containedURNStrings[i]));
			} catch(IOException e) {
				assertTrue("unexpected exception "+e+"\r\n"+
						   containedURNStrings[i], false);
			}
		}

		_uncontainedUrnSet = new HashSet();
		for(int i=0; i<10; i++) {
			try {
				_uncontainedUrnSet.add(URNFactory.createUrn(uncontainedURNStrings[i]));
			} catch(IOException e) {
				assertTrue("unexpected exception "+e+"\r\n"+
						   uncontainedURNStrings[i], false);
			}
		}

		File curDir = new File(CommonUtils.getCurrentDirectory());
		File parDir = curDir.getParentFile();
		_fileArray = parDir.listFiles();
		_fileDescArray = new FileDesc[_containedUrnSet.size()];
		for(int i=0; i<_containedUrnSet.size(); i++) {
			_fileDescArray[i] = new FileDesc(_fileArray[i], i, _containedUrnSet);
		}
	}

	/**
	 * Tests the FileDesc construcotor.
	 */
	public void testConstructor() {
		File file = new File("FileDescTester.java");
		try {
			FileDesc fd = new FileDesc(file, 0, null);
			assertTrue("null values should not be permitted for FileDesc "+
					   "constructor", false);
		} catch(NullPointerException e) {
		}

		try {
			FileDesc fd = new FileDesc(null, 0, new HashSet());
			assertTrue("null values should not be permitted for FileDesc "+
					   "constructor", false);
		} catch(NullPointerException e) {
		}		
	}


	/**
	 * Tests the containsUrn method that returns whether or not the 
	 * <tt>FileDesc</tt> contains the specified URN.
	 */
	public void testContainsUrn() {
		for(int i=0; i<_fileDescArray.length; i++) {
			Iterator iter0 = _containedUrnSet.iterator();
			while(iter0.hasNext()) {
				URN urn = (URN)iter0.next();
				assertTrue("The FileDesc should contain the URN: "+urn,
						   _fileDescArray[i].containsUrn(urn));
			}
			
			Iterator iter1 = _uncontainedUrnSet.iterator();
			while(iter1.hasNext()) {
				URN urn = (URN)iter1.next();
				assertTrue("The FileDesc should not contain the URN: "+urn,
						   !_fileDescArray[i].containsUrn(urn));
			}
		}
	}

	/**
	 * Tests the method for checking whether or not the file desc has a 
	 * SHA1 urn.
	 */
	public void testHasSHA1Urn() {
		for(int i=0; i<_fileDescArray.length; i++) {
			assertTrue("this should contain a URN", _fileDescArray[i].hasSHA1Urn());
		}
	}

	/**
	 * Tests the method for getting the SHA1 URN from the FileDesc.
	 */
	public void testGetSHA1Urn() {
		URN firstUrn = null;
		for(int i=0; i<_fileDescArray.length; i++) {
			URN urn = _fileDescArray[i].getSHA1Urn();
			if(firstUrn == null) firstUrn = urn;			
			assertTrue("These urns should be equal", firstUrn.equals(urn));
		}		
	}
}
