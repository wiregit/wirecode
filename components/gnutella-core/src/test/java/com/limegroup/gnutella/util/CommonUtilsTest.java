package com.limegroup.gnutella.util;

import junit.framework.*;
import com.sun.java.util.collections.*;
import java.util.jar.*;
import java.io.*;

/**
 * Tests certain features of CommonUtils
 */
public class CommonUtilsTest extends com.limegroup.gnutella.util.BaseTestCase {

    public CommonUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CommonUtilsTest.class);
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
        majorVersion = CommonUtils.getMajorVersionNumberInternal("2.7.13");
        assertEquals("unexpected major version number",2, majorVersion); 
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

        minorVersion = CommonUtils.getMinorVersionNumberInternal("2.7.13");
        assertEquals("unexpected minor version number",7, minorVersion); 
    }


	public void testCommonUtilsPortCheck() {
		int port = -1;
		assertTrue("port should not be valid", !CommonUtils.isValidPort(port));
		port = 99999999;
		assertTrue("port should not be valid", !CommonUtils.isValidPort(port));
		port = 20;
		assertTrue("port should be valid", CommonUtils.isValidPort(port));
	}
    

	/**
	 * Test the method for copying files from jars to disk.
	 */
	public void testCommonUtilsCopyResourceFile() throws Exception {
		File newResourceFile = new File(getSaveDirectory(), "copyTest");
		newResourceFile.deleteOnExit();
		String fileName = "com/sun/java/util/collections/Comparable.class";
		File collectionsFile = CommonUtils.getResourceFile("lib/collections.jar");
		if(!collectionsFile.isFile()) {
			fail("collections.jar not located");
		}
		JarFile collections = new JarFile(collectionsFile);
		JarEntry entry = collections.getJarEntry(fileName);
		long entrySize = entry.getCompressedSize();
		CommonUtils.copyResourceFile(fileName, newResourceFile, false);
		assertEquals("size of file in jar should equal size on disk", 
					 entrySize, newResourceFile.length());

		newResourceFile.delete();
	}

}
