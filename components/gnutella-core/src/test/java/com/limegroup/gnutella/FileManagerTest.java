package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.messages.*;
import junit.framework.*;

import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.stubs.SimpleFileManager;
import com.sun.java.util.collections.*;
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
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        	    
	    cleanFiles(_sharedDir, false);
	    fman = new SimpleFileManager();
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
            
        // it is important to check the query at all bounds,
        // including tests for case.
        responses=fman.query(QueryRequest.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=fman.query(QueryRequest.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=fman.query(QueryRequest.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=fman.query(QueryRequest.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=fman.query(QueryRequest.createQuery(
            "FileManager_UNIT_tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);        
                
        
        // should not be able to remove unshared file
        assertNull("should have not been able to remove f3", 
				   fman.removeFileIfShared(f3));
				   
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

        assertEquals("should not have been able to share file",
                  -1, fman.addFileIfShared(new File("C:\\bad.ABCDEF")));
        assertNotEquals("should have been able to share file", 
                  -1, fman.addFileIfShared(f2));
        assertEquals("unexpected number of files", 2, fman.getNumFiles());
        assertEquals("unexpected fman size", 4, fman.getSize());
        responses=fman.query(QueryRequest.createQuery("unit", (byte)3));
        assertNotEquals("responses gave same index",
            responses[0].getIndex(), responses[1].getIndex() );
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
        assertNull("shouldn't have been able to remove unshared file", 
            fman.removeFileIfShared(f3));
        assertNotNull("should have been able to remove shared file", 
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
        assertNotEquals("should have been able to add shared file", 
            -1, fman.addFileIfShared(f3));
        assertEquals("unexpected file size", 12, fman.getSize());
        assertEquals("unexpedted number of files", 2, fman.getNumFiles());
        responses=fman.query(QueryRequest.createQuery("unit", (byte)3));
        assertEquals("unexpected response length", 2, responses.length);
        assertNotEquals("unexpected response[0] index",
            1, responses[0].getIndex());
        assertNotEquals("unexpected response[1] index",
            1, responses[1].getIndex());
        fman.get(0);
        fman.get(2);
        assertNull("should be null (unshared)", fman.get(1));
        try {
            fman.get(3);
            fail("should not have gotten anything");
        } catch (IndexOutOfBoundsException e) { }
        
        assertFalse("should not be valid", fman.isValidIndex(3));
        assertTrue("should be valid", fman.isValidIndex(0));
        assertTrue("should be valid (was at one time)", fman.isValidIndex(1));

        responses=fman.query(QueryRequest.createQuery("*unit*", (byte)3));
        assertEquals("unexpected responses length", 2, responses.length);

        files=fman.getSharedFiles(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[0], f1);
        assertEquals("files differ", files[1], f3);
        files=fman.getAllSharedFiles();
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[1], f1);
        assertEquals("files differ", files[0], f3);
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
        assertEquals("shouldn't have been able to add shared file", 
            -1, fman.addFileIfShared(f4));
        assertEquals("unexpected number of files", 1, fman.getNumFiles());
        assertEquals("unexpected fman size", 11, fman.getSize());
        //Add really big files.
        f5=createFakeTestFile(Integer.MAX_VALUE-1);
        f6=createFakeTestFile(Integer.MAX_VALUE);
        assertNotEquals("should have been able to add shared file",
            -1, fman.addFileIfShared(f5));
        assertNotEquals("should have been able to add shared file",
            -1, fman.addFileIfShared(f6));
        assertEquals("unexpected number of files", 3, fman.getNumFiles());
        assertEquals("unexpected fman size",
            Integer.MAX_VALUE, fman.getSize());
        responses=fman.query(QueryRequest.createQuery("*.*", (byte)3));
        assertEquals("unexpected responses length", 3, responses.length);
        assertEquals("files differ", responses[0].getName(), f3.getName());
        assertEquals("files differ", responses[1].getName(), f5.getName());
        assertEquals("files differ", responses[2].getName(), f6.getName());
    }
    
    /**
     * Tests adding incomplete files to the FileManager.
     */
    public void testAddIncompleteFile() throws Exception {    
        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());    
        
        // add one incomplete file and make sure the numbers go up.
        Set urns = new HashSet();
        urns.add( HugeTestUtils.URNS[0] );
        fman.addIncompleteFile(
            new File("a"), urns, "a", 0, new VerifyingFile(false));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        // add another incomplete file with the same hash and same
        // name and make sure it's not added.
        fman.addIncompleteFile(
            new File("a"), urns, "a", 0, new VerifyingFile(false));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        // add another incomplete file with another hash, it should be added.
        urns = new HashSet();
        urns.add( HugeTestUtils.URNS[1] );
        fman.addIncompleteFile(
            new File("c"), urns, "c", 0, new VerifyingFile(false));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            2, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
    }
    
    /**
     * Tests the removeFileIfShared for incomplete files.
     */
    public void testRemovingIncompleteFiles() {
        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        Set urns = new HashSet();
        urns.add( HugeTestUtils.URNS[0] );
        fman.addIncompleteFile(
            new File("a"), urns, "a", 0, new VerifyingFile(false));
        urns = new HashSet();
        urns.add( HugeTestUtils.URNS[1] );
        fman.addIncompleteFile(
            new File("b"), urns, "b", 0, new VerifyingFile(false));        
        assertEquals("unexpected shared incomplete",
            2, fman.getNumIncompleteFiles());
            
        fman.removeFileIfShared( new File("a") );
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());
        
        fman.removeFileIfShared( new File("c") );
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());
        
        fman.removeFileIfShared( new File("b") );
        assertEquals("unexpected shared incomplete",
            0, fman.getNumIncompleteFiles());
    }
    
    /**
     * Tests that responses are not returned for IncompleteFiles.
     */
    public void testQueryRequestsDoNotReturnIncompleteFiles() {
        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        Set urns = new HashSet();
        URN urn = HugeTestUtils.URNS[0];
        urns.add( urn );
        fman.addIncompleteFile(
            new File("sambe"), urns, "a", 0, new VerifyingFile(false));
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());            
            
        QueryRequest qr = QueryRequest.createQuery(urn, "sambe");
        Response[] responses = fman.query(qr);
        assertNotNull(responses);
        assertEquals("unexpected number of resp.", 0, responses.length);
    }
    
    /**
     * Tests that IncompleteFileDescs are returned for FileDescForUrn only
     * if there are no complete files.
     */
    public void testGetFileDescForUrn() throws Exception {
        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        Set urns = new HashSet();
        URN urn = HugeTestUtils.URNS[0];
        urns.add( urn );
        fman.addIncompleteFile(
            new File("sambe"), urns, "a", 0, new VerifyingFile(false));
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());
            
        // First test that we DO get this IFD.
        FileDesc fd = fman.getFileDescForUrn(urn);
        assertEquals( urns, fd.getUrns() );
        
        // add a file to the library and load it up.
        f3 = createNewTestFile(11);   
        waitForLoad();
        assertEquals("unexected shared files", 1, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());        
        
        // ensure it got shared.
        files=fman.getSharedFiles(_sharedDir);
        assertEquals( f3, files[0] );
        fd = fman.get(0);
        urn = fd.getSHA1Urn();
        urns = fd.getUrns();
        
        // now add an ifd with those urns.
        fman.addIncompleteFile(
            new File("sam"), urns, "b", 0, new VerifyingFile(false));
        
        FileDesc retFD = fman.getFileDescForUrn(urn);    
        assertNotNull(retFD);
        assertNotInstanceof(IncompleteFileDesc.class, retFD);
        assertEquals(retFD, fd);
    }
        
        
	/**
	 * Tests URN requests on the FileManager.
	 */
	public void testUrnRequests() throws Exception {
	    addFilesToLibrary();

		for(int i = 0; i < fman.getNumFiles(); i++) {
			FileDesc fd = fman.get(i);
			Response testResponse = new Response(fd);
			URN urn = fd.getSHA1Urn();
			assertEquals("FileDescs should match", fd, 
						 fman.getFileDescForUrn(urn));
			
			// first set does not include any requested types
			// third includes both
			Set requestedUrnSet0 = new HashSet();
			Set requestedUrnSet1 = new HashSet();
			Set requestedUrnSet2 = new HashSet();
			Set requestedUrnSet3 = new HashSet();
			requestedUrnSet1.add(UrnType.ANY_TYPE);
			requestedUrnSet2.add(UrnType.SHA1);
			requestedUrnSet3.add(UrnType.ANY_TYPE);
			requestedUrnSet3.add(UrnType.SHA1);
			Set[] requestedUrnSets = {requestedUrnSet0, requestedUrnSet1, 
									  requestedUrnSet2, requestedUrnSet3};
			Set queryUrnSet = new HashSet();
			queryUrnSet.add(urn);
			for(int j = 0; j < requestedUrnSets.length; j++) {
				QueryRequest qr = QueryRequest.createQuery(
                                requestedUrnSets[j], queryUrnSet);
				Response[] responses = fman.query(qr);
				assertEquals("there should only be one response", 1, responses.length);
				assertEquals("responses should be equal", testResponse, responses[0]);		
			}
		}
	}

	/**
	 * Tests sending request that do not explicitly request any URNs -- traditional
	 * requests -- to make sure that they do return URNs in their responses.
	 */
	public void testThatUrnsAreReturnedWhenNotRequested() throws Exception {
	    addFilesToLibrary();
	    
	    boolean checked = false;
		for(int i = 0; i < fman.getNumFiles(); i++) {
			FileDesc fd = fman.get(i);
			Response testResponse = new Response(fd);
			URN urn = fd.getSHA1Urn();
			QueryRequest qr = QueryRequest.createQuery(fd.getName());
			Response[] responses = fman.query(qr);
			assertNotNull("didn't get a response for query " + qr, responses);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
			if ( responses.length != 1 ) continue;
			checked = true;
			assertEquals("responses should be equal", testResponse, responses[0]);
			Set urnSet = responses[0].getUrns();
			URN[] responseUrns = (URN[])urnSet.toArray(new URN[0]);
			// this is just a sanity check
			assertEquals("urns should be equal for " + fd, urn, responseUrns[0]);		
		}
		assertTrue("wasn't able to find any unique classes to check against.", checked);
	}
	
	private void addFilesToLibrary() throws Exception {
		String dirString = "com/limegroup/gnutella";
		File testDir = CommonUtils.getResourceFile(dirString);
		testDir = testDir.getCanonicalFile();
		assertTrue("could not find the gnutella directory",
		    testDir.isDirectory());
		
        File[] files = testDir.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                // use files with a $ because they'll generally
                // trigger a single-response return, which is
                // easier to check
                return !file.isDirectory() && file.getName().indexOf("$")!=-1;
            }
        });
		assertNotNull("no files to test against", files);
		assertNotEquals("no files to test against", 0, files.length);

        waitForLoad();

   		for(int i=0; i<files.length; i++) {
			if(!files[i].isFile()) continue;
			File shared = new File(
			    _sharedDir, files[i].getName() + "." + EXTENSION);
			assertTrue("unable to get file",
			    CommonUtils.copy( files[i], shared));
            assertNotEquals(-1, fman.addFileIfShared(shared));
		}
        
        
        // the below test depends on the filemanager loading shared files in 
        // alphabetical order, and listFiles returning them in alphabetical
        // order since neither of these must be true, a length check can
        // suffice instead.
        //for(int i=0; i<files.length; i++)
        //    assertEquals(files[i].getName()+".tmp", 
        //                 fman.get(i).getFile().getName());
            
        assertEquals("unexpected number of shared files",
            files.length, fman.getNumFiles() );
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
