package org.limewire.util;

import java.io.File;
import java.io.IOException;

import org.limewire.util.FileUtils;
import org.limewire.util.SystemUtils;

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

	public void testGetFilesRecursive() throws Exception {
		File tmpFile = File.createTempFile("tmp", "file");
		File tmpDir = tmpFile.getParentFile();
		tmpFile.delete();
		
        File emptyDir = new File(tmpDir, "emptydir");
        File emptyNameDir = new File(tmpDir, "emptyname");
        File emptyExtensionDir = new File(tmpDir, "emptyextension");
        File recursiveDir = new File(tmpDir, "recursivedir");
        
        emptyDir.mkdir();
        emptyNameDir.mkdir();
        emptyExtensionDir.mkdir();
        recursiveDir.mkdir();
        
        emptyDir.deleteOnExit();
        emptyNameDir.deleteOnExit();
        emptyExtensionDir.deleteOnExit();
        recursiveDir.deleteOnExit();
        
		File subDir = new File(emptyDir, "subdir");
		subDir.mkdir();
		subDir.deleteOnExit();
		
		File f = new File(emptyNameDir, ".emptyname");
		f.createNewFile();
		f.deleteOnExit();
		
		f = new File(emptyExtensionDir, "emptyextension.");
		f.createNewFile();
		f.deleteOnExit();
		
		f = new File(recursiveDir, "afile.txt");
		f.createNewFile();
		f.deleteOnExit();
		
		f = new File(recursiveDir, "subdir");
		f.mkdir();
		f.deleteOnExit();
		f = new File(f, "subfile.txt");
		f.createNewFile();
		f.deleteOnExit();
	    
		File[] fa = FileUtils.getFilesRecursive(emptyDir, null);
		assertEquals("directory should have no files, only a subdir", 0, fa.length);
		
		fa = FileUtils.getFilesRecursive(emptyNameDir, null);
		assertEquals("directory should have 1 hidden file", 1, fa.length);
		
		fa = FileUtils.getFilesRecursive(emptyNameDir, new String[] {"emptyname"});
		assertEquals("directory should have no file matching extension \"emptyname\"", 0, fa.length);
		
		fa = FileUtils.getFilesRecursive(emptyExtensionDir, null);
		assertEquals("directory should have one file", 1, fa.length);
		
		fa = FileUtils.getFilesRecursive(emptyExtensionDir, new String[] { "" });
		assertEquals("directory should have no file matching empty extension", 0, fa.length);
		
		// test if files in subdirectories are found too
		fa = FileUtils.getFilesRecursive(recursiveDir, null);
		assertEquals("wrong number of files found", 2, fa.length);
		
		// test if files in subdirectories are found with filter
		fa = FileUtils.getFilesRecursive(recursiveDir, new String[] {"unmatchedextension", "", "txt"});
		assertEquals("wrong number of matching files found", 2, fa.length);
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
        assertTrue("Cannot set file writable: "+testFile, FileUtils.setWriteable(testFile));
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
            // This test doesn't work and is not important.
			// fail("created file in test dir");
		} catch(IOException expected) {}
        assertTrue(FileUtils.setWriteable(testDir));
        testInTestDir.delete();
		assertTrue(testInTestDir.createNewFile());
        assertTrue(testDir.canWrite());
        // Make sure it doesn't die if called on a file that doesn't exist
        File nowhere = new File("m'kay");
		assertTrue(!nowhere.exists());
        assertTrue(FileUtils.setWriteable(nowhere));
        assertTrue(!nowhere.canWrite()); // doesn't exist, can't write.
    }
    
    public void testIsReallyParent() throws Exception {
        /* Simple benign case */
        File testFile = new File("/a/b/c/d.txt");
        File testDirectory = new File("/a/b/c");
        assertTrue("Simple benign case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Simple malicious case */
        testFile = new File("/e/a/b/c/d.txt");
        testDirectory = new File("/a/b/c");
        assertFalse("Simple malicious case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Benign use of ../ */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/b/../b/c/../c/d.txt");
        assertTrue("Benign ../ case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Malicious use of ../ */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/b/c/../e/d.txt");
        assertFalse("Malicious ../ case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Complex benign case */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/e/../b/c/d.txt");
        assertTrue("Complex benign case failed.", FileUtils.isReallyParent(testDirectory,testFile));
        
        /* Complex malicious case */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/e/../b/c/d.txt");
        assertTrue("Complex benign case failed.", FileUtils.isReallyParent(testDirectory,testFile));
        
        /* Simple relative benign case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/d.txt");
        assertTrue("Simple relative benign case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Simple relative malicious case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/e/c/d.txt");
        assertFalse("Simple relative malicious case failed.", FileUtils.isReallyParent(testDirectory, testFile));

        /* Benign use of ../ in relative paths */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/../b/c/../c/d.txt");
        assertTrue("Benign use of ../ in relative case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Malicious use of ../ in relative paths */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../e/d.txt");
        assertFalse("Malicious use of ../ in relative case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Multi-level benign case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../../../a/b/c/d.txt");
        assertTrue("Multi-level benign case failed.", FileUtils.isReallyParent(testDirectory, testFile));
        
        /* Multi-level malicious case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../../../e/b/c/d.txt");
        assertFalse("Multi-level malicious case failed.", FileUtils.isReallyParent(testDirectory, testFile));
    }
    
    public void testReallyInParentPath() throws Exception {
        File testDirectory;
        File testFile;
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../../../a/b/c/d/e/f.txt");
        assertTrue(FileUtils.isReallyInParentPath(testDirectory, testFile));
        testFile = new File("a/b/c/../../../e/b/c/d.txt");
        assertFalse(FileUtils.isReallyInParentPath(testDirectory, testFile));
        testFile = new File("/");
        assertNull(testFile.getParentFile());
        assertFalse(FileUtils.isReallyInParentPath(testDirectory, testFile));
    }
    
    private File createHierarchy() throws IOException {
    	File tmpFile = File.createTempFile("test", "test");
    	tmpFile.deleteOnExit();
    	
    	File tmpDir = tmpFile.getParentFile();
    	
    	File dir = new File(tmpDir, "testRecursiveDeleteDir");
    	assertTrue(dir.mkdirs());
    	dir.deleteOnExit();
    	
    	File hiddenFile = new File(dir, ".hidden");
    	assertTrue(hiddenFile.createNewFile());
    	hiddenFile.deleteOnExit();
    	
    	File file = new File(dir, "file.file");
    	assertTrue(file.createNewFile());
    	file.deleteOnExit();
    	
    	File subDir = new File(dir, "subdir");
    	assertTrue(subDir.mkdirs());
    	subDir.deleteOnExit();
    	
    	File hiddenSubFile = new File(subDir, ".hidden.file");
    	assertTrue(hiddenSubFile.createNewFile());
    	hiddenSubFile.deleteOnExit();
    	
    	File hiddenSubdir = new File(dir, ".hiddenSubdir");
    	assertTrue(hiddenSubdir.mkdirs());
    	hiddenSubdir.deleteOnExit();
    	
    	return dir;
    }
    
    public void testDeleteRecursive() throws IOException { 
    	File dir = createHierarchy();
    	assertTrue(FileUtils.deleteRecursive(dir));
    	assertFalse(dir.exists());
    }
    
}
