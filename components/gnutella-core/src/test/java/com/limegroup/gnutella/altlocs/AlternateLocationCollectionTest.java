package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;


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
            try {
                _alternateLocations.add(
                        AlternateLocation.create(HugeTestUtils.EQUAL_URLS[i]));
            } catch (IOException e) {
                fail("could not set up test");
            }
		}


        boolean created = false;
		Iterator iter = _alternateLocations.iterator();
		for(; iter.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)iter.next();
			if(!created) {
				_alCollection = 
					AlternateLocationCollection.create(al.getSHA1Urn());
                created = true;
			}            
			_alCollection.add(al);
		}
        assertTrue("failed to set test up",_alCollection.getAltLocsSize()==_alternateLocations.size());
	}

	/**
	 * Tests that adding an <tt>AlternateLocationCollection</tt> works correctly.
	 */
    //Sumeet:TODO: This test does not test anything. We should at some point
    //write a common method for parsing a collection out of a HTTP header, and
    //then add this test and make it sensible
//  	public void testCreateCollectionFromHttpValue() {
//  		AlternateLocationCollection collection = 
//  			AlternateLocationCollection.create
//  			(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0].getSHA1Urn());
//  		AlternateLocationCollection testCollection = collection;
//  		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
//  			collection.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[i]);
//  		}

