
package com.limegroup.gnutella.altlocs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.IOUtils;

/**
 * Tests the functionality of the altloc digest.
 */
public class AltLocDigestTest extends BaseTestCase {
    
    public AltLocDigestTest(String name) {
        super(name);
    }

	public static Test suite() {
		return buildTestSuite(AltLocDigestTest.class);
	}
	
	
	static AlternateLocationCollection _direct,_push; 
	
	static Set _alternateLocations;
	static PushAltLoc pa,pa2;
	
	protected void setUp() throws Exception{
	    _alternateLocations = new HashSet();
        
		for(int i=0; i<HugeTestUtils.EQUAL_SHA1_LOCATIONS.length; i++) {
            try {
                _alternateLocations.add(
                        HugeTestUtils.create(HugeTestUtils.EQUAL_URLS[i]));
            } catch (IOException e) {
                fail("could not set up test");
            }
		}


        boolean created = false;
		Iterator iter = _alternateLocations.iterator();
		for(; iter.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)iter.next();
			if(!created) {
				_direct = 
					AlternateLocationCollection.create(al.getSHA1Urn());
                created = true;
			}            
			_direct.add(al);
		}
		
		GUID g = new GUID(GUID.makeGuid());
		_push = AlternateLocationCollection.create(FileDescStub.DEFAULT_SHA1);
        PushEndpoint pe = new PushEndpoint(g.toHexString()+";1:2.2.2.2;1.1.1.1:2");
        PushEndpoint pe2 = new PushEndpoint(g.toHexString()+";2:3.3.3.3;2.2.2.2:3");
        pa = new PushAltLoc(pe,FileDescStub.DEFAULT_SHA1);
        pa2 = new PushAltLoc(pe2,FileDescStub.DEFAULT_SHA1);
        _push.add(pa);
        _push.add(pa2);
	}
	
	/**
	 * tests serialization and reading of the digest
	 */
	public void testReadAndWrite() throws Exception {
	    AltLocDigest digest = _direct.getDigest();
	    assertTrue(digest.containsAll(_alternateLocations));
	    
	    
	    // assert writing out to byte array stream and byte is identical
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    digest.write(baos);
	    byte [] toByte = digest.toBytes();
	    byte [] stream = baos.toByteArray();
	    assertTrue(Arrays.equals(toByte,stream));
	    
	    digest = AltLocDigest.parseDigest(toByte,0,toByte.length);
	    assertTrue(digest.containsAll(_alternateLocations));
	    
	}
	
	public void testXORing() throws Exception {
	    
	    //XORing oneself = 0
	    AltLocDigest digest = _direct.getDigest();
	    digest =(AltLocDigest) digest.XOR(digest);
	    
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertFalse(digest.contains(loc));
	    }
	    
	    //now both digests will contain a pushloc which should not exist in the final digest
	    _direct.add(pa);
	    digest = _direct.getDigest();
	    assertTrue(digest.contains(pa));
	    AltLocDigest digest2 = _push.getPushDigest();
	    assertTrue(digest2.contains(pa));
	    digest = (AltLocDigest)digest.XOR(digest2);
	    
	    assertFalse(digest.contains(pa));
	}
	
	public void testANDing() throws Exception {
	    //anding with empty = empty
	    AltLocDigest digest = _direct.getDigest();
	    digest = (AltLocDigest)digest.AND(_direct.getPushDigest());
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertFalse(digest.contains(loc));
	    }
	    
	    _direct.add(pa);
	    assertTrue(digest.contains(pa));
	    assertFalse(digest.contains(pa2));
	    AltLocDigest digest2 = _push.getPushDigest();
	    assertTrue(digest2.contains(pa));
	    assertTrue(digest2.contains(pa2));
	    digest = (AltLocDigest)digest.AND(digest2);
	    assertTrue(digest.contains(pa));
	    assertFalse(digest.contains(pa2));
	}
	
	public void testORing() throws Exception {
	    //oring with empty = oneself
	    AltLocDigest digest = _direct.getDigest();
	    digest = (AltLocDigest)digest.AND(_direct.getPushDigest());
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertTrue(digest.contains(loc));
	    }
	    
	    _direct.add(pa);
	    assertTrue(digest.contains(pa));
	    assertFalse(digest.contains(pa2));
	    AltLocDigest digest2 = _push.getPushDigest();
	    assertTrue(digest2.contains(pa));
	    assertTrue(digest2.contains(pa2));
	    digest = (AltLocDigest)digest.AND(digest2);
	    assertTrue(digest.contains(pa));
	    assertTrue(digest.contains(pa2));
	}
	
	public void testNOTing() throws Exception {
	    
	    AltLocDigest digest = _direct.getDigest();
	    digest = (AltLocDigest)digest.invert();
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertFalse(digest.contains(loc));
	    }
	}
}
