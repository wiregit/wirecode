package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.*;
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

	private Set _urnSet;
	private Set _alternateLocations;

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
		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
			_alternateLocations.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[i]);
		}


		Iterator iter = _alternateLocations.iterator();
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			if(_alCollection == null) {
				_alCollection = 
					AlternateLocationCollection.createCollection(al.getSHA1Urn());
			}
			_alCollection.addAlternateLocation(al);
		}
	}

	/**
	 * Tests that adding an <tt>AlternateLocationCollection</tt> works correctly.
	 */
	public void testCreateCollectionFromHttpValue() {
		AlternateLocationCollection collection = 
			AlternateLocationCollection.createCollection
			(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0].getSHA1Urn());
		AlternateLocationCollection testCollection = collection;
		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
			collection.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[i]);
		}

		testCollection.addAlternateLocationCollection(collection);
	}

	/**
	 * Tests to make sure that unequal SHA1s cannot be added to an 
	 * <tt>AlternateLocationCollection</tt>.
	 */
	public void testAddWrongLocation() {
		AlternateLocationCollection collection = 
			AlternateLocationCollection.createCollection
			(HugeTestUtils.UNIQUE_SHA1);
		for(int i=0; i<HugeTestUtils.UNEQUAL_SHA1_LOCATIONS.length; i++) {
			try {
				collection.addAlternateLocation(HugeTestUtils.UNEQUAL_SHA1_LOCATIONS[i]);
				fail("should not have accepted unequal location: "+
					 HugeTestUtils.UNEQUAL_SHA1_LOCATIONS[i]);
			} catch(IllegalArgumentException e) {
				// this is the expected behavior
			}
		}
	}


	/**
	 * Tests the method for adding alternate locations to this alternate 
	 * location collection.
	 */
	public void testAddAlternateLocation() {
		AlternateLocationCollection alc = 
			AlternateLocationCollection.createCollection(_alCollection.getSHA1Urn());
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
		AlternateLocationCollection alc1 = 
			AlternateLocationCollection.createCollection(_alCollection.getSHA1Urn());
		Iterator iter = _alternateLocations.iterator();
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			alc1.addAlternateLocation(al);
		}

		AlternateLocationCollection alc2 = 
			AlternateLocationCollection.createCollection(_alCollection.getSHA1Urn());
		alc2.addAlternateLocationCollection(alc1);
	}	

	/**
	 * Tests the method for checking whether or not the specified alternate
	 * location collection has alternate locations stored.
	 */
	public void testHasAlternateLocations() {
	   assertTrue("should have alternate locations", 
				  _alCollection.hasAlternateLocations());
	   AlternateLocationCollection testCollection = 
		   AlternateLocationCollection.createCollection(_alCollection.getSHA1Urn());
	   assertTrue("should not have alternate locations", 
				  !testCollection.hasAlternateLocations());
	}

	/**
	 * Tests the method for getting the HTTP string value for the alternate
	 * location collection.
	 */
	public void testHTTPStringValue() {
		String val = _alCollection.httpStringValue();
		StringTokenizer st = new StringTokenizer(val, ",");
		AlternateLocationCollection alc1 = 
			AlternateLocationCollection.createCollection(_alCollection.getSHA1Urn());
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
