package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.SimpleFileManager;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.StringUtils;

public class FileManagerTest extends com.limegroup.gnutella.util.BaseTestCase {

    protected static final String EXTENSION = "XYZ";
    private static final int MAX_LOCATIONS = 10;
    
    private File f1 = null;
    private File f2 = null;
    private File f3 = null;
    private File f4 = null;
    private File f5 = null;
    private File f6 = null;
    //changed to protected so that MetaFileManagerTest can
    //use these variables as well.
    protected FileManager fman = null;
    protected Object loaded = new Object();
    private Response[] responses;
    private FileDesc[] files;

	private File emptyDir = null;
	private File emptyNameDir;
	private File emptyExtensionDir;
	private File recursiveDir;

    public FileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileManagerTest.class);
    }
    
    public static void globalSetUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        try {
            RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }        
    }
    
	public void setUp() throws Exception {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        	    
	    cleanFiles(_sharedDir, false);
	    fman = new SimpleFileManager();
	    PrivilegedAccessor.setValue(RouterService.class, "callback", new FManCallback());
	    
		File tmpFile = File.createTempFile("tmp", "file");
		File tmpDir = tmpFile.getParentFile();
		tmpFile.delete();
		
		emptyDir = new File(tmpDir, "emptydir");
		emptyDir.mkdir();
		emptyDir.deleteOnExit();
		
		File subDir = new File(emptyDir, "subdir");
		subDir.mkdir();
		subDir.deleteOnExit();
		
		emptyNameDir = new File(tmpDir, "emptyname");
		emptyNameDir.mkdir();
		emptyNameDir.deleteOnExit();
		
		File f = new File(emptyNameDir, ".emptyname");
		f.createNewFile();
		f.deleteOnExit();
		
		emptyExtensionDir = new File(tmpDir, "emptyextension");
		emptyExtensionDir.mkdir();
		emptyExtensionDir.deleteOnExit();
		
		f = new File(emptyExtensionDir, "emptyextesion.");
		f.createNewFile();
		f.deleteOnExit();
		
		recursiveDir = new File(tmpDir, "recursivedir");
		recursiveDir.mkdir();
		recursiveDir.deleteOnExit();
		f = new File(recursiveDir, "afile.txt");
		f.createNewFile();
		f.deleteOnExit();
		f = new File(recursiveDir, "subdir");
		f.mkdir();
		f.deleteOnExit();
		f = new File(f, "subfile.txt");
		f.createNewFile();
		f.deleteOnExit();
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
            new File(FileUtils.getParentFile(f1).getCanonicalPath()),
            new File(_sharedDir.getCanonicalPath()));
    }
    
    public void testGetSharedFilesWithNoShared() throws Exception {
        FileDesc[] sharedFiles =  fman.getSharedFileDescriptors(_sharedDir);
        assertNull("should not be sharing any files", sharedFiles);
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
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);        
                
        
        // should not be able to remove unshared file
        assertNull("should have not been able to remove f3", 
				   fman.removeFileIfShared(f3));
				   
        assertEquals("first file should be f1", f1, fman.get(0).getFile());
        
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("unexpected length of shared files", 1, files.length);
        assertEquals("files should be the same", files[0].getFile(), f1);
        files=fman.getSharedFileDescriptors(FileUtils.getParentFile(_sharedDir));
        assertNull("file manager listed shared files in file's parent dir",
            files);
    }
    
    public void testAddingOneSharedFile() throws Exception {
        f1 = createNewTestFile(1);
        waitForLoad();
        f2 = createNewTestFile(3);
        f3 = createNewTestFile(11);

        assertEquals("should not have been able to share file",
                  null, fman.addFileIfShared(new File("C:\\bad.ABCDEF")));
        assertNotEquals("should have been able to share file", 
                  null, fman.addFileIfShared(f2));
        assertEquals("unexpected number of files", 2, fman.getNumFiles());
        assertEquals("unexpected fman size", 4, fman.getSize());
        responses=fman.query(QueryRequest.createQuery("unit", (byte)3));
        assertNotEquals("responses gave same index",
            responses[0].getIndex(), responses[1].getIndex() );
        for (int i=0; i<responses.length; i++) {
            assertTrue("responses should be expected indexes", 
                responses[i].getIndex()==0 || responses[i].getIndex()==1);
        }
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("first shared file is not f1", files[0].getFile(), f1);
        assertEquals("second shared file is not f2", files[1].getFile(), f2);
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
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("unexpected files length", 1, files.length);
        assertEquals("files differ", files[0].getFile(), f1);
    }
    
    public void testAddAnotherSharedFileDifferentIndex() throws Exception {
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        f3 = createNewTestFile(11);
        fman.removeFileIfShared(f2);

        //Add a new second file, with new index.
        assertNotNull("should have been able to add shared file", 
            fman.addFileIfShared(f3));
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

        files = fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[0].getFile(), f1);
        assertEquals("files differ", files[1].getFile(), f3);
        files=fman.getAllSharedFileDescriptors();
        assertEquals("unexpected files length", 2, files.length);
        // we don't know the order the filedescs are returned ...
        if( files[0].getFile().equals(f1) ) {
            assertEquals("files differ", files[0].getFile(), f1);
            assertEquals("files differ", files[1].getFile(), f3);
        } else {
            assertEquals("files differ", files[0].getFile(), f3);
            assertEquals("files differ", files[1].getFile(), f1);
        }            
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
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[0].getFile(), f3);
        assertEquals("files differ", files[1].getFile(), f2);
        assertTrue("shouldn't have been able to rename shared file", 
            !fman.renameFileIfShared(f2, new File("C\\garbage.XSADF")));
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("unexpected files length", 1, files.length);
        assertEquals("files differ", files[0].getFile(), f3);
    }
    
    public void testIgnoreHugeFiles() throws Exception {
        f3 = createNewTestFile(11);   
        waitForLoad();
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        
        //Try to add a huge file.  (It will be ignored.)
        f4=createFakeTestFile(Integer.MAX_VALUE+1l);
        assertEquals("shouldn't have been able to add shared file", 
            null, addFile(f4));
        assertEquals("unexpected number of files", 1, fman.getNumFiles());
        assertEquals("unexpected fman size", 11, fman.getSize());
        //Add really big files.
        f5=createFakeTestFile(Integer.MAX_VALUE-1);
        f6=createFakeTestFile(Integer.MAX_VALUE);
        assertNotEquals("should have been able to add shared file",
            null, addFile(f5));
        assertNotEquals("should have been able to add shared file",
            null, addFile(f6));
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
     * Calls FileManager.addFile directly.  This is necessary so we keep the
     * file object.  Otherwise addFileIfShared will actually call addFile
     * with the canonical file, which is a different object.
     */
    private FileDesc addFile(File f) throws Exception {
        FileDesc fd = (FileDesc)PrivilegedAccessor.invokeMethod(
            fman, "addFile", new Object[] {f}, new Class[] {File.class} );
        return fd;
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
            new File("a"), urns, "a", 0, new VerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        // add another incomplete file with the same hash and same
        // name and make sure it's not added.
        fman.addIncompleteFile(
            new File("a"), urns, "a", 0, new VerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        // add another incomplete file with another hash, it should be added.
        urns = new HashSet();
        urns.add( HugeTestUtils.URNS[1] );
        fman.addIncompleteFile(
            new File("c"), urns, "c", 0, new VerifyingFile(0));

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
            new File("a"), urns, "a", 0, new VerifyingFile(0));
        urns = new HashSet();
        urns.add( HugeTestUtils.URNS[1] );
        fman.addIncompleteFile(
            new File("b"), urns, "b", 0, new VerifyingFile(0));
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
            new File("sambe"), urns, "a", 0, new VerifyingFile(0));
        assertEquals("unexpected shared incomplete",
            1, fman.getNumIncompleteFiles());            
            
        QueryRequest qr = QueryRequest.createQuery(urn, "sambe");
        Response[] hits = fman.query(qr);
        assertNotNull(hits);
        assertEquals("unexpected number of resp.", 0, hits.length);
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
            new File("sambe"), urns, "a", 0, new VerifyingFile(0));
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
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals( f3, files[0].getFile() );
        fd = fman.get(0);
        urn = fd.getSHA1Urn();
        urns = fd.getUrns();
        
        // now add an ifd with those urns.
        fman.addIncompleteFile(
            new File("sam"), urns, "b", 0, new VerifyingFile(0));
        
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
				Response[] hits = fman.query(qr);
				assertEquals("there should only be one response", 1, hits.length);
				assertEquals("responses should be equal", testResponse, hits[0]);		
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
			String name = I18NConvert.instance().getNorm(fd.getName());
			if(name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue())
			    continue;
			QueryRequest qr = QueryRequest.createQuery(name);
			Response[] hits = fman.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
			if ( hits.length != 1 ) continue;
			checked = true;
			assertEquals("responses should be equal", testResponse, hits[0]);
			Set urnSet = hits[0].getUrns();
			URN[] responseUrns = (URN[])urnSet.toArray(new URN[0]);
			// this is just a sanity check
			assertEquals("urns should be equal for " + fd, urn, responseUrns[0]);		
		}
		assertTrue("wasn't able to find any unique classes to check against.", checked);
	}
	
	/**
	 * Tests that alternate locations are returned in responses.
	 */
	public void testThatAlternateLocationsAreReturned() throws Exception {
	    addFilesToLibrary();
	    addAlternateLocationsToFiles();

        boolean checked = false;
		for(int i = 0; i < fman.getNumFiles(); i++) {
			FileDesc fd = fman.get(i);
			Response testResponse = new Response(fd);
			String name = I18NConvert.instance().getNorm(fd.getName());
			if(name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue())
			    continue;
			QueryRequest qr = QueryRequest.createQuery(name);
			Response[] hits = fman.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
			if ( hits.length != 1 ) continue;
			checked = true;
			assertEquals("responses should be equal", testResponse, hits[0]);
			assertEquals("should have 10 other alts",
			    10, testResponse.getLocations().size());
			assertEquals("should have equal alts",
			    testResponse.getLocations(), hits[0].getLocations());
		}
		assertTrue("wasn't able to find any unique classes to check against.", checked);
    }	
    
    /**
     * tests for the QRP thats kept by the FileManager 
     * tests that the function getQRP of FileManager returns
     * the correct table after addition, removal, and renaming
     * of shared files.
     */
    public void testFileManagerQRP() throws Exception {
        f1 = createNewNamedTestFile(10, "hello");
        f2 = createNewNamedTestFile(10, "\u5bae\u672c\u6b66\u8535\u69d8");
        f3 = createNewNamedTestFile(10, "\u00e2cc\u00e8nts");
        waitForLoad(); 

        //get the QRT from the filemanager
        QueryRouteTable qrt = fman.getQRT();

        //test that QRT doesn't contain random keyword
        QueryRequest qr = QueryRequest.createQuery("asdfasdf");
        assertFalse("query should not be in qrt",
                   qrt.contains(qr));

        //test that the qrt contains the three files 
        qr = get_qr(f1);
        assertTrue("query not in QRT", qrt.contains(qr));
        
        qr = get_qr(f2);
        assertTrue("query not in QRT", qrt.contains(qr));

        qr = get_qr(f3);
        assertTrue("query not in QRT", qrt.contains(qr));


        //now remove one of the files
        fman.removeFileIfShared(f3);
        
        qrt = fman.getQRT();
        
        //make sure the removed file is no longer in qrt
        assertFalse("query should not be in qrt",
                   qrt.contains(qr));
        
        //just check that the one of the other files is still 
        //in the qrt
        qr = get_qr(f2);
        assertTrue("query not in QRT", qrt.contains(qr));

        

        //test rename
        f4 = createNewNamedTestFile(10, "miyamoto_musashi_desu");
        
        //check that this file doesn't hit
        qr = get_qr(f4);
        assertFalse("query should not be in qrt", qrt.contains(qr));

        //now rename one of the files
        fman.renameFileIfShared(f2, f4);
        qrt = fman.getQRT();
        
        //check hit with new name
        qr = get_qr(f4);
        assertTrue("query not in qrt", qrt.contains(qr));
        
        //check old name
        qr = get_qr(f2);
        assertFalse("query should not be in qrt", qrt.contains(qr));
    }
    
    /**
     * Tests whether specially shared files are indeed shared.  Also
     * tests that another file in the same directory as a specially
     * shared file is not shared.   
     */
    public void testSpecialSharing() throws Exception {
        //  create "shared" and "notShared" out of shared directory
        File shared    = createNewNamedTestFile(10, "shared", _sharedDir.getParentFile());
        File notShared = createNewNamedTestFile(10, "notShared", _sharedDir.getParentFile());

        //  Add "shared" to special shared files
        SharingSettings.SPECIAL_FILES_TO_SHARE.add(shared);
        waitForLoad();

        //  assert that "shared" and "notShared" are not in a shared directory
        assertFalse("shared should be specially shared, not shared in a shared directory", fman.isFileInCompletelySharedDirectory(shared));
        assertFalse("notShared should not be shared in a shared directory", fman.isFileInCompletelySharedDirectory(notShared));
        
        //  assert that "shared" is shared
        FileDesc[] sharedFiles = fman.getAllSharedFileDescriptors();
        assertNotNull("no shared files, even though just added a specially shared file", sharedFiles);
        boolean found = false;
        for(int i = 0; i < sharedFiles.length; i++) {
            FileDesc fd = sharedFiles[i];
            if(fd == null) continue;
            if(fd.getFile().equals(shared)) {
                found = true;
            }
        }
        assertTrue("specially shared file not found in list of shared files", found);
        
        //  assert that "notShared" is not shared
        found = false;
        for(int i = 0; i < sharedFiles.length; i++) {
            FileDesc fd = sharedFiles[i];
            if(fd == null) continue;
            if(fd.getFile().equals(notShared)) {
                found = true;
            }
        }
        assertFalse("non-shared file found in list of shared files", found);
    }

    /**
     * Tests whether specially shared files are indeed shared despite having
     * an extension that is not on the list of shareable extensions.
     */
    public void testSpecialSharingWithNonShareableExtension() throws Exception {
        //  create "shared" file out of shared directory
        File tmp = createNewNamedTestFile(10, "tmp", _sharedDir.getParentFile());
        File shared = new File(tmp.getParentFile(), "shared.badextension");
        tmp.renameTo(shared);
                
        //  Add "shared" to special shared files
        SharingSettings.SPECIAL_FILES_TO_SHARE.add(shared);
        waitForLoad();

        //  assert that "shared" file does not have a shareable extension
        Method extensionCheck =
            PrivilegedAccessor.getMethod(FileManager.class,  
                "hasShareableExtension", new Class[] { File.class });
        Object[] params = new File[] { shared };
        boolean valid = 
            ((Boolean)extensionCheck.invoke(FileManager.class, 
                params)).booleanValue();
        assertFalse("shared file should not have a shareable extension", valid);
        
        //  assert that "shared" is not in shared directories
        assertFalse("shared should be specially shared, not shared in a shared directory", fman.isFileInCompletelySharedDirectory(shared));
        
        //  assert that "shared" is shared
        FileDesc[] sharedFiles = fman.getAllSharedFileDescriptors();
        assertNotNull("no shared files, even though just added a specially shared file", sharedFiles);
        boolean found = false;
        for(int i = 0; i < sharedFiles.length; i++) {
            FileDesc fd = sharedFiles[i];
            if(fd == null) continue;
            if(fd.getFile().equals(shared)) {
                found = true;
            }
        }
        assertTrue("specially shared file not found in list of shared files", found);
    }

	public void testGetFilesRecursive() throws Exception {
	
		File[] files = FileUtils.getFilesRecursive(emptyDir, null);
		assertEquals("directory should have no files, only a subdir", 0, files.length);
		
		files = FileUtils.getFilesRecursive(emptyNameDir, null);
		assertEquals("directory should have 1 hidden file", 1, files.length);
		
		files = FileUtils.getFilesRecursive(emptyNameDir, new String[] {
				"emptyname"});
		assertEquals("directory should have no file matching extension \"emptyname\"",
				0, files.length);
		
		files = FileUtils.getFilesRecursive(emptyExtensionDir, null);
		assertEquals("directory should have one file", 1, files.length);
		
		files = FileUtils.getFilesRecursive(emptyExtensionDir, 
				new String[] { "" });
		assertEquals("directory should have no file matching empty extension", 
				0, files.length);
		
		// test if files in subdirectories are found too
		files = FileUtils.getFilesRecursive(recursiveDir, null);
		assertEquals("wrong number of files found", 2, files.length);
		
		// test if files in subdirectories are found with filter
		files = FileUtils.getFilesRecursive(recursiveDir, new String[] {
				"unmatchedextension", "", "txt"});
		assertEquals("wrong number of matching files found", 2, files.length);
	}
	
    /**
     * Tests whether a directory placed on the non-recursive share list
     * successfully does not share files in its subdirectories. 
     */
    public void testNonRecursiveSharing() throws Exception {
        //  add file "shared" in shared directory
        File shared = createNewNamedTestFile(10, "shared");
        waitForLoad();
        
        //  assert that "shared" is in a shared directory
        assertTrue("shared should be in a shared directory", fman.isFileInCompletelySharedDirectory(shared));

        //  make sure "shared" is shared
        FileDesc[] sharedFiles = fman.getAllSharedFileDescriptors();
        assertNotNull("no shared files, even though just added a shared file", sharedFiles);
        boolean found = false;
        for(int i = 0; i < sharedFiles.length; i++) {
            FileDesc fd = sharedFiles[i];
            if(fd == null) continue;
            if(fd.getFile().equals(shared)) {
                found = true;
            }
        }
        assertTrue("shared file not found in list of shared files", found);
        
        //  make new subdirectory in shared directory
        File subDir = new File(shared.getParent(), "noShare");
        assertTrue("subdirectory \"noShare\" could not be created", subDir.mkdirs());
        
        //  mark shared directory so that it's shared non-recursively
        SharingSettings.DIRECTORIES_TO_SHARE_NON_RECURSIVELY.add(subDir);

        //  add "notShared" to subdirectory
        File notShared = createNewNamedTestFile(10, "notShared", subDir);
        waitForLoad();
        
        //  make sure "notShared" is not shared
        found = false;
        for(int i = 0; i < sharedFiles.length; i++) {
            FileDesc fd = sharedFiles[i];
            if(fd == null) continue;
            if(fd.getFile().equals(notShared)) {
                found = true;
            }
        }
        assertFalse("file not intended to be shared found in list of shared files", found);
    }
    
    /**
     * Tests whether the FileManager.isSensitiveDirectory(File) function is functioning properly. 
     */
    public void testSensitiveDirectoryPredicate() throws Exception {
        //  check defensive programming
        File file = null;
        assertFalse("null directory should not be a sensitive directory", FileManager.isSensitiveDirectory(file));
        file = new File("lksfjlsakjfsldfjak.slfkjs");
        assertFalse("random file should not be a sensitive directory", FileManager.isSensitiveDirectory(file));
        
        //  check that system roots are sensitive directories
        File[] faRoots = File.listRoots();
        if(faRoots != null && faRoots.length > 0) {
            for(int i = 0; i < faRoots.length; i++) {
                assertTrue("root directory "+faRoots[i]+ " should be a sensitive directory", FileManager.isSensitiveDirectory(faRoots[i]));
            }
        }
        
        //  check that the user's home dir is a sensitive directory
        String userHome = System.getProperty("user.dir");
        assertTrue("user's home directory should be a sensitive directory", FileManager.isSensitiveDirectory(new File(userHome)));
        
        //  check for OS-specific directories:
        String realOS = System.getProperty("os.name");
        setOSName("Windows");
        if(CommonUtils.isWindows()) {
            //  check for "Documents and Settings"
            assertTrue("Documents and Settings should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Documents and Settings")));
            
            //  check for "My Documents"
            assertTrue("My Documents should be a sensitive directory", FileManager.isSensitiveDirectory(new File(userHome, "My Documents")));
            
            //  check for "Desktop"
            assertTrue("Desktop should be a sensitive directory", FileManager.isSensitiveDirectory(new File(userHome, "Desktop")));
            
            //  check for "Program Files"
            assertTrue("Program Files should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Program Files")));
            
            //  check for "Windows"
            assertTrue("Windows should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Windows")));
            
            //  check for "WINNT"
            assertTrue("Windows should be a sensitive directory", FileManager.isSensitiveDirectory(new File("WINNT")));
        }
        
        setOSName("Mac OS X");
        if(CommonUtils.isMacOSX()) {
            //  check for /Users
            assertTrue("Users should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Users")));
            
            //  check for /System
            assertTrue("System should be a sensitive directory", FileManager.isSensitiveDirectory(new File("System")));
            
            //  check for /System Folder
            assertTrue("System Folder should be a sensitive directory", FileManager.isSensitiveDirectory(new File("System Folder")));
            
            //  check for /Previous Systems
            assertTrue("Previous Systems should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Previous Systems")));
            
            //  check for /private
            assertTrue("private should be a sensitive directory", FileManager.isSensitiveDirectory(new File("private")));
            
            //  check for /Volumes
            assertTrue("Volumes should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Volumes")));
            
            //  check for /Desktop
            assertTrue("Desktop should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Desktop")));
            
            //  check for /Applications
            assertTrue("Applications should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Applications")));
            
            //  check for /Applications (Mac OS 9)
            assertTrue("Applications (Mac OS 9) should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Applications (Mac OS 9)")));
            
            //  check for /Network
            assertTrue("Network should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Network")));
        }
        
        setOSName("Linux");
        if(CommonUtils.isPOSIX()) {
            //  check for /bin
            assertTrue("bin should be a sensitive directory", FileManager.isSensitiveDirectory(new File("bin")));
            
            //  check for /boot
            assertTrue("boot should be a sensitive directory", FileManager.isSensitiveDirectory(new File("boot")));
            
            //  check for /dev
            assertTrue("dev should be a sensitive directory", FileManager.isSensitiveDirectory(new File("dev")));
            
            //  check for /etc
            assertTrue("etc should be a sensitive directory", FileManager.isSensitiveDirectory(new File("etc")));
            
            //  check for /home
            assertTrue("home should be a sensitive directory", FileManager.isSensitiveDirectory(new File("home")));
            
            //  check for /mnt
            assertTrue("mnt should be a sensitive directory", FileManager.isSensitiveDirectory(new File("mnt")));
            
            //  check for /opt
            assertTrue("opt should be a sensitive directory", FileManager.isSensitiveDirectory(new File("opt")));
            
            //  check for /proc
            assertTrue("proc should be a sensitive directory", FileManager.isSensitiveDirectory(new File("proc")));
            
            //  check for /root
            assertTrue("root should be a sensitive directory", FileManager.isSensitiveDirectory(new File("root")));
            
            //  check for /sbin
            assertTrue("sbin should be a sensitive directory", FileManager.isSensitiveDirectory(new File("sbin")));
            
            //  check for /usr
            assertTrue("usr should be a sensitive directory", FileManager.isSensitiveDirectory(new File("usr")));
            
            //  check for /var
            assertTrue("var should be a sensitive directory", FileManager.isSensitiveDirectory(new File("var")));
        }
        
        //  revert the os.name system property back to normal 
        setOSName(realOS);
    }
    
    /**
     * Helper function to set the operating system so that multiple OSs can be partially-checked
     * by testing on one platform.
     */
    private static void setOSName(String name) throws Exception {
        System.setProperty("os.name", name);
        PrivilegedAccessor.invokeMethod(CommonUtils.class, "setOperatingSystems", null);
    }
    
    //helper function to create queryrequest with I18N
    private QueryRequest get_qr(File f) {
        String norm = I18NConvert.instance().getNorm(f.getName());
        norm = StringUtils.replace(norm, "_", " ");
        return QueryRequest.createQuery(norm);
    }

    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name, in the default shared directory.
     */
    protected File createNewNamedTestFile(int size, String name) 
        throws Exception {
        return createNewNamedTestFile(size, name, _sharedDir);
    }

    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name, in the given directory.
     */
    protected File createNewNamedTestFile(int size, String name, File directory) throws Exception {
		File file = File.createTempFile(name, "." + EXTENSION, directory);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);
        out.write(new byte[size]);
        out.flush();
        out.close();
        //Needed for comparisons between "C:\Progra~1" and "C:\Program Files".
        return FileUtils.getCanonicalFile(file);
    }
	
	private void addFilesToLibrary() throws Exception {
		String dirString = "com/limegroup/gnutella";
		File testDir = CommonUtils.getResourceFile(dirString);
		testDir = testDir.getCanonicalFile();
		assertTrue("could not find the gnutella directory",
		    testDir.isDirectory());
		
        File[] testFiles = testDir.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                // use files with a $ because they'll generally
                // trigger a single-response return, which is
                // easier to check
                return !file.isDirectory() && file.getName().indexOf("$")!=-1;
            }
        });
		assertNotNull("no files to test against", testFiles);
		assertNotEquals("no files to test against", 0, testFiles.length);

        waitForLoad();

   		for(int i=0; i<testFiles.length; i++) {
			if(!testFiles[i].isFile()) continue;
			File shared = new File(
			    _sharedDir, testFiles[i].getName() + "." + EXTENSION);
			assertTrue("unable to get file",
			    CommonUtils.copy( testFiles[i], shared));
            assertNotEquals(null, fman.addFileIfShared(shared));
		}
        
        // the below test depends on the filemanager loading shared files in 
        // alphabetical order, and listFiles returning them in alphabetical
        // order since neither of these must be true, a length check can
        // suffice instead.
        //for(int i=0; i<files.length; i++)
        //    assertEquals(files[i].getName()+".tmp", 
        //                 fman.get(i).getFile().getName());
            
        assertEquals("unexpected number of shared files",
            testFiles.length, fman.getNumFiles() );
    }
    
    private void addAlternateLocationsToFiles() throws Exception {
        FileDesc[] fds = fman.getAllSharedFileDescriptors();
        for(int i = 0; i < fds.length; i++) {
            String urn = fds[i].getSHA1Urn().httpStringValue();
            for(int j = 0; j < MAX_LOCATIONS + 5; j++) {
                String loc = "http://1.2.3." + j + ":6346/uri-res/N2R?" + urn;
                fds[i].add(AlternateLocation.create(loc));
            }
        }
    }

    protected File createNewTestFile(int size) throws Exception {
		File file = File.createTempFile("FileManager_unit_test", 
		    "." + EXTENSION , _sharedDir);
		file.deleteOnExit();
        OutputStream out=new FileOutputStream(file);
        out.write(new byte[size]);
        out.flush();
        out.close();
        //Needed for comparisons between "C:\Progra~1" and "C:\Program Files".			
        return FileUtils.getCanonicalFile(file);
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
    
    public class FManCallback extends ActivityCallbackStub {
        public void fileManagerLoaded() {
            synchronized(loaded) {
                loaded.notify();
            }
        }
    }

    protected void waitForLoad() {
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

