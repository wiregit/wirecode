package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

/**
 * Class for testing the FileUtils utility methods.
 */
public final class FileUtilsTest extends BaseTestCase {

    /**
     * @param name
     */
    public FileUtilsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(FileUtilsTest.class);
    }

    /**
     * Tests the method for extracting the file extension from a file.
     * 
     * @throws Exception if any error occurs.
     */
    public void testGetFileExtension() throws Exception  {
        final String EXT = "ext";
        File[] testFiles = new File[]  {
          new File("standard."+EXT),  
          new File("a.little.weird."+EXT), 
          new File(".ext."+EXT), 
        };   
        
        for(int i=0; i<testFiles.length; i++)  {
            assertEquals("unexpected extension extracted", 
                EXT, FileUtils.getFileExtension(testFiles[i]));
        }
        
        // test files that should return null
        File[] nullFiles = new File[]  {
          new File("standard."),  
          new File("."+EXT), 
          new File("noextension"), 
        };
        
        for(int i=0; i<nullFiles.length; i++)  {
            assertNull("extension should be null", 
                FileUtils.getFileExtension(nullFiles[i]));
        };
    }
    
    /**
     * Tests the setWriteable method.
     */
    public void testSetWriteable() throws Exception {
        PrivilegedAccessor.setValue(SystemUtils.class, "isLoaded", Boolean.TRUE);
        
        File testFile = File.createTempFile("test", "file");
        testFile.deleteOnExit();
        testFile.setReadOnly();
		assertTrue(testFile.exists());
		assertTrue(testFile.isFile());
        assertTrue(!testFile.canWrite());
        assertTrue(FileUtils.setWriteable(testFile));
        assertTrue(testFile.canWrite());
		SystemUtils.setWriteable(testFile.getPath());
		assertTrue(FileUtils.setWriteable(testFile));
        File testDir = new File("directory");
		testDir.deleteOnExit();
        testDir.mkdirs();
		assertTrue(testDir.exists());
		assertTrue(testDir.isDirectory());
        testDir.setReadOnly();
        assertTrue(!testDir.canWrite());
		File testInTestDir = new File(testDir, "testDirTest");
		testInTestDir.deleteOnExit();
		try {
			testInTestDir.createNewFile();
			fail("created file in test dir");
		} catch(IOException expected) {}
        assertTrue(FileUtils.setWriteable(testDir));
		assertTrue(testInTestDir.createNewFile());
        assertTrue(testDir.canWrite());
        // Make sure it doesn't die if called on a file that doesn't exist
        File nowhere = new File("m'kay");
		assertTrue(!nowhere.exists());
        assertTrue(FileUtils.setWriteable(nowhere));
        assertTrue(!nowhere.canWrite()); // doesn't exist, can't write.
    }
}
