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
        assertEquals(2,majorVersion);
        majorVersion = CommonUtils.getMajorVersionNumberInternal("3.7.7");
        assertEquals(3,majorVersion);
        majorVersion = CommonUtils.getMajorVersionNumberInternal("14.7.7");
        assertEquals(14,majorVersion);
        majorVersion = CommonUtils.getMajorVersionNumberInternal("13.34.7");
        assertEquals(13,majorVersion);
        majorVersion = CommonUtils.getMajorVersionNumberInternal(".34.7");
        assertEquals(2,majorVersion);
        majorVersion = CommonUtils.getMajorVersionNumberInternal("2.7.13");
        assertEquals("unexpected major version number",2, majorVersion); 
    }

    public void testMinorRevisionMethod() {
        int minorVersion = CommonUtils.getMinorVersionNumber();
        assertEquals(7,minorVersion);
        minorVersion = CommonUtils.getMinorVersionNumberInternal("3.8.7");
        assertEquals(8,minorVersion);
        minorVersion = CommonUtils.getMinorVersionNumberInternal("14.13.7");
        assertEquals(13,minorVersion);
        minorVersion = CommonUtils.getMinorVersionNumberInternal("2.34.7");
        assertEquals(34,minorVersion);        
        minorVersion = CommonUtils.getMinorVersionNumberInternal("..7");
        assertEquals(7,minorVersion);        
        minorVersion = CommonUtils.getMinorVersionNumberInternal("2..7");
        assertEquals(7,minorVersion);    

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

	/**
	 * Tests that the method for converting file name strings to use
	 * only cross-platform characters works correctly.
	 */
	public void testCommonUtilsConvertFileName() throws Exception {
		char[] illegalChars = 
			(char[])PrivilegedAccessor.getValue(CommonUtils.class, 
												"ILLEGAL_CHARS_ANY_OS");

		char[] illegalCharsUnix = 
			(char[])PrivilegedAccessor.getValue(CommonUtils.class, 
												"ILLEGAL_CHARS_UNIX");

		char[] illegalCharsWindows = 
			(char[])PrivilegedAccessor.getValue(CommonUtils.class, 
												"ILLEGAL_CHARS_WINDOWS");

		runCharTest(illegalChars);
		
		if(CommonUtils.isUnix()) {
			runCharTest(illegalCharsUnix);
		}
		if(CommonUtils.isWindows()) {
			runCharTest(illegalCharsWindows);
		}
	}

	/**
	 * Helper method for testing illegal character conversion method.
	 */
	private void runCharTest(char[] illegalChars) {
		String test = "test";
		String correctResult = "test_";
		for(int i=0; i<illegalChars.length; i++) {
			String curTest = CommonUtils.convertFileName(test + illegalChars[i]);
			assertEquals("illegal char: "+illegalChars[i]+ " not replaced correctly",
						 correctResult, curTest);
		}
	}

}
