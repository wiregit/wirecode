
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
import com.limegroup.gnutella.util.PrivilegedAccessor;

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
		GUID g2 = new GUID(GUID.makeGuid());
		_push = AlternateLocationCollection.create(FileDescStub.DEFAULT_SHA1);
        PushEndpoint pe = new PushEndpoint(g.toHexString()+";1:2.2.2.2;1.1.1.1:2");
        PushEndpoint pe2 = new PushEndpoint(g2.toHexString()+";2:3.3.3.3;2.2.2.2:3");
        pa = new PushAltLoc(pe,FileDescStub.DEFAULT_SHA1);
        pa2 = new PushAltLoc(pe2,FileDescStub.DEFAULT_SHA1);
        _push.add(pa);
        _push.add(pa2);
	}
	
	/**
	 * tests that parsing digests with the standard element size works
	 */
	public void testStandardSize() throws Exception {
	    byte [] digest = new byte[6+3];
	    digest[0] = 12;
	    digest[1] = 4;
	    digest[2] = 0;
	    for (int i =3;i < 9;i++)
	        digest[i]=(byte)i;
	    
	    AltLocDigest parsed = AltLocDigest.parseDigest(digest,0,9);
	    
	    BitSet b = (BitSet)PrivilegedAccessor.getValue(parsed,"_values");
	    assertEquals(4,b.cardinality());
	    
	    int elementSize = ((Integer)PrivilegedAccessor.getValue(parsed,"_elementSize")).intValue();
	    assertEquals(12,elementSize);
	    
	    byte [] digest2 = parsed.toBytes();
	    assertEquals(digest[0],digest2[0]);
	    assertEquals(digest[1],digest2[1]);
	    assertFalse(Arrays.equals(digest,digest2));
	    
	    parsed = AltLocDigest.parseDigest(digest2,0,9);
	    byte [] digest3 = parsed.toBytes();
	    assertEquals(digest2,digest3);
	}
	/**
	 * tests that reading of a digest with different element size works fine
	 */
	public void testParseDifferentSize() throws Exception {
	    // create a digest with ten values, where each value is 1 byte
	    byte [] digest = new byte[3+10];
	    digest[0] = 8;
	    digest[1] = 10;
	    digest[2] = 0;
	    for (int i = 3;i < 13; i++)
	        digest[i]=(byte)i;
	    
	    AltLocDigest parsed = AltLocDigest.parseDigest(digest,0,13);
	    
	    BitSet b = (BitSet)PrivilegedAccessor.getValue(parsed,"_values");
	    assertEquals(10,b.cardinality());
	    
	    int elementSize = ((Integer)PrivilegedAccessor.getValue(parsed,"_elementSize")).intValue();
	    assertEquals(8,elementSize);
	    
	    byte [] digest2 = parsed.toBytes();
	    assertTrue(Arrays.equals(digest,digest2));
	    
	    // rince and repeat, claiming that there are eight values, 10 bits each
	    digest[0] = 10;
	    digest[1] = 8;
	    
	    parsed = AltLocDigest.parseDigest(digest,0,13);
	    b = (BitSet)PrivilegedAccessor.getValue(parsed,"_values");
	    assertEquals(8,b.cardinality());
	    
	    elementSize = ((Integer)PrivilegedAccessor.getValue(parsed,"_elementSize")).intValue();
	    assertEquals(10,elementSize);
	    
	    // digest 2 will not be the same as digest1, since the numbers were not in increasing order
	    digest2 = parsed.toBytes();
	    assertFalse(Arrays.equals(digest,digest2));

	    // but it will be equal to digest3
	    parsed = AltLocDigest.parseDigest(digest2,0,13);
	    byte [] digest3 = parsed.toBytes();
	    assertTrue(Arrays.equals(digest3,digest2));
	    
	    // add an extra location, make sure it expands the serialized size with 2 bytes
	    AlternateLocation altloc =AlternateLocation.create("1.2.3.4:5",FileDescStub.DEFAULT_SHA1); 
	    parsed.add(altloc);
	    digest3 = parsed.toBytes();
	    assertEquals(digest2.length+2,digest3.length);
	    
	    parsed = AltLocDigest.parseDigest(digest3,0,15);
	    assertTrue(parsed.contains(altloc));
	    
	    // try boundary conditions
	    digest = new byte[24*8+3];
	    digest[0]=24;
	    digest[1]=8;
	    for (int i = 3;i<24*8;i++)
	        digest[i]=(byte)i; //wraparound is ok
	    
	    parsed = AltLocDigest.parseDigest(digest,0,24*8);
	    digest2 = parsed.toBytes();
	    parsed = AltLocDigest.parseDigest(digest2,0,digest2.length);
	    digest3 = parsed.toBytes();
	    assertTrue(Arrays.equals(digest3,digest2));
	    
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
	    digest.xor(digest);
	    
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertFalse(digest.contains(loc));
	    }
	    
	    //now both digests will contain a pushloc which should not exist in the final digest
	    _direct.add(pa);
	    digest = _direct.getPushDigest();
	    assertTrue(digest.contains(pa));
	    AltLocDigest digest2 = _push.getPushDigest();
	    assertTrue(digest2.contains(pa));
	    digest.xor(digest2);
	    
	    assertFalse(digest.contains(pa));
	}
	
	public void testANDing() throws Exception {
	    //anding with empty = empty
	    AltLocDigest digest = _direct.getDigest();
	    digest.and(_direct.getPushDigest());
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertFalse(digest.contains(loc));
	    }
	    
	    _direct.add(pa);
	    digest = _direct.getPushDigest();
	    assertTrue(digest.contains(pa));
	    assertFalse(digest.contains(pa2));
	    AltLocDigest digest2 = _push.getPushDigest();
	    assertTrue(digest2.contains(pa));
	    assertTrue(digest2.contains(pa2));
	    digest.and(digest2);
	    assertTrue(digest.contains(pa));
	    assertFalse(digest.contains(pa2));
	}
	
	public void testORing() throws Exception {
	    //oring with empty = oneself
	    AltLocDigest digest = _direct.getDigest();
	    digest.or(_direct.getPushDigest());
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertTrue(digest.contains(loc));
	    }
	    
	    _direct.add(pa);
	    digest = _direct.getPushDigest();
	    assertTrue(digest.contains(pa));
	    assertFalse(digest.contains(pa2));
	    AltLocDigest digest2 = _push.getPushDigest();
	    assertTrue(digest2.contains(pa));
	    assertTrue(digest2.contains(pa2));
	    digest.or(digest2);
	    assertTrue(digest.contains(pa));
	    assertTrue(digest.contains(pa2));
	}
	
	public void testNOTing() throws Exception {
	    
	    AltLocDigest digest = _direct.getDigest();
	    digest.invert();
	    for (Iterator iter = _alternateLocations.iterator();iter.hasNext();) {
	        AlternateLocation loc = (AlternateLocation)iter.next();
	        assertFalse(digest.contains(loc));
	    }
	}
}
