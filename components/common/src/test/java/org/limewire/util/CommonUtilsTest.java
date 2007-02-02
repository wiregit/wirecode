package org.limewire.util;

import java.io.File;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import junit.framework.Test;

public class CommonUtilsTest extends BaseTestCase {


    public CommonUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CommonUtilsTest.class);
    }  


    /**
     * Test the method for copying files from jars to disk.
     */
    public void testCommonUtilsCopyResourceFile() throws Exception {
        File newResourceFile = File.createTempFile("copy", "test");
        newResourceFile.delete();
        newResourceFile.deleteOnExit();
        String fileName = "org/apache/commons/logging/Log.class";
        URL url = ClassLoader.getSystemResource(fileName);
        // Hack out the jarfile location -- strip off jar:file: from beginning, and !/<name> from end
        String jarPath = url.toString().substring("jar:file:".length(), url.toString().length() -  fileName.length() - 2);
        File jarFile = new File(jarPath);
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
            (char[])PrivilegedAccessor.getValue(CommonUtils.class, 
                                                "ILLEGAL_CHARS_ANY_OS");

        char[] illegalCharsUnix = 
            (char[])PrivilegedAccessor.getValue(CommonUtils.class, 
                                                "ILLEGAL_CHARS_UNIX");

        char[] illegalCharsWindows = 
            (char[])PrivilegedAccessor.getValue(CommonUtils.class, 
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
