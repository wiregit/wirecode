package com.limegroup.gnutella;

import com.limegroup.gnutella.altlocs.*;
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
public final class AlternateLocationCollectionTest extends BaseTestCase {

	private Set _urnSet;
	private Set _alternateLocations;

	private AlternateLocationCollection _alCollection;

	public AlternateLocationCollectionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(AlternateLocationCollectionTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		_alternateLocations = new HashSet();
		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
			_alternateLocations.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[i]);
		}


        boolean created = false;
		Iterator iter = _alternateLocations.iterator();
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			if(!created) {
				_alCollection = 
					AlternateLocationCollection.create(al.getSHA1Urn());
                created = true;
			}
			_alCollection.add(al);
		}
	}

    /**
     * Test to make sure the wasRemoved method is working as expected.
     */
    public void testWasRemoved() throws Exception {
        AlternateLocation al = HugeTestUtils.EQUAL_SHA1_LOCATIONS[0];
        URN sha1 = al.getSHA1Urn();
        AlternateLocationCollection a=AlternateLocationCollection.create(sha1);
        AlternateLocationCollection b=AlternateLocationCollection.create(sha1);
        AltLocCollectionsManager man = new AltLocCollectionsManager(a,b);
        man.addLocation(al);
        man.removeLocation(al,false);
        assertFalse("should not have been removed", man.wasRemoved(al));
        man.removeLocation(al,true);
        assertTrue("loc should have been removed", man.wasRemoved(al));
    }


	/**
	 * Tests that adding an <tt>AlternateLocationCollection</tt> works correctly.
	 */
	public void testCreateCollectionFromHttpValue() {
		AlternateLocationCollection collection = 
			AlternateLocationCollection.create
			(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0].getSHA1Urn());
		AlternateLocationCollection testCollection = collection;
		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
			collection.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[i]);
		}

		testCollection.addAll(collection);
	}

	/**
	 * Tests to make sure that unequal SHA1s cannot be added to an 
	 * <tt>AlternateLocationCollection</tt>.
	 */
	public void testAddWrongLocation() {
		AlternateLocationCollection collection = 
			AlternateLocationCollection.create
			(HugeTestUtils.UNIQUE_SHA1);
		for(int i=0; i<HugeTestUtils.UNEQUAL_SHA1_LOCATIONS.length; i++) {
			try {
				collection.add(HugeTestUtils.UNEQUAL_SHA1_LOCATIONS[i]);
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
			AlternateLocationCollection.create(_alCollection.getSHA1Urn());
		Iterator iter = _alternateLocations.iterator();
		int i = 0;
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			alc.add(al);
			assertEquals("was not able to add as new",
			    ++i, alc.getAltLocsSize() );
		}
	}	

	/**
	 * Tests the method for adding a collections of alternate locations to
	 * this AlternateLocationCollection.
	 */
	public void testAddAlternateLocationCollection() {
		AlternateLocationCollection alc1 = 
			AlternateLocationCollection.create(_alCollection.getSHA1Urn());
		Iterator iter = _alternateLocations.iterator();
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			alc1.add(al);
		}

		AlternateLocationCollection alc2 = 
			AlternateLocationCollection.create(_alCollection.getSHA1Urn());
		alc2.addAll(alc1);
		assertEquals("was not able to add all from the collection.",
		    alc1.getAltLocsSize(),
		    alc2.getAltLocsSize());
	}	

	/**
	 * Tests the method for checking whether or not the specified alternate
	 * location collection has alternate locations stored.
	 */
	public void testHasAlternateLocations() {
	   assertTrue("should have alternate locations", 
				  _alCollection.hasAlternateLocations());
	   AlternateLocationCollection testCollection = 
		   AlternateLocationCollection.create(_alCollection.getSHA1Urn());
	   assertTrue("should not have alternate locations", 
				  !testCollection.hasAlternateLocations());
	}

	/**
	 * Tests the method for getting the HTTP string value for the alternate
	 * location collection.
	 */
	public void testHTTPStringValue() throws Exception {
		String val = _alCollection.httpStringValue();
		StringTokenizer st = new StringTokenizer(val, ",");
		AlternateLocationCollection alc1 = 
			AlternateLocationCollection.create(_alCollection.getSHA1Urn());
		while(st.hasMoreTokens()) {
			String str = st.nextToken();
			str = str.trim();
			AlternateLocation al = 
			    AlternateLocation.create(str);
			alc1.add(al);
		}

		//assertTrue("AlternateLocationCollections should be equal:\r\n"+_alCollection+
        //         "\r\n"+alc1, 
        //         _alCollection.equals(alc1));

		//AlternateLocationCollection alc2 = new AlternateLocationCollection();
		//alc2.addAll(alc1);
		//assertTrue("AlternateLocationCollections should be equal", 
		//	   _alCollection.equals(alc2));
		
	}
	
	/**
	 * Tests that locations are succesfully removed.
	 */
	public void testCanRemoveLocation() {
	    Iterator iter = _alternateLocations.iterator();
	    int total = _alCollection.getAltLocsSize();
	    int i = 0;
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
                i++;
			    assertTrue("unable to remove al: " + al + " from collection: " + _alCollection,
			        _alCollection.remove(al));
                assertEquals("size is off", 
                    total-i, _alCollection.getAltLocsSize() );
		}
    }
    
    /**
     * Tests that locations cannot be readded after being removed.
     */
    public void testCantAddAfterRemove() {
	    Iterator iter = _alternateLocations.iterator();
	    int total = _alCollection.getAltLocsSize();
	    int i = 0;
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
                i++;
			    assertTrue("unable to remove al: " + al,
			        _alCollection.remove(al));
                assertEquals("size is off", 
                    total-i, _alCollection.getAltLocsSize() );
                _alCollection.add(al);
                assertEquals("size is off", 
                    total-i, _alCollection.getAltLocsSize() );                
		}
    }        
}




