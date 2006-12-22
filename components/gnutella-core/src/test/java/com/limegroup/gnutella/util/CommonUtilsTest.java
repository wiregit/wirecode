package com.limegroup.gnutella.util;

import java.io.File;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;


import junit.framework.Test;

/**
 * Tests certain features of CommonUtils
 */
public class CommonUtilsTest extends com.limegroup.gnutella.util.LimeTestCase {

    public CommonUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CommonUtilsTest.class);
    }  
    
    public void testMajorRevisionMethod() {
        int majorVersion = LimeWireUtils.getMajorVersionNumber();
        assertEquals(2,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("3.7.7");
        assertEquals(3,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("14.7.7");
        assertEquals(14,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("13.34.7");
        assertEquals(13,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal(".34.7");
        assertEquals(2,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("2.7.13");
        assertEquals("unexpected major version number",2, majorVersion); 
    }

    public void testMinorRevisionMethod() {
        int minorVersion = LimeWireUtils.getMinorVersionNumber();
        assertEquals(7,minorVersion);
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("3.8.7");
        assertEquals(8,minorVersion);
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("14.13.7");
        assertEquals(13,minorVersion);
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("2.34.7");
        assertEquals(34,minorVersion);        
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("..7");
        assertEquals(7,minorVersion);        
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("2..7");
        assertEquals(7,minorVersion);    

        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("2.7.13");
        assertEquals("unexpected minor version number",7, minorVersion); 
    }


    

	/**
	 * Test the method for copying files from jars to disk.
	 */
	public void testCommonUtilsCopyResourceFile() throws Exception {
		File newResourceFile = new File(getSaveDirectory(), "copyTest");
		newResourceFile.deleteOnExit();
		String fileName = "org/apache/commons/logging/Log.class";
		File jarFile = CommonUtils.getResourceFile("commons-logging.jar");
		if(!jarFile.isFile())
			fail("jar not located");
		JarFile jar = new JarFile(jarFile);
		JarEntry entry = jar.getJarEntry(fileName);
		long entrySize = entry.getSize();
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
			(char[])PrivilegedAccessor.getValue(LimeWireUtils.class, 
												"ILLEGAL_CHARS_ANY_OS");

		char[] illegalCharsUnix = 
			(char[])PrivilegedAccessor.getValue(LimeWireUtils.class, 
												"ILLEGAL_CHARS_UNIX");

		char[] illegalCharsWindows = 
			(char[])PrivilegedAccessor.getValue(LimeWireUtils.class, 
												"ILLEGAL_CHARS_WINDOWS");

		runCharTest(illegalChars);
		
		if(OSUtils.isUnix()) {
			runCharTest(illegalCharsUnix);
		}
		if(OSUtils.isWindows()) {
			runCharTest(illegalCharsWindows);
		}

		// now, test really long strings to make sure they're truncated.
		String testStr = "FPJWJIEJFFJSFHIUHBUNCENCNUIEHCEHCEHUCIEBCUHEUHULHULHLH"+
			"JFDHCIOSHCIOSJCIODJSKJFDSJFKSLDHFUIOSHFUDSBUBBUIBCUDISLBCLSDBFKSCLJ"+
			"JFDHCIOSHCIOSJCIODJSKJFDSJFKSLDHFUIOSHFUDSBUBBUIBCUDISLBCLSDBFKSCLJ"+
			"JFDHCIOSHCIOSJCIODJSKJFDSJFKSLDHFUIOSHFUDSBUBBUIBCUDISLBCLSDBFKSCLJ"+
			"JFDHCIOSHCIOSJCIODJSKJFDSJFKSLDHFUIOSHFUDSBUBBUIBCUDISLBCLSDBFKSCLJ"+
			"JFDHCIOSHCIOSJCIODJSKJFDSJFKSLDHFUIOSHFUDSBUBBUIBCUDISLBCLSDBFKSCLJ"+
			"JFDHCIOSHCIOSJCIODJSKJFDSJFKSLDHFUIOSHFUDSBUBBUIBCUDISLBCLSDBFKSCLJ"+
			"JFDHCIOSHCIOSJCIODJSKJFDSJFKSLDHFUIOSHFUDSBUBBUIBCUDISLBCLSDBFKSCLJ";
		assertGreaterThan("string should be longer", 300, testStr.length());
		testStr = CommonUtils.convertFileName(testStr);
		assertEquals("unexpected string length", 180, testStr.length());
		
		// test conversion with an extension.
		StringBuffer twoHundred = new StringBuffer(200);
		for(int i = 0; i < 200; i++) twoHundred.append("a");
		String withExt = twoHundred + ".zip";
		String withBigExt = twoHundred.substring(0, 170) + ".ziiiiiiiiiiiiiip";
		String testOne = CommonUtils.convertFileName(withExt);
		String testTwo = CommonUtils.convertFileName(withBigExt);
		String expectedOne = twoHundred.substring(0, 176) + ".zip";
		String expectedTwo = twoHundred.substring(0, 163) + ".ziiiiiiiii";
		assertEquals("unexpected length1", expectedOne.length(), testOne.length());
		assertEquals("unexpected conversion1", expectedOne, testOne);
		assertEquals("unexpected length2", expectedTwo.length(), testTwo.length());		
		assertEquals("unexpected conversion2", expectedTwo, testTwo);
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
