package org.limewire.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
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
        String expectedTwo = twoHundred.substring(0, 169) + ".ziiiiiiiii";
        assertEquals("unexpected length1", expectedOne.length(), testOne.length());
        assertEquals("unexpected conversion1", expectedOne, testOne);
        assertEquals("unexpected length2", expectedTwo.length(), testTwo.length());     
        assertEquals("unexpected conversion2", expectedTwo, testTwo);
    }
    
    public void testConvertFileNameWithUnicodeInput() {
        String fileSystem = "ファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステム";
        String converted = CommonUtils.convertFileName(fileSystem);
        assertTrue(converted.length() <= 255);
        assertEquals(180, converted.getBytes().length);
    }
    
    public void testConvertFileNameWithExtensionAndUnicodeInput() {
        String[] fileSystems =  { 
                "ファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシ.ステム",
                "ファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステム.txt",
                "ファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイルシステムファイ.ルシステムファイルシステム",
        };
        for (String fileSystem : fileSystems) { 
            String converted = CommonUtils.convertFileName(fileSystem);
            assertTrue(fileSystem + " not 180 bytes long", converted.getBytes().length <= 180);
            // still has extension
            assertTrue(converted.contains("."));
            // extension is substring of original extension
            assertTrue(fileSystem.substring(fileSystem.indexOf('.')).contains(converted.substring(converted.indexOf('.'))));
        }

        // test identity for small end substrings
        for (String fileSystem : fileSystems) {
            String halfed = fileSystem.substring(fileSystem.length() - 40);
            String converted = CommonUtils.convertFileName(halfed);
            assertEquals(halfed, converted);
        }
    }
    
    public void testConvertFileNameIdentity() {
        assertEquals("test", CommonUtils.convertFileName("test"));
        assertEquals("test.me", CommonUtils.convertFileName("test.me"));
        assertEquals("Test me with Spaces!.meee", CommonUtils.convertFileName("Test me with Spaces!.meee"));
        assertEquals("long.extension", CommonUtils.convertFileName("long.extension"));
    }
    
    public void testConvertFileNameWithParentDir() throws IOException {
        File dir = new File("/short/dir/");
        assertEquals("test", CommonUtils.convertFileName(dir, "test"));
        assertEquals("test.me", CommonUtils.convertFileName(dir, "test.me"));
        assertEquals("Test me with Spaces!.meee", CommonUtils.convertFileName(dir, "Test me with Spaces!.meee"));
        assertEquals("long.extension", CommonUtils.convertFileName(dir, "long.extension"));
        
        char[] dirName = new char[OSUtils.getMaxPathLength()];
        Arrays.fill(dirName, 'a');
        for (int i = 0; i < dirName.length; i += 254) {
            dirName[i] = '/';
        }
        dir = new File(new String(dirName));
        try {
            CommonUtils.convertFileName(dir, "lbkajdf ;alksdf");
            fail("IOException expected for too long parent dir");
        }
        catch (IOException iee) {
        }
        dir = new File(new String(dirName, 0, dirName.length - 2));
        assertEquals(1, CommonUtils.convertFileName(dir, "blah, blhalksd").getBytes().length);
        dir = new File(new String(dirName, 0, dirName.length - 5));
        assertEquals("1234", CommonUtils.convertFileName(dir, "12345"));
    }
    
    public void testMaxBytesParameter() {
        assertEquals("1", CommonUtils.convertFileName("12345", 1));
        assertEquals("12", CommonUtils.convertFileName("12345", 2));
        
        try {
            CommonUtils.convertFileName("1345", 0);
            fail("No IAE thrown");
        }
        catch (IllegalArgumentException iae) {
        }
    }
    
    public void testNoTrailingSeparator() throws IOException {
        File tmpFile = File.createTempFile("tmpfile", "file");
        File tmpDir = tmpFile.getParentFile();
        tmpFile.delete();
        
        assertFalse(tmpDir.getAbsolutePath().endsWith(File.separator));
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
