package org.limewire.core.impl;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;

public class URNImplTest extends BaseTestCase {

    public URNImplTest(String name) {
        super(name);
    }

    public void testBasics() throws Exception {
        try {
            new URNImpl(null);
            fail("UrnImpl should not be able to take a null urn as a parameter.");
        } catch(NullPointerException e) {
            //expected
        }
        
        URN urn1 = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        URN urn2 = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        URN urn3 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB");
        URNImpl urnImpl1 = new URNImpl(urn1);
        URNImpl urnImpl2 = new URNImpl(urn2);
        URNImpl urnImpl3 = new URNImpl(urn3);

        assertTrue(urnImpl1.equals(urnImpl2));
        assertTrue(urnImpl2.equals(urnImpl1));
        assertFalse(urnImpl1.equals(urnImpl3));
        assertFalse(urnImpl2.equals(urnImpl3));
        assertFalse(urnImpl3.equals(urnImpl1));
        assertFalse(urnImpl3.equals(urnImpl2));
        assertFalse(urnImpl1.equals("test"));
        assertFalse(urnImpl2.equals("test"));
        assertFalse(urnImpl3.equals("test"));

        assertEquals(urnImpl1.hashCode(), urnImpl2.hashCode());
        
        assertEquals(0, urnImpl1.compareTo(urnImpl2));
        assertEquals(0, urnImpl2.compareTo(urnImpl1));
        
        assertNotEquals(0, urnImpl1.compareTo(urnImpl3));
        assertNotEquals(0, urnImpl2.compareTo(urnImpl3));
        assertNotEquals(0, urnImpl3.compareTo(urnImpl1));
        assertNotEquals(0, urnImpl3.compareTo(urnImpl2));
        
        assertEquals(urn1, urnImpl1.getUrn());
        assertEquals(urn2, urnImpl2.getUrn());
        assertEquals(urn3, urnImpl3.getUrn());
        

        assertEquals(urnImpl1.toString(), urnImpl2.toString());
        assertEquals(urnImpl2.toString(), urnImpl1.toString());
        
        assertNotEquals(urnImpl1.toString(), urnImpl3.toString());
        assertNotEquals(urnImpl2.toString(), urnImpl3.toString());
        assertNotEquals(urnImpl3.toString(), urnImpl1.toString());
        assertNotEquals(urnImpl3.toString(), urnImpl2.toString());
    }
}
