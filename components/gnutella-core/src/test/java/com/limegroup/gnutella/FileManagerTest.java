package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.messages.*;
import junit.framework.*;
import java.io.*;

public class FileManagerTest extends com.limegroup.gnutella.util.BaseTestCase {

    private static final String EXTENSION = "XYZ";
    
    private File f1 = null;
    private File f2 = null;
    private File f3 = null;
    private File f4 = null;
    private File f5 = null;
    private File f6 = null;
    private FileManager fman = null;
    private Object loaded = new Object();
    private Response[] responses;
    private File[] files;

    public FileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileManagerTest.class);
    }
    
	public void setUp() throws Exception {
        SettingsManager settings=SettingsManager.instance();
        settings.setExtensions(EXTENSION);
        	    
	    cleanFiles(_sharedDir, false);
	    fman = new FileManager();
	    PrivilegedAccessor.setValue(fman, "_callback", new FManCallback());
	    
	}
	
	public void tearDown() {
        if (f1!=null) f1.delete();
        if (f2!=null) f2.delete();
        if (f3!=null) f3.delete();
        if (f4!=null) f4.delete();
        if (f5!=null) f5.delete();
        if (f6!=null) f6.delete();	    
    }
    
    
    
    public void testGetParentFile() throws Exception {
        f1 = createNewTestFile(1);
        assertEquals("getParentFile doesn't work",
            new File(fman.getParentFile(f1).getCanonicalPath()),
            new File(_sharedDir.getCanonicalPath()));
    }
    
    public void testGetSharedFilesWithNoShared() throws Exception {
        File[] files=fman.getSharedFiles(_sharedDir);
        assertNull("should not be sharing any files", files);
    }
    
    public void testOneSharedFile() throws Exception {
        f1 = createNewTestFile(1);
        waitForLoad();
        f2 = createNewTestFile(3);
        f3 = createNewTestFile(11);

        // fman should only have loaded f1
        assertEquals("Unexpected number of shared files", 
            1, fman.getNumFiles());
        assertEquals("Unexpected size of filemanager",
            1, fman.getSize());
        responses=fman.query(QueryRequest.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        
        // should not be able to remove unshared file
        assertTrue("should have not been able to remove f3", 
				   !fman.removeFileIfShared(f3));
				   
        assertEquals("first file should be f1", f1, fman.get(0).getFile());
        
        files=fman.getSharedFiles(_sharedDir);
        assertEquals("unexpected length of shared files", 1, files.length);
        assertEquals("files should be the same", files[0], f1);
        files=fman.getSharedFiles(FileManager.getParentFile(_sharedDir));
        assertNull("file manager listed shared files in file's parent dir",
            files);
    }
    
    public void testAddingOneSharedFile() throws Exception {
        f1 = createNewTestFile(1);
        waitForLoad();
        f2 = createNewTestFile(3);
        f3 = createNewTestFile(11);

        assertTrue("should not have been able to share file",
                  !fman.addFileIfShared(new File("C:\\bad.ABCDEF")));
        assertTrue("should have been able to share file", 
                  fman.addFileIfShared(f2));
        assertEquals("unexpected number of files", 2, fman.getNumFiles());
        assertEquals("unexpected fman size", 4, fman.getSize());
        responses=fman.query(QueryRequest.createQuery("unit", (byte)3));
        assertTrue("responses gave same index " +
             "[0:" + responses[0].getIndex() + "], " +
             "[1:" + responses[1].getIndex() + "]",
             responses[0].getIndex()!=responses[1].getIndex());
        for (int i=0; i<responses.length; i++) {
            assertTrue("responses should be expected indexes", 
                responses[i].getIndex()==0 || responses[i].getIndex()==1);
        }
        files=fman.getSharedFiles(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("first shared file is not f1", files[0], f1);
        assertEquals("second shared file is not f2", files[1], f2);
    }
    
    public void testRemovingOneSharedFile() throws Exception {
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        f3 = createNewTestFile(11);

        //Remove file that's shared.  Back to 1 file.                        
        assertTrue("shouldn't have been able to remove unshared file", 
            !fman.removeFileIfShared(f3));
        assertTrue("should have been able to remove shared file", 
            fman.removeFileIfShared(f2));
        assertEquals("unexpected fman size", 1, fman.getSize());
        assertEquals("unexpected number of files", 1, fman.getNumFiles());
        responses=fman.query(QueryRequest.createQuery("unit", (byte)3));
        assertEquals("unexpected response length", 1, responses.length);
        files=fman.getSharedFiles(_sharedDir);
        assertEquals("unexpected files length", 1, files.length);
        assertEquals("files differ", files[0], f1);
    }
    
    public void testAddAnotherSharedFileDifferentIndex() throws Exception {
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        f3 = createNewTestFile(11);
        fman.removeFileIfShared(f2);

        //Add a new second file, with new index.
        assertTrue("should have been able to add shared file", 
            fman.addFileIfShared(f3));
        assertEquals("unexpected file size", 12, fman.getSize());
        assertEquals("unexpedted number of files", 2, fman.getNumFiles());
        responses=fman.query(QueryRequest.createQuery("unit", (byte)3));
        assertEquals("unexpected response length", 2, responses.length);
        assertTrue("response[0] index should not be 1", responses[0].getIndex()!=1);
        assertTrue("response[1] index should not be 1", responses[1].getIndex()!=1);
        fman.get(0);
        fman.get(2);
        try {
            fman.get(1);
            fail("should not have gotten anything");
        } catch (IndexOutOfBoundsException e) { }

        responses=fman.query(QueryRequest.createQuery("*unit*", (byte)3));
        assertEquals("unexpected responses length", 2, responses.length);

        files=fman.getSharedFiles(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[0], f1);
        assertEquals("files differ", files[1], f3);
        files=fman.getSharedFiles(null);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[0], f1);
        assertEquals("files differ", files[1], f3);
    }
    
    public void testRenameSharedFiles() throws Exception {
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        f3 = createNewTestFile(11);
        fman.removeFileIfShared(f2);
        fman.addFileIfShared(f3);

        //Rename files
        assertTrue("shouldn't have been able to rename unshared file", 
            !fman.renameFileIfShared(f2, f2));
        assertTrue("should have been able to rename shared file",
             fman.renameFileIfShared(f1, f2));
        files=fman.getSharedFiles(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[0], f3);
        assertEquals("files differ", files[1], f2);
        assertTrue("shouldn't have been able to rename shared file", 
            !fman.renameFileIfShared(f2, new File("C\\garbage.XSADF")));
        files=fman.getSharedFiles(_sharedDir);
        assertEquals("unexpected files length", 1, files.length);
        assertEquals("files differ", files[0], f3);
    }
    
    public void testIgnoreHugeFiles() throws Exception {
        f3 = createNewTestFile(11);   
        waitForLoad();
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        
        //Try to add a huge file.  (It will be ignored.)
        f4=createFakeTestFile(Integer.MAX_VALUE+1l);
        assertTrue("shouldn't have been able to add shared file", 
            !fman.addFileIfShared(f4));
        assertEquals("unexpected number of files", 1, fman.getNumFiles());
        assertEquals("unexpected fman size", 11, fman.getSize());
        //Add really big files.
        f5=createFakeTestFile(Integer.MAX_VALUE-1);
        f6=createFakeTestFile(Integer.MAX_VALUE);
        assertTrue("should have been able to add shared file",
            fman.addFileIfShared(f5));
        assertTrue("should have been able to add shared file",
            fman.addFileIfShared(f6));
        assertEquals("unexpected number of files", 3, fman.getNumFiles());
        assertEquals("unexpected fman size",
            Integer.MAX_VALUE, fman.getSize());
        responses=fman.query(QueryRequest.createQuery("*.*", (byte)3));
        assertEquals("unexpected responses length", 3, responses.length);
        assertEquals("files differ", responses[0].getName(), f3.getName());
        assertEquals("files differ", responses[1].getName(), f5.getName());
        assertEquals("files differ", responses[2].getName(), f6.getName());
    }


    File createNewTestFile(int size) throws Exception {
		File file = File.createTempFile("FileManager_unit_test", 
		    "." + EXTENSION , _sharedDir);
		file.deleteOnExit();
        OutputStream out=new FileOutputStream(file);
        out.write(new byte[size]);
        out.flush();
        out.close();
        //Needed for comparisons between "C:\Progra~1" and "C:\Program Files".			
        return FileManager.getCanonicalFile(file);
    }

    /** Same a createNewTestFile but doesn't actually allocate the requested
     *  number of bytes on disk.  Instead returns a subclass of File. */
    File createFakeTestFile(long size) throws Exception {
        File real=createNewTestFile(1);
        return new HugeFakeFile(real.getParentFile(), real.getName(), size);       
    }

    private static class HugeFakeFile extends File {
        long length;

        public HugeFakeFile(File dir, String name, long length) {
            super(dir, name);
            this.length=length;
        }

        public long length() {
            return length;
        }
    }
    
    private class FManCallback extends ActivityCallbackStub {
        public void fileManagerLoaded() {
            synchronized(loaded) {
                loaded.notify();
            }
        }
    }
    
    private void waitForLoad() {
        synchronized(loaded) {
            try {
                fman.loadSettings(false); // true won't matter either                
                loaded.wait();
            } catch (InterruptedException e) {
                //good.
            }
        }
    }    
}