//  		testCollection.addAll(collection);
//  	}

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
			    AlternateLocation.create(str, _alCollection.getSHA1Urn());
			alc1.add(al);
		}
	}
	
	/**
	 * Tests that locations are succesfully removed only after it is demoted
	 */
	public void testCanRemoveLocation() {
	    Iterator iter = _alternateLocations.iterator();
	    int total = _alCollection.getAltLocsSize();
	    int i = 0;
		for( ; iter.hasNext() ; ) {
            AlternateLocation al=(AlternateLocation)iter.next();
            i++;
            assertFalse("location already demoted "+al,al.getDemoted());
            boolean removed = _alCollection.remove(al);//demoted
            assertFalse("location removed without demoting ",removed);
            //make sure it's still there
            assertTrue("demoted location removed "+
                                            al,_alCollection.contains(al));
            removed = _alCollection.remove(al);//now it should be removed
            assertTrue("unable to remove al: " + al + " from collection: " 
                       + _alCollection, removed);
            assertEquals("size is off",total-i,_alCollection.getAltLocsSize() );
		}
    }
    
    /**
     * Tests that locations cannot be readded after being removed.
     * TODO: check repromotion
     */
    public void testLocationPromotionDemotionWithAddRemove() {
	    Iterator iter = _alternateLocations.iterator();
	    int total = _alCollection.getAltLocsSize();
	    int i = 0;
		for( ; iter.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)iter.next(); 
            i++;
            assertFalse("location already demoted "+al,al.getDemoted());
            boolean removed = _alCollection.remove(al);//demoted
            assertFalse("location removed without demoting ",removed);
            //make sure it's still there
            assertTrue("demoted location removed "+
                                            al,_alCollection.contains(al));
            //add it back
            _alCollection.add(al);
            assertFalse("location not promoted after add  "+al,al.getDemoted());
            removed = _alCollection.remove(al);//demoted again
            assertFalse("location removed without demoting ",removed);
            //make sure it's still there
            assertTrue("demoted location removed "+
                                            al,_alCollection.contains(al));
            removed = _alCollection.remove(al);//now it should be removed
            assertTrue("unable to remove al: " + al + " from collection: " 
                       + _alCollection, removed);
            assertEquals("size is off",total-i,_alCollection.getAltLocsSize() );
 		}
    }

    public void testClonedSharedLocsWork() {
        AlternateLocationCollection c1=
        AlternateLocationCollection.create(_alCollection.getSHA1Urn());
        AlternateLocationCollection c2=
        AlternateLocationCollection.create(_alCollection.getSHA1Urn());
        
        AlternateLocation[] alts = new AlternateLocation[5];
        
        for(int i=0; i<5; i++) {
            AlternateLocation al = null;
            try {
                al= AlternateLocation.create(HugeTestUtils.EQUAL_URLS[i]);
            } catch (MalformedURLException e) {
                fail("unable to set up test");
            } catch (IOException e) {
                fail("unable to set up test");
            }
            alts[i] = al;
            c1.add(al);
        }
        
        try {
            c1.add(AlternateLocation.create(HugeTestUtils.UNEQUAL_URLS[2]));
            fail("exception should have been thrown by now");
        } catch(Exception e) {
            //expected behaviour
        }        
        
        //add the same elements as clones to c2
        for(int i=0; i<5; i++)
            c2.add(alts[i].createClone());

        assertEquals(c1,c2);
        
        for(int i=0; i<5; i++)
            c1.add(alts[i].createClone());
        
        assertNotEquals(c1,c2);//alt locs should have changed.
        
    }

    public void testCollectionOrder() {
        AlternateLocation shouldFirst;
        AlternateLocation shouldSecond;
        AlternateLocation shouldThird;
        AlternateLocation shouldLast;
        int totalAlts = _alternateLocations.size();
        Iterator iter = _alternateLocations.iterator();
        for(int i=0; i<totalAlts-4; i++) {
            AlternateLocation loc = (AlternateLocation)iter.next();
            iter.remove();
            _alCollection.remove(loc);//demote
            _alCollection.remove(loc);//remove
        }
        assertEquals("Coll of incorrect size",4,_alCollection.getAltLocsSize());
        iter =  _alternateLocations.iterator();
        shouldFirst = (AlternateLocation)iter.next();
        shouldSecond = (AlternateLocation)iter.next();
        shouldThird = (AlternateLocation)iter.next();
        shouldLast = (AlternateLocation)iter.next();
        
        _alCollection.add(shouldFirst);//count == 2, no demotion

        _alCollection.add(shouldSecond);
        _alCollection.add(shouldSecond);//count == 3 for second, no demotion

        _alCollection.add(shouldThird);//count == 2
        _alCollection.remove(shouldThird);//count == 2, and demoteddemote

        _alCollection.add(shouldLast);//
        _alCollection.add(shouldLast);//count == 3
        _alCollection.remove(shouldLast);//count == 2, and demoteddemote

        synchronized(_alCollection) {
            Iterator i = _alCollection.iterator();
            AlternateLocation loc = (AlternateLocation)i.next();
            assertEquals("iterator method not sorted ",loc,shouldFirst);
            loc = (AlternateLocation)i.next();
            assertEquals("iterator method not sorted",loc,shouldSecond);
            loc = (AlternateLocation)i.next();
            assertEquals("iterator method not sorted",loc,shouldThird);
            loc = (AlternateLocation)i.next();
            assertEquals("iterator method not sorted",loc, shouldLast);
            assertFalse(i.hasNext());
        }
        
        
        FixedSizeSortedSet set = null;
        try {
            set = (FixedSizeSortedSet)
            PrivilegedAccessor.getValue(_alCollection,"LOCATIONS");
        } catch (NoSuchFieldException e) {
            fail("could not access test field");
        } catch (IllegalAccessException e) {
            fail("could not access test field");
        }
        assertEquals(set.first(),shouldFirst);
        assertEquals(set.last(), shouldLast);
        
        _alCollection.remove(shouldLast);//it's out
        _alCollection.remove(shouldFirst);//demoted
        _alCollection.remove(shouldFirst);//remove
        
        try {
            set = (FixedSizeSortedSet)
            PrivilegedAccessor.getValue(_alCollection,"LOCATIONS");
        } catch (NoSuchFieldException e) {
            fail("could not access test field");
        } catch (IllegalAccessException e) {
            fail("could not access test field");
        }
        
        assertEquals(set.first(), shouldSecond);
        assertEquals(set.last(), shouldThird);
    }
    
    public void testToBytes() throws Exception {
    	byte [] altlocs =_alCollection.toBytes(3);
    	assertEquals(3*6,altlocs.length);
    	altlocs = _alCollection.toBytes(0);
    	assertNull(altlocs);
    	altlocs = _alCollection.toBytes(-3);
    	assertNull(altlocs);
    	altlocs =_alCollection.toBytes();
    	assertEquals(_alternateLocations.size()*6,altlocs.length);
    	
    	Collection parsed =NetworkUtils.unpackIps(altlocs);
    	
		assertEquals(_alternateLocations.size(),parsed.size());
		Iterator parsedIter = parsed.iterator();
		
    	for(Iterator iter = _alCollection.iterator();iter.hasNext();) {
    		AlternateLocation current = (AlternateLocation)iter.next();
    		QueryReply.IPPortCombo address = (QueryReply.IPPortCombo) parsedIter.next();
    		
    		assertEquals(current.getHost().getInetAddress(),address.getInetAddress());
    		assertEquals(current.getHost().getPort(),address.getPort());
    		
    	}
    	
    }
}




