package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

/**
 * Test the public methods of the <tt>FileDesc</tt> class.
 */
public final class AlternateLocationCollectionTest extends TestCase {

	private static final String[] validTimestampedLocs = {
		"Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"X-Gnutella-Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"http: //Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z"
	};

	private final String [] urnStrings = {
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
	};

	private final String[] hostNameStrings = {
		"www.limewire.com",
		"www.limewire.org",
		"www.cnn.com",
		"www.download.com",
		"www.news.com",
		"www.help.com",
		"www.columbia.edu",
		"www.test.com",
		"www.now.com",
		"www.urn.com",
		"jakarta.apache.org",
		"www.junit.org",
		"www.xerces.com",
		"www.slahdot.org",
		"www.media.com",
		"www.espn.com",
		"www.rr.com",
		"www.light.com"
	};

	private Set _urnSet;
	private Set _alternateLocations;

	private File[] _fileArray;
	private FileDesc[] _fileDescArray;

	private AlternateLocationCollection _alCollection;

	public AlternateLocationCollectionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(AlternateLocationCollectionTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		_alternateLocations = new HashSet();
		for(int i=0; i<hostNameStrings.length; i++) {
			try {
				URL url = new URL("http", hostNameStrings[i], 6346, "/test.htm");
				AlternateLocation al = 
				    AlternateLocation.createAlternateLocation(url);
				_alternateLocations.add(al);
			} catch(IOException e) {
				assertTrue("unexpected exception: "+e, false);
			}
		}

		File curDir = CommonUtils.getCurrentDirectory();
		File parDir = curDir.getParentFile();
		_fileArray = parDir.listFiles();
		_fileDescArray = new FileDesc[_fileArray.length];
		for(int i=0; i<_fileArray.length; i++) {
			_fileDescArray[i] = new FileDesc(_fileArray[i], 
											 FileDesc.calculateAndCacheURN(_fileArray[i]),
											 i);
		}

		_alCollection = new AlternateLocationCollection();
		Iterator iter = _alternateLocations.iterator();
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			_alCollection.addAlternateLocation(al);
		}
	}

	/**
	 * Tests the method for adding alternate locations to this alternate 
	 * location collection.
	 */
	public void testAddAlternateLocation() {
		AlternateLocationCollection alc = new AlternateLocationCollection();
		Iterator iter = _alternateLocations.iterator();
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			alc.addAlternateLocation(al);
		}
	}	

	/**
	 * Tests the method for adding a collections of alternate locations to
	 * this AlternateLocationCollection.
	 */
	public void testAddAlternateLocationCollection() {
		AlternateLocationCollection alc1 = new AlternateLocationCollection();
		Iterator iter = _alternateLocations.iterator();
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			alc1.addAlternateLocation(al);
		}

		AlternateLocationCollection alc2 = new AlternateLocationCollection();
		alc2.addAlternateLocationCollection(alc1);
	}	

	/**
	 * Tests the method for checking whether or not the specified alternate
	 * location collection has alternate locations stored.
	 */
	public void testHasAlternateLocations() {
	   assertTrue("should have alternate locations", 
				  _alCollection.hasAlternateLocations());
	   assertTrue("should not have alternate locations", 
				  !new AlternateLocationCollection().hasAlternateLocations());
	}

	/**
	 * Tests the method for getting the HTTP string value for the alternate
	 * location collection.
	 */
	public void testHTTPStringValue() {
		String val = _alCollection.httpStringValue();
		StringTokenizer st = new StringTokenizer(val, ",");
		AlternateLocationCollection alc1 = new AlternateLocationCollection();
		while(st.hasMoreTokens()) {
			String str = st.nextToken();
			str = str.trim();
			try {
				AlternateLocation al = 
				    AlternateLocation.createAlternateLocation(str);
				alc1.addAlternateLocation(al);
			} catch(IOException e) {
				assertTrue("Exception in AlternateLocation contruction: "+e, false);
			}
		}

		//assertTrue("AlternateLocationCollections should be equal:\r\n"+_alCollection+
		//	   "\r\n"+alc1, 
		//	   _alCollection.equals(alc1));

		//AlternateLocationCollection alc2 = new AlternateLocationCollection();
		//alc2.addAlternateLocationCollection(alc1);
		//assertTrue("AlternateLocationCollections should be equal", 
		//	   _alCollection.equals(alc2));
		
	}
}
