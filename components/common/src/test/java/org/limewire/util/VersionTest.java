package org.limewire.util;

import junit.framework.Test;

public final class VersionTest extends BaseTestCase {

	public VersionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(VersionTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testInvalidVersions() throws Exception {
	    try {
	        new Version("@version@");
	        fail("invalid v.");
	    } catch(VersionFormatException expected) {}
	        
        try {
            new Version("1");
            fail("invalid v");
        } catch(VersionFormatException expected) {}
            
        try {
            new Version("1.2");
            fail("invalid v");
        } catch(VersionFormatException expected) {}
            
        
        try {
            new Version("1.3.a");
            fail("invalid v");
        } catch(VersionFormatException expected) {}
            
        try {
            new Version("1.a.3");
            fail("invalid v");
        } catch(VersionFormatException expected) {}
            
        try {
            new Version("1.a");
            fail("invalid v");
        } catch(VersionFormatException expected) {}
    }
    
    public void testValidVersion() throws Exception {
        new Version("0.0.0");
        new Version("1.2.3");
        new Version("10.2.3052");
        new Version("10.2.3_05");
        new Version("2.1.81");
        new Version("4.6.0 Pro");
        new Version("4.6.0jum");
        new Version("4.6.0_01");
        new Version("4.6.0jum233");
        new Version("4.6.0 666");
        new Version("4.6.0_01abc");
        new Version("1.5.0b56");
        new Version("1.5.0_01");
        new Version("1.5.0_02");
    }
    
    public void testComparingVersions() throws Exception {
        Version v1, v2;
        
        v1 = new Version("0.0.0");
        v2 = new Version("0.0.0");
        assertEquals(v1, v2);
        
        v2 = new Version("0.0.0_1");
        // make sure aGT a aLT are doing it the way we expect ...
        // param 2 is 'X than' param 1
        assertGreaterThan(0, v2.compareTo(v1));
        assertLessThan(0, v1.compareTo(v2));

        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);
        
        
        v2 = new Version("1.0.0");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);
        
        v2 = new Version("0.1.0");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);
        
        v2 = new Version("0.0.1");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);
        
        // try with a leading zero.
        v2 = new Version("0.0.0_01");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);
        
        // try with words
        v2 = new Version("0.0.0 PRO");
        assertEquals(v1, v2);
        
        v2 = new Version("0.0.1 PRO");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);
    }
        
}