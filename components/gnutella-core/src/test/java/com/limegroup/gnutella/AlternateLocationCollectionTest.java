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
					AlternateLocationCollection.createCollection(al.getSHA1Urn());
                created = true;
			}
			_alCollection.addAlternateLocation(al);
		}
	}

    /**
     * Test to make sure the wasRemoved method is working as expected.
     */
    public void testWasRemoved() throws Exception {
        AlternateLocation al = HugeTestUtils.EQUAL_SHA1_LOCATIONS[0];
        URN sha1 = al.getSHA1Urn();
        AlternateLocationCollection alc = 
            AlternateLocationCollection.createCollection(sha1);
        alc.addAlternateLocation(al);
        alc.removeAlternateLocation(al);
        assertTrue("should have been removed", alc.wasRemoved(al));
    }

    /**
     * Test the method for getting a diff of the alternate location
     * collection.
     */
    public void testDiffAlternateLocationCollection() throws Exception {

        // make sure the general case works
        AlternateLocation al = HugeTestUtils.EQUAL_SHA1_LOCATIONS[0];
        URN sha1 = al.getSHA1Urn();
        AlternateLocationCollection alc1 = 
            AlternateLocationCollection.createCollection(sha1);

        alc1.addAlternateLocation(al);

        // create a second one to pass into the diff method
        AlternateLocationCollection alc2 = 
            AlternateLocationCollection.createCollection(sha1);
        alc2.addAlternateLocation(al);
        alc2.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[1]);
        alc2.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[2]);
        alc2.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[3]);

        // create a third one that should be equal to the diffed collection
        // after the method is invoked
        AlternateLocationCollection alc3 = 
            AlternateLocationCollection.createCollection(sha1);
        alc3.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[1]);
        alc3.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[2]);
        alc3.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[3]);

        
        AlternateLocationCollection alcTest = 
            alc1.diffAlternateLocationCollection(alc2);
        assertEquals("diffed collection should be equal", alc3, alcTest);

        
        // now, make sure the diff works when some of the locations 
        // have already been removed.
        alc1.removeAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0]);
        
        // if the above remove hadn't worked, or if the diff method 
        // didn't work, this would return alc2
        alcTest = alc1.diffAlternateLocationCollection(alc2);
        assertEquals("diffed collection should be equal", alc3, alcTest);                
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
		int i = 0;
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
			alc.addAlternateLocation(al);
			assertEquals("was not able to add as new",
			    ++i, alc.getNumberOfAlternateLocations() );
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
		assertEquals("was not able to add all from the collection.",
		    alc1.getNumberOfAlternateLocations(),
		    alc2.getNumberOfAlternateLocations());
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
	public void testHTTPStringValue() throws Exception {
		String val = _alCollection.httpStringValue();
		StringTokenizer st = new StringTokenizer(val, ",");
		AlternateLocationCollection alc1 = 
			AlternateLocationCollection.createCollection(_alCollection.getSHA1Urn());
		while(st.hasMoreTokens()) {
			String str = st.nextToken();
			str = str.trim();
			AlternateLocation al = 
			    AlternateLocation.createAlternateLocation(str);
			alc1.addAlternateLocation(al);
		}

		//assertTrue("AlternateLocationCollections should be equal:\r\n"+_alCollection+
        //         "\r\n"+alc1, 
        //         _alCollection.equals(alc1));

		//AlternateLocationCollection alc2 = new AlternateLocationCollection();
		//alc2.addAlternateLocationCollection(alc1);
		//assertTrue("AlternateLocationCollections should be equal", 
		//	   _alCollection.equals(alc2));
		
	}
	
	/**
	 * Tests that locations are succesfully removed.
	 */
	public void testCanRemoveLocation() {
	    Iterator iter = _alternateLocations.iterator();
	    int total = _alCollection.getNumberOfAlternateLocations();
	    int i = 0;
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
                i++;
			    assertTrue("unable to remove al: " + al + " from collection: " + _alCollection,
			        _alCollection.removeAlternateLocation(al));
                assertEquals("size is off", 
                    total-i, _alCollection.getNumberOfAlternateLocations() );
		}
    }
    
    /**
     * Tests that locations cannot be readded after being removed.
     */
    public void testCantAddAfterRemove() {
	    Iterator iter = _alternateLocations.iterator();
	    int total = _alCollection.getNumberOfAlternateLocations();
	    int i = 0;
		for(AlternateLocation al = (AlternateLocation)iter.next(); 
			iter.hasNext();   al = (AlternateLocation)iter.next()) {
                i++;
			    assertTrue("unable to remove al: " + al,
			        _alCollection.removeAlternateLocation(al));
                assertEquals("size is off", 
                    total-i, _alCollection.getNumberOfAlternateLocations() );
                _alCollection.addAlternateLocation(al);
                assertEquals("size is off", 
                    total-i, _alCollection.getNumberOfAlternateLocations() );                
		}
    }        
}




