 package com.limegroup.gnutella;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.StubContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.library.LibraryData;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.SimpleFileManager;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class FileManagerTest extends LimeTestCase {

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
    protected volatile FileManager fman = null;
    protected Object loaded = new Object();
    private Response[] responses;
    private FileDesc[] files;

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
		
        // ensure each test gets a brand new content manager.
        PrivilegedAccessor.setValue(RouterService.class, "contentManager", new ContentManager());
        LimeTestUtils.setActivityCallBack(new ActivityCallbackStub());
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
            new File(f1.getParentFile().getCanonicalPath()),
            new File(_sharedDir.getCanonicalPath()));
    }
    
    public void testGetSharedFilesWithNoShared() throws Exception {
        FileDesc[] sharedFiles =  fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("should not be sharing any files " + Arrays.asList(sharedFiles), 
				0, sharedFiles.length);
    }
    
    public void testSharingWithContentManager() throws Exception {
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        f3 = createNewTestFile(11);
        f4 = createNewTestFile(23);
        
        URN u1 = getURN(f1);
        URN u2 = getURN(f2);
        URN u3 = getURN(f3);
        
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);        
        ContentManager cm = RouterService.getContentManager();
        cm.initialize();
        // request the urn so we can use the response.
        cm.request(u1, new StubContentResponseObserver(), 1000);
        cm.handleContentResponse(new ContentResponse(u1, false));
        
        waitForLoad();
        assertEquals("unexpected # of shared files", 3, fman.getNumFiles());
        assertEquals("unexpected size of shared files", 37, fman.getSize());
        assertFalse("shouldn't be shared", fman.isFileShared(f1));
        assertTrue("should be shared", fman.isFileShared(f2));
        assertTrue("should be shared", fman.isFileShared(f3));
        assertTrue("should be shared", fman.isFileShared(f4));
        
        FileDesc fd2 = fman.getFileDescForFile(f2);
        FileDesc fd3 = fman.getFileDescForFile(f3);
        FileDesc fd4 = fman.getFileDescForFile(f4);
        
        // test invalid content response.
        fman.validate(fd2);
        cm.handleContentResponse(new ContentResponse(u2, false));
        assertFalse("shouldn't be shared anymore", fman.isFileShared(f2));
        assertEquals("wrong # shared files", 2, fman.getNumFiles());
        assertEquals("wrong shared file size", 34, fman.getSize());
        
        // test valid content response.
        fman.validate(fd3);
        cm.handleContentResponse(new ContentResponse(u3, true));
        assertTrue("should still be shared", fman.isFileShared(f3));
        assertEquals("wrong # shared files", 2, fman.getNumFiles());
        assertEquals("wrong shared file size", 34, fman.getSize());

        // test valid content response.
        fman.validate(fd4);
        Thread.sleep(10000);
        assertTrue("should still be shared", fman.isFileShared(f4));
        assertEquals("wrong # shared files", 2, fman.getNumFiles());
        assertEquals("wrong shared file size", 34, fman.getSize());
        
        // Make sure adding a new file to be shared doesn't work if it
        // returned bad before.
        fman.addFileIfShared(f1);
        assertFalse("shouldn't be shared", fman.isFileShared(f1));
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
        files=fman.getSharedFileDescriptors(_sharedDir.getParentFile());
        assertEquals("file manager listed shared files in file's parent dir",
            0, files.length);
    }
    
    public void testAddingOneSharedFile() throws Exception {
        f1 = createNewTestFile(1);
        waitForLoad();
        f2 = createNewTestFile(3);
        f3 = createNewTestFile(11);

        FileManagerEvent result = addIfShared(new File("C:\\bad.ABCDEF"));
        assertTrue(result.toString(), result.isFailedEvent());
        
        result = addIfShared(f2);
        assertTrue(result.toString(), result.isAddEvent());
        assertNotNull(result.getFileDescs()[0]);
        
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
        assertEquals(2, fman.getNumFiles());     
        assertNull("shouldn't have been able to remove unshared file",  fman.removeFileIfShared(f3));
        assertNotNull("should have been able to remove shared file", fman.removeFileIfShared(f2));
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
        FileManagerEvent result = addIfShared(f3);
        assertTrue(result.toString(), result.isAddEvent());
        assertNotNull(result.getFileDescs()[0]);
        assertEquals("unexpected file size", 12, fman.getSize());
        assertEquals("unexpedted number of files", 2, fman.getNumFiles());
        responses=fman.query(QueryRequest.createQuery("unit", (byte)3));
        assertEquals("unexpected response length", 2, responses.length);
        assertNotEquals("unexpected response[0] index", 1, responses[0].getIndex());
        assertNotEquals("unexpected response[1] index", 1, responses[1].getIndex());
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
        addIfShared(f3);

        FileManagerEvent result = renameIfShared(f2, new File("c:\\asdfoih"));
        assertTrue(result.toString(), result.isFailedEvent());
        assertEquals(f2, result.getFiles()[0]);
        
        result = renameIfShared(f1, f2);
        assertTrue(result.toString(), result.isRenameEvent());
        assertEquals(f1, result.getFileDescs()[0].getFile());
        assertEquals(f2, result.getFileDescs()[1].getFile());
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals("unexpected files length", 2, files.length);
        assertEquals("files differ", files[0].getFile(), f3);
        assertEquals("files differ", files[1].getFile(), f2);
        
        result = renameIfShared(f2, new File("C\\garbage.XSADF"));
        assertTrue(result.toString(), result.isRemoveEvent());
        assertEquals(f2, result.getFileDescs()[0].getFile());
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
        f4 = createFakeTestFile(MAX_FILE_SIZE+1l);
        FileManagerEvent result = addIfShared(f4);
        assertTrue(result.toString(), result.isFailedEvent());
        assertEquals(f4, result.getFiles()[0]);
        assertEquals("unexpected number of files", 1, fman.getNumFiles());
        assertEquals("unexpected fman size", 11, fman.getSize());
        //Add really big files.
        f5=createFakeTestFile(MAX_FILE_SIZE-1);
        f6=createFakeTestFile(MAX_FILE_SIZE);
        result = addIfShared(f5);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(f5, result.getFileDescs()[0].getFile());
        result = addIfShared(f6);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(f6, result.getFileDescs()[0].getFile());
        assertEquals("unexpected number of files", 3, fman.getNumFiles());
        assertEquals("unexpected fman size", Integer.MAX_VALUE, fman.getSize());
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
        assertEquals("unexpected shared incomplete", 0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());    
        
        // add one incomplete file and make sure the numbers go up.
        Set<URN> urns = new UrnSet();
        urns.add( HugeTestUtils.URNS[0] );
        fman.addIncompleteFile(new File("a"), urns, "a", 0, new VerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete", 1, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        // add another incomplete file with the same hash and same
        // name and make sure it's not added.
        fman.addIncompleteFile(new File("a"), urns, "a", 0, new VerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete", 1, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        // add another incomplete file with another hash, it should be added.
        urns = new UrnSet();
        urns.add( HugeTestUtils.URNS[1] );
        fman.addIncompleteFile(new File("c"), urns, "c", 0, new VerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete", 2, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
    }
    
    /**
     * Tests the removeFileIfShared for incomplete files.
     */
    public void testRemovingIncompleteFiles() {
        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        Set<URN> urns = new UrnSet();
        urns.add( HugeTestUtils.URNS[0] );
        fman.addIncompleteFile(new File("a"), urns, "a", 0, new VerifyingFile(0));
        urns = new UrnSet();
        urns.add( HugeTestUtils.URNS[1] );
        fman.addIncompleteFile(new File("b"), urns, "b", 0, new VerifyingFile(0));
        assertEquals("unexpected shared incomplete", 2, fman.getNumIncompleteFiles());
            
        fman.removeFileIfShared( new File("a") );
        assertEquals("unexpected shared incomplete", 1, fman.getNumIncompleteFiles());
        
        fman.removeFileIfShared( new File("c") );
        assertEquals("unexpected shared incomplete", 1, fman.getNumIncompleteFiles());
        
        fman.removeFileIfShared( new File("b") );
        assertEquals("unexpected shared incomplete", 0, fman.getNumIncompleteFiles());
    }
    
    /**
     * Tests that responses are not returned for IncompleteFiles.
     */
    public void testQueryRequestsDoNotReturnIncompleteFiles() {
        assertEquals("unexected shared files", 0, fman.getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        Set<URN> urns = new UrnSet();
        URN urn = HugeTestUtils.URNS[0];
        urns.add( urn );
        fman.addIncompleteFile(new File("sambe"), urns, "a", 0, new VerifyingFile(0));
        assertEquals("unexpected shared incomplete", 1, fman.getNumIncompleteFiles());            
            
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
            
        Set<URN> urns = new UrnSet();
        URN urn = HugeTestUtils.URNS[0];
        urns.add( urn );
        fman.addIncompleteFile(new File("sambe"), urns, "a", 0, new VerifyingFile(0));
        assertEquals("unexpected shared incomplete", 1, fman.getNumIncompleteFiles());
            
        // First test that we DO get this IFD.
        FileDesc fd = fman.getFileDescForUrn(urn);
        assertEquals( urns, fd.getUrns() );
        
        // add a file to the library and load it up.
        f3 = createNewTestFile(11);   
        waitForLoad();
        assertEquals("unexected shared files", 1, fman.getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
        
        // ensure it got shared.
        files=fman.getSharedFileDescriptors(_sharedDir);
        assertEquals( f3, files[0].getFile() );
        fd = fman.get(0);
        urn = fd.getSHA1Urn();
        urns = fd.getUrns();
        
        // now add an ifd with those urns.
        fman.addIncompleteFile(new File("sam"), urns, "b", 0, new VerifyingFile(0));
        
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
			Set<URN.Type> requestedUrnSet1 = new HashSet<URN.Type>();
			Set<URN.Type> requestedUrnSet2 = new HashSet<URN.Type>();
			Set<URN.Type> requestedUrnSet3 = new HashSet<URN.Type>();
			requestedUrnSet1.add(URN.Type.ANY_TYPE);
			requestedUrnSet2.add(URN.Type.SHA1);
			requestedUrnSet3.add(URN.Type.ANY_TYPE);
			requestedUrnSet3.add(URN.Type.SHA1);
			Set[] requestedUrnSets = {URN.Type.NO_TYPE_SET, requestedUrnSet1, 
									  requestedUrnSet2, requestedUrnSet3};
			Set<URN> queryUrnSet = new UrnSet();
			queryUrnSet.add(urn);
			for(int j = 0; j < requestedUrnSets.length; j++) {
				QueryRequest qr = QueryRequest.createQuery(queryUrnSet);
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
			String name = I18NConvert.instance().getNorm(fd.getFileName());
            
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = QueryRequest.createQuery(name);
			Response[] hits = fman.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
			if ( hits.length != 1 ) continue;
			checked = true;
			assertEquals("responses should be equal", testResponse, hits[0]);
			Set<URN> urnSet = hits[0].getUrns();
			URN[] responseUrns = urnSet.toArray(new URN[0]);
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

	    FileDesc[] fds = fman.getAllSharedFileDescriptors();
	    for(int i = 0; i < fds.length; i++) {
	        URN urn = fds[i].getSHA1Urn();
	        for(int j = 0; j < MAX_LOCATIONS + 5; j++) {
	            RouterService.getAltlocManager().add(AlternateLocation.create("1.2.3." + j, urn),null);
	        }
	    }
        
        boolean checked = false;
		for(int i = 0; i < fman.getNumFiles(); i++) {
			FileDesc fd = fman.get(i);
			Response testResponse = new Response(fd);
			String name = I18NConvert.instance().getNorm(fd.getFileName());
            
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = QueryRequest.createQuery(name);
			Response[] hits = fman.query(qr);
			assertNotNull("didn't get a response for query " + qr, hits);
			// we can only do this test on 'unique' names, so if we get more than
			// one response, don't test.
			if ( hits.length != 1 ) continue;
			checked = true;
			assertEquals("responses should be equal", testResponse, hits[0]);
			assertEquals("should have 10 other alts", 10, testResponse.getLocations().size());
			assertEquals("should have equal alts",
			    testResponse.getLocations(), hits[0].getLocations());
		}
		assertTrue("wasn't able to find any unique classes to check against.", checked);
        RouterService.getAltlocManager().purge();
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
        assertFalse("query should not be in qrt", qrt.contains(qr));
        
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
        FileManagerEvent result = renameIfShared(f2, f4);
        assertTrue(result.toString(), result.isRenameEvent());
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
        File sessionShared = createNewNamedTestFile(10, "sessionShared", _sharedDir.getParentFile());
        getLibraryData().SPECIAL_FILES_TO_SHARE.add(shared);
        waitForLoad();

        // add the session-shared file
        fman.addFileForSession(sessionShared);
        Thread.sleep(200);
        
        //  assert that "shared" and "notShared" are not in a shared directory
        assertFalse(fman.isFileInCompletelySharedDirectory(shared));
        assertFalse(fman.isFileInCompletelySharedDirectory(notShared));
        assertFalse(fman.isFileInCompletelySharedDirectory(sessionShared));
        
        //  assert that "shared" and "sessionShared" are shared
        assertEquals(2,fman.getNumFiles());
        assertTrue(fman.isFileShared(shared));
        assertTrue(fman.isFileShared(sessionShared));
        assertFalse(fman.isFileShared(notShared));
        assertNotNull(fman.getFileDescForFile(shared));
        assertNotNull(fman.getFileDescForFile(sessionShared));
        assertNull(fman.getFileDescForFile(notShared));
        
        // simulate restart
        fman = new SimpleFileManager();
        waitForLoad();
        
        //  assert that "shared" is shared
        assertEquals(1,fman.getNumFiles());
        assertTrue(fman.isFileShared(shared));
        assertNotNull(fman.getFileDescForFile(shared));
        
        // but sessionShared is no more.
        assertFalse(fman.isFileShared(sessionShared));
        assertFalse(fman.isFileShared(notShared));
        assertNull(fman.getFileDescForFile(notShared));
        assertNull(fman.getFileDescForFile(notShared));
    }
    
    public void testSpecialApplicationShare() throws Exception{
        File specialShare = createNewNamedTestFile(10, "shared", FileManager.APPLICATION_SPECIAL_SHARE);
        MultiListener listener = new MultiListener();
        fman.addFileEventListener(listener);
        waitForLoad();
        //should not have dipatched an event for the folder
        for(FileManagerEvent fevt: listener.getFileManagerEventList()) {
            if(fevt.isAddFolderEvent()) {
                File[] files = fevt.getFiles();
                for(int i = 0 ; i < files.length; i++) {
                    if(files[i] != null) {
                        assertFalse(files[i].getParent().equals(FileManager.APPLICATION_SPECIAL_SHARE));
                    }
                }
            }
        }
        specialShare = createNewNamedTestFile(10, "shared2", FileManager.APPLICATION_SPECIAL_SHARE);
        FileManagerEvent evt = addFileForSession(specialShare);
        assertTrue(evt.isAddEvent());
        //should have shared file
        assertTrue(fman.isFileShared(specialShare));
        //should not be an individual share
        assertFalse(fman.isIndividualShare(specialShare));
    }
	
	/**
	 * Tests {@link FileManager#addFileAlways(File)}.
	 * @throws Exception
	 */
	public void testAddFileAlways() throws Exception {
	    assertFalse(fman.isLoadFinished());
	    waitForLoad(); // ensure it's loaded with 0 files.
	    assertEquals(0, fman.getNumFiles());
	    assertTrue(fman.isLoadFinished());
	    
		// test if too large files are not shared
		File tooLarge = createFakeTestFile(MAX_FILE_SIZE+1l);
		FileManagerEvent result = addAlways(tooLarge);
		assertTrue(result.toString(), result.isFailedEvent());
		assertEquals(tooLarge, result.getFiles()[0]);
		
		// test if files in shared directories are still shared
		File test = createNewTestFile(5);
		result = addAlways(test);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(test, result.getFileDescs()[0].getFile());
		assertEquals(1, fman.getNumFiles());
		
		// try again, it will fail because it's already shared.
		result = addAlways(test);
		assertTrue(result.toString(), result.isAlreadySharedEvent());
		assertEquals(test, result.getFiles()[0]);
		
		// test that non-existent files are not shared
		test = new File("non existent file").getCanonicalFile();
		result = addAlways(test);
		assertTrue(result.toString(), result.isFailedEvent());
		assertEquals(test, result.getFiles()[0]);
		
		// test that file in non shared directory is shared
		File dir = createNewBaseDirectory("notshared");
		dir.mkdir();
		dir.deleteOnExit();
		test = createNewNamedTestFile(500, "specially shared", dir);
		result = addAlways(test);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(test, result.getFileDescs()[0].getFile());
        assertEquals(2, fman.getNumFiles());
        
        // try again, it will fail because it's already shared.
        result = addAlways(test);
        assertTrue(result.toString(), result.isAlreadySharedEvent());
        assertEquals(test, result.getFiles()[0]);
	}

	public void testIsFileInCompletelySharedDirectory() throws Exception {
		// non existent file should not be in a shared directory
		File nonexistent = new File("nonexistent");
		assertFalse("File should not be in a shared directory", fman.isFileInCompletelySharedDirectory(nonexistent));
		
		File dir = createNewBaseDirectory("notshared");
		dir.mkdir();
		dir.deleteOnExit();
		File test = createNewNamedTestFile(10, "noshared", dir);
		assertFalse("File should not be in a shared directory",fman.isFileInCompletelySharedDirectory(test));
		
		// test for files in subdirs
		File subDir = new File(_sharedDir, "newSubDir");
		subDir.mkdir();
		subDir.deleteOnExit();
		waitForLoad();
		assertTrue("Subdir should be in shared directory", fman.isFileInCompletelySharedDirectory(subDir));
		
		test = createNewNamedTestFile(50, "subdirfile", subDir);
		waitForLoad();
		// test if subdir is shared
		assertTrue("File in subdir should be in shared directory now", fman.isFileInCompletelySharedDirectory(test));
	}
	
	/**
	 * Tests {@link FileManager#addFileIfShared(File)}.
	 * <p>
	 * Basically files should be added when they are shareable.
	 * @throws Exception
	 */
	public void testAddFileIfShared() throws Exception {
		// non shareable files:
		
		// too large tested above
		
		// non shareable extension in shared directory
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("abc");
		waitForLoad(); // set the extensions correctly.
		File nonshareable = createNewNamedTestFile(10, "nonshareable extension");
		FileManagerEvent result = addIfShared(nonshareable);
		assertTrue(result.toString(), result.isFailedEvent());
		assertEquals(nonshareable, result.getFiles()[0]);
		nonshareable.delete();
		
		// not in shared directory and not specially shared, but valid extension
		SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
		waitForLoad(); // set the new extensions
		File validExt = createNewNamedTestFile(10, "valid extension", _sharedDir.getParentFile());
		result = addIfShared(validExt);
		assertTrue(result.toString(), result.isFailedEvent());
		assertEquals(validExt, result.getFiles()[0]);

		// nonexistent in shared directory
		File nonexistent = new File(_sharedDir, "test." + EXTENSION);
		result = addIfShared(nonexistent);
		assertTrue(result.toString(), result.isFailedEvent());
		assertEquals(nonexistent, result.getFiles()[0]);
		
		// nonexistent in non shared directory
		nonexistent = new File("nonexistent." + EXTENSION).getCanonicalFile();
		result = addIfShared(nonexistent);
		assertTrue(result.toString(), result.isFailedEvent());
		assertEquals(nonexistent, result.getFiles()[0]);
		
		// nonexistent, but specially shared
		result = addAlways(nonexistent);
		assertTrue(result.toString(), result.isFailedEvent());
		assertEquals(nonexistent, result.getFiles()[0]);
		
		// shareable files:
		
		// files with shareable extension in shared directory
		File shareable = createNewNamedTestFile(10, "shareable");
		result = addIfShared(shareable);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(shareable, result.getFileDescs()[0].getFile());
		assertEquals(1, fman.getNumFiles());
		assertEquals(result.getFileDescs()[0], fman.get(0));
		
		// files with shareable extension, specially shared
		File speciallyShareable = createNewNamedTestFile(10, "specially shareable", _sharedDir.getParentFile());
		result = addAlways(speciallyShareable);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(speciallyShareable, result.getFileDescs()[0].getFile());
		assertEquals(2, fman.getNumFiles());
		assertEquals(result.getFileDescs()[0], fman.get(1));
		
		// files with non shareable extension, specially shared
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("abc");
		File speciallyShared = createNewNamedTestFile(10, "speciall shared", _sharedDir.getParentFile());
		result = addAlways(speciallyShared);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(speciallyShared, result.getFileDescs()[0].getFile());
		assertEquals(3, fman.getNumFiles());
		assertEquals(result.getFileDescs()[0], fman.get(2));
	}
	
	public void testAddSharedFoldersWithBlackList() throws Exception {
		File[] dirs = LimeTestUtils.createDirs(_sharedDir, 
				"recursive1",
				"recursive1/sub1",
				"recursive1/sub1/subsub",
				"recursive1/sub2",
				"recursive2");
		
		// create files in all folders, so we can see if they were shared
		for (int i = 0; i < dirs.length; i++) {
			createNewNamedTestFile(i + 1, "recshared" + i, dirs[i]);
		}
		
		List<File> whiteList = Arrays.asList(dirs[0], dirs[1], dirs[4]);
		List<File> blackList = Arrays.asList(dirs[2], dirs[3]);
		fman.addSharedFolders(new HashSet<File>(whiteList), new HashSet<File>(blackList));
		waitForLoad();
		
		// assert blacklist worked
		for (File dir : blackList) {
			assertEquals(0, fman.getSharedFileDescriptors(dir).length);
		}
		
		// assert others were shared
		for (File dir : whiteList) {
			assertEquals(1, fman.getSharedFileDescriptors(dir).length);
		}
	}
	
    public void testExplicitlySharedSubfolderUnsharedDoesntStayShared() throws Exception {
        File[] dirs = LimeTestUtils.createDirs(_sharedDir, 
                "recursive1",
                "recursive1/sub1");
        
        assertEquals(2, dirs.length);
        assertEquals("sub1", dirs[1].getName()); // make sure sub1 is second!
        
        // create files in all folders, so we can see if they were shared
        for (int i = 0; i < dirs.length; i++) {
            createNewNamedTestFile(i + 1, "recshared" + i, dirs[i]);
        }
        
        fman.removeFolderIfShared(_sharedDir);
        fman.addSharedFolder(dirs[1]);
        fman.addSharedFolder(dirs[0]);
        
        waitForLoad();
        
        assertEquals(1, fman.getSharedFileDescriptors(dirs[0]).length);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[1]).length);
        
        // Now unshare sub1
        fman.removeFolderIfShared(dirs[1]);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[0]).length);
        assertEquals(0, fman.getSharedFileDescriptors(dirs[1]).length);
        
        // Now reload fman and make sure it's still not shared!
        fman.loadSettingsAndWait(10000);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[0]).length);
        assertEquals(0, fman.getSharedFileDescriptors(dirs[1]).length);
    }
    
    public void testExplicitlySharedSubSubfolderUnsharedDoesntStayShared() throws Exception {
        File[] dirs = LimeTestUtils.createDirs(_sharedDir, 
                "recursive1",
                "recursive1/sub1",
                "recursive1/sub1/sub2");
        
        assertEquals(3, dirs.length);
        assertEquals("sub2", dirs[2].getName()); // make sure sub2 is third!
        
        // create files in all folders, so we can see if they were shared
        for (int i = 0; i < dirs.length; i++) {
            createNewNamedTestFile(i + 1, "recshared" + i, dirs[i]);
        }
        
        fman.removeFolderIfShared(_sharedDir);
        fman.addSharedFolder(dirs[2]);
        fman.addSharedFolder(dirs[0]);
        
        waitForLoad();
        
        assertEquals(1, fman.getSharedFileDescriptors(dirs[0]).length);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[1]).length);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[2]).length);
        
        // Now unshare sub2
        fman.removeFolderIfShared(dirs[2]);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[0]).length);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[1]).length);
        assertEquals(0, fman.getSharedFileDescriptors(dirs[2]).length);
        
        // Now reload fman and make sure it's still not shared!
        fman.loadSettingsAndWait(10000);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[0]).length);
        assertEquals(1, fman.getSharedFileDescriptors(dirs[1]).length);
        assertEquals(0, fman.getSharedFileDescriptors(dirs[2]).length);
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
        
        //  check that the user's home dir is a sensitive directory
        String userHome = System.getProperty("user.home");
        assertTrue("user's home directory should be a sensitive directory", FileManager.isSensitiveDirectory(new File(userHome)));
        
        //  check for OS-specific directories:
        String realOS = System.getProperty("os.name");
        
        try {
            
        setOSName("Windows");
        assertTrue("Documents and Settings should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Documents and Settings")));
        assertTrue("My Documents should be a sensitive directory", FileManager.isSensitiveDirectory(new File(userHome, "My Documents")));
        assertTrue("Desktop should be a sensitive directory", FileManager.isSensitiveDirectory(new File(userHome, "Desktop")));
        assertTrue("Program Files should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Program Files")));
        assertTrue("Windows should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Windows")));
        assertTrue("Windows should be a sensitive directory", FileManager.isSensitiveDirectory(new File("WINNT")));
        
        setOSName("Mac OS X");
        assertTrue("Users should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Users")));
        assertTrue("System should be a sensitive directory", FileManager.isSensitiveDirectory(new File("System")));
        assertTrue("System Folder should be a sensitive directory", FileManager.isSensitiveDirectory(new File("System Folder")));
        assertTrue("Previous Systems should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Previous Systems")));
        assertTrue("private should be a sensitive directory", FileManager.isSensitiveDirectory(new File("private")));
        assertTrue("Volumes should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Volumes")));
        assertTrue("Desktop should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Desktop")));
        assertTrue("Applications should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Applications")));
        assertTrue("Applications (Mac OS 9) should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Applications (Mac OS 9)")));
        assertTrue("Network should be a sensitive directory", FileManager.isSensitiveDirectory(new File("Network")));
        
        setOSName("Linux");
        assertTrue("bin should be a sensitive directory", FileManager.isSensitiveDirectory(new File("bin")));
        assertTrue("boot should be a sensitive directory", FileManager.isSensitiveDirectory(new File("boot")));
        assertTrue("dev should be a sensitive directory", FileManager.isSensitiveDirectory(new File("dev")));
        assertTrue("etc should be a sensitive directory", FileManager.isSensitiveDirectory(new File("etc")));
        assertTrue("home should be a sensitive directory", FileManager.isSensitiveDirectory(new File("home")));
        assertTrue("mnt should be a sensitive directory", FileManager.isSensitiveDirectory(new File("mnt")));
        assertTrue("opt should be a sensitive directory", FileManager.isSensitiveDirectory(new File("opt")));
        assertTrue("proc should be a sensitive directory", FileManager.isSensitiveDirectory(new File("proc")));
        assertTrue("root should be a sensitive directory", FileManager.isSensitiveDirectory(new File("root")));
        assertTrue("sbin should be a sensitive directory", FileManager.isSensitiveDirectory(new File("sbin")));
        assertTrue("usr should be a sensitive directory", FileManager.isSensitiveDirectory(new File("usr")));
        assertTrue("var should be a sensitive directory", FileManager.isSensitiveDirectory(new File("var")));
        
        } finally {
            //  revert the os.name system property back to normal 
            setOSName(realOS);
        }
    }
    
    public void testIsSharableFolder() throws Exception {
        //  check that system roots are sensitive directories
        File[] faRoots = File.listRoots();
        if(faRoots != null && faRoots.length > 0) {
            for(int i = 0; i < faRoots.length; i++) {
                assertFalse("root directory "+faRoots[i]+ " should not be sharable", 
                           fman.isFolderShareable(faRoots[i], false));
                assertFalse("root directory "+faRoots[i]+ " should not be sharable", 
                        fman.isFolderShareable(faRoots[i], true));
            }
        }
    }
    
    public void testGetIndexingIterator() throws Exception {
        LimeXMLDocument document = new LimeXMLDocument(
                "<?xml version=\"1.0\"?>"+
                "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
                "  <audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
                "</audios>");

        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        fman.get(0).addLimeXMLDocument(document);
        fman.get(1).addLimeXMLDocument(document);
        f3 = createNewTestFile(11);

        assertEquals(2, fman.getNumFiles());
        Iterator<Response> it = fman.getIndexingIterator(false);
        Response response = it.next();
        assertEquals(response.getName(), f1.getName());
        assertNull(response.getDocument());
        response = it.next();
        assertEquals(response.getName(), f2.getName());
        assertNull(response.getDocument());
        assertFalse(it.hasNext());
        try {
            response = it.next();
            fail("Expected NoSuchElementException, got: " + response);
        } catch (NoSuchElementException e) {
        }
        
        it = fman.getIndexingIterator(false);
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        fman.removeFileIfShared(f2);
        assertFalse(it.hasNext());

        it = fman.getIndexingIterator(true);
        response = it.next();
        assertNotNull(response.getDocument());
        assertFalse(it.hasNext());
    }
    
    /**
     * Helper function to set the operating system so that multiple OSs can be partially-checked
     * by testing on one platform.
     */
    private static void setOSName(String name) throws Exception {
        System.setProperty("os.name", name);
        PrivilegedAccessor.invokeMethod(OSUtils.class, "setOperatingSystems");
    }
    
    //helper function to create queryrequest with I18N
    private QueryRequest get_qr(File f) {
        String norm = I18NConvert.instance().getNorm(f.getName());
        norm = StringUtils.replace(norm, "_", " ");
        return QueryRequest.createQuery(norm);
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

   		for(int i=0; i<testFiles.length; i++) {
			if(!testFiles[i].isFile()) continue;
			File shared = new File(
			    _sharedDir, testFiles[i].getName() + "." + EXTENSION);
			assertTrue("unable to get file", FileUtils.copy( testFiles[i], shared));
		}

        waitForLoad();

        
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
    


    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name, in the default shared directory.
     */
    protected File createNewNamedTestFile(int size, String name) throws Exception {
        return createNewNamedTestFile(size, name, _sharedDir);
    }

    protected File createNewTestFile(int size) throws Exception {
        return createNewNamedTestFile(size, "FileManager_unit_test", _sharedDir);
    }
    
    protected URN getURN(File f) throws Exception {
        return URN.createSHA1Urn(f);
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

    /** Same a createNewTestFile but doesn't actually allocate the requested
     *  number of bytes on disk.  Instead returns a subclass of File. */
    File createFakeTestFile(long size) throws Exception {
        File real=createNewTestFile(1);
        return new HugeFakeFile(real.getParentFile(), real.getName(), size);       
    }
    
    protected LibraryData getLibraryData() throws Exception {
        return (LibraryData)PrivilegedAccessor.getValue(fman, "_data");
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
        
        public File getCanonicalFile() {
            return this;
        }
    }

    protected void waitForLoad() {
        try {
            fman.loadSettingsAndWait(10000);
        } catch(InterruptedException e) {
            fail(e);
        } catch(TimeoutException te) {
            fail(te);
        }
    }    
    
    public static class Listener implements FileEventListener {
        public FileManagerEvent evt;
        public synchronized void handleFileEvent(FileManagerEvent fme) {
            evt = fme;
            notify();
        }
    }
    
    private static class MultiListener implements FileEventListener {
        private List<FileManagerEvent> evtList = new ArrayList<FileManagerEvent>();
        public synchronized void handleFileEvent(FileManagerEvent fme) {
            evtList.add(fme);
        }

        public synchronized List<FileManagerEvent> getFileManagerEventList() {
            return evtList;
        }
    }
    
    protected FileManagerEvent addIfShared(File f) throws Exception {
        Listener fel = new Listener();
        synchronized(fel) {
            fman.addFileIfShared(f, fel);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent addAlways(File f) throws Exception {
        Listener fel = new Listener();
        synchronized(fel) {
            fman.addFileAlways(f, fel);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent renameIfShared(File f1, File f2) throws Exception {
        Listener fel = new Listener();
        synchronized(fel) {
            fman.renameFileIfShared(f1, f2, fel);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent addFileForSession(File f1) throws Exception {
        Listener fel = new Listener();
        synchronized(fel) {
            fman.addFileForSession(f1, fel);
            fel.wait(5000);
        }
        return fel.evt;
    }
}


