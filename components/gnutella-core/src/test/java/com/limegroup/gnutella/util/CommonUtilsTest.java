package com.limegroup.gnutella.util;

import junit.framework.*;

/**
 * Tests certain features of CommonUtils
 */
public class CommonUtilsTest extends TestCase {

    public CommonUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(CommonUtilsTest.class);
    }  

    public void testMajorRevisionMethod() {
        int majorVersion = CommonUtils.getMajorVersionNumber();
        assertTrue(majorVersion==2);
        majorVersion = CommonUtils.getMajorVersionNumberInternal("3.7.7");
        assertTrue(majorVersion==3);
        majorVersion = CommonUtils.getMajorVersionNumberInternal("14.7.7");
        assertTrue("14, instead majorVersion = " + majorVersion, 
                   majorVersion==14);
        majorVersion = CommonUtils.getMajorVersionNumberInternal("13.34.7");
        assertTrue(majorVersion==13);        
        majorVersion = CommonUtils.getMajorVersionNumberInternal(".34.7");
        assertTrue(majorVersion==2);        
    }

    public void testMinorRevisionMethod() {
        int minorVersion = CommonUtils.getMinorVersionNumber();
        assertTrue(minorVersion==7);
        minorVersion = CommonUtils.getMinorVersionNumberInternal("3.8.7");
        assertTrue(minorVersion==8);
        minorVersion = CommonUtils.getMinorVersionNumberInternal("14.13.7");
        assertTrue(minorVersion==13);
        minorVersion = CommonUtils.getMinorVersionNumberInternal("2.34.7");
        assertTrue(minorVersion==34);        
        minorVersion = CommonUtils.getMinorVersionNumberInternal("..7");
        assertTrue(minorVersion==7);        
        minorVersion = CommonUtils.getMinorVersionNumberInternal("2..7");
        assertTrue(minorVersion==7);        
    }
    

}
