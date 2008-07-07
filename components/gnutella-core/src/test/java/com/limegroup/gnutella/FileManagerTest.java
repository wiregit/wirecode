package com.limegroup.gnutella;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.Range;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.limegroup.gnutella.LimeWireCoreModule.FileEventListenerProvider;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.StubContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.LibraryData;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.util.FileManagerTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.MessageTestUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;


public class FileManagerTest extends LimeTestCase {

    protected static final String SHARE_EXTENSION = "XYZ";
    protected static final String EXTENSION = SHARE_EXTENSION + ";mp3";
    private static final int MAX_LOCATIONS = 10;
    
    protected File f1 = null;
    protected File f2 = null;
    protected File f3 = null;
    protected File f4 = null;
    protected File f5 = null;
    protected File f6 = null;
    
    protected File store1 = null;
    protected File store2 = null;
    protected File store3 = null;
   
    //changed to protected so that MetaFileManagerTest can
    //use these variables as well.
    protected volatile FileManagerImpl fman = null;
    protected QRPUpdater qrpUpdater = null;
    protected Object loaded = new Object();
    protected Response[] responses;
    protected List<FileDesc> sharedFiles;
    protected Injector injector;
    protected SharedFilesKeywordIndex keywordIndex;
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    private QueryRequestFactory queryRequestFactory;
    
    private SchemaReplyCollectionMapper schemaMapper;

    public FileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileManagerTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        cleanFiles(_incompleteDir, false);
        cleanFiles(_sharedDir, false);
        cleanFiles(_storeDir, false);


        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        });
        
        fman = (FileManagerImpl)injector.getInstance(FileManager.class);
        keywordIndex = injector.getInstance(SharedFilesKeywordIndex.class);
        qrpUpdater = injector.getInstance(QRPUpdater.class);
        schemaMapper = injector.getInstance(SchemaReplyCollectionMapper.class);
        
        fman.addFileEventListener(keywordIndex);
        fman.addFileEventListener(qrpUpdater);
        fman.addFileEventListener(schemaMapper);
              
        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }
	
    @Override
	protected void tearDown() {
        if (f1!=null) f1.delete();
        if (f2!=null) f2.delete();
        if (f3!=null) f3.delete();
        if (f4!=null) f4.delete();
        if (f5!=null) f5.delete();
        if (f6!=null) f6.delete();	    
        
        if(store1!=null) store1.delete();
        if(store2!=null) store2.delete();
        if(store3!=null) store3.delete();
    }
        
    public void testGetParentFile() throws Exception {
        f1 = createNewTestFile(1);
        assertEquals("getParentFile doesn't work",
            new File(f1.getParentFile().getCanonicalPath()),
            new File(_sharedDir.getCanonicalPath()));
    }
       
    public void testGetSharedFilesWithNoShared() throws Exception {
        List<FileDesc> sharedFiles =  fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals(sharedFiles.toString(), 
				0, sharedFiles.size());
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
        ContentManager cm = injector.getInstance(ContentManager.class);
        cm.start();
        // request the urn so we can use the response.
        cm.request(u1, new StubContentResponseObserver(), 1000);
        cm.handleContentResponse(new ContentResponse(u1, false));
        
        waitForLoad();
        assertEquals("unexpected # of shared files", 3, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected size of shared files", 37, fman.getSharedFileList().getNumBytes());
        assertFalse("shouldn't be shared", fman.getSharedFileList().contains(f1));
        assertTrue("should be shared", fman.getSharedFileList().contains(f2));
        assertTrue("should be shared", fman.getSharedFileList().contains(f3));
        assertTrue("should be shared", fman.getSharedFileList().contains(f4));
        
        FileDesc fd2 = fman.getFileDescForFile(f2);
        FileDesc fd3 = fman.getFileDescForFile(f3);
        FileDesc fd4 = fman.getFileDescForFile(f4);
        
        // test invalid content response.
        fman.validate(fd2);
        cm.handleContentResponse(new ContentResponse(u2, false));
        assertFalse("shouldn't be shared anymore", fman.getSharedFileList().contains(f2));
        assertEquals("wrong # shared files", 2, fman.getSharedFileList().getNumFiles());
        assertEquals("wrong shared file size", 34, fman.getSharedFileList().getNumBytes());
        
        // test valid content response.
        fman.validate(fd3);
        cm.handleContentResponse(new ContentResponse(u3, true));
        assertTrue("should still be shared", fman.getSharedFileList().contains(f3));
        assertEquals("wrong # shared files", 2, fman.getSharedFileList().getNumFiles());
        assertEquals("wrong shared file size", 34, fman.getSharedFileList().getNumBytes());

        // test valid content response.
        fman.validate(fd4);
        Thread.sleep(10000);
        assertTrue("should still be shared", fman.getSharedFileList().contains(f4));
        assertEquals("wrong # shared files", 2, fman.getSharedFileList().getNumFiles());
        assertEquals("wrong shared file size", 34, fman.getSharedFileList().getNumBytes());
        
        // Make sure adding a new file to be shared doesn't work if it
        // returned bad before.
        fman.addFileIfShared(f1);
        assertFalse("shouldn't be shared", fman.getSharedFileList().contains(f1));
    }
    
    public void testOneSharedFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        f1 = createNewTestFile(1);
        waitForLoad();
        f2 = createNewTestFile(3);
        f3 = createNewTestFile(11);

        // fman should only have loaded f1
        assertEquals("Unexpected number of shared files", 
            1, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected size of filemanager",
            1, fman.getSharedFileList().getNumBytes());
            
        // it is important to check the query at all bounds,
        // including tests for case.
        responses=keywordIndex.query(queryRequestFactory.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery(
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 1, responses.length);        
                
        
        // should not be able to remove unshared file
        assertNull("should have not been able to remove f3", 
				   fman.removeFileIfSharedOrStore(f3));
				   
        assertEquals("first file should be f1", f1, fman.getSharedFileList().get(0).getFile());
        
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals("unexpected length of shared files", 1, sharedFiles.size());
        assertEquals("files should be the same", sharedFiles.get(0).getFile(), f1);
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir.getParentFile());
        assertEquals("file manager listed shared files in file's parent dir",
            0, sharedFiles.size());
    }
    
    public void testAddingOneSharedFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        f1 = createNewTestFile(1);
        waitForLoad();
        f2 = createNewTestFile(3);
        f3 = createNewTestFile(11);

        FileManagerEvent result = addIfShared(new File("C:\\bad.ABCDEF"));
        assertTrue(result.toString(), result.isFailedAddEvent());
        
        result = addIfShared(f2);
        assertTrue(result.toString(), result.isAddEvent());
        assertNotNull(result.getFileDescs()[0]);
        
        assertEquals("unexpected number of files", 2, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected fman size", 4, fman.getSharedFileList().getNumBytes());
        responses=keywordIndex.query(queryRequestFactory.createQuery("unit", (byte)3));
        assertNotEquals("responses gave same index",
            responses[0].getIndex(), responses[1].getIndex() );
        for (int i=0; i<responses.length; i++) {
            assertTrue("responses should be expected indexes", 
                responses[i].getIndex()==0 || responses[i].getIndex()==1);
        }
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals("unexpected files length", 2, sharedFiles.size());
        assertSharedFiles(sharedFiles, f1, f2);
    }
    
    public void testRemovingOneSharedFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        f3 = createNewTestFile(11);

        //Remove file that's shared.  Back to 1 file.                   
        assertEquals(2, fman.getSharedFileList().getNumFiles());     
        assertNull("shouldn't have been able to remove unshared file",  fman.removeFileIfSharedOrStore(f3));
        assertNotNull("should have been able to remove shared file", fman.removeFileIfSharedOrStore(f2));
        assertEquals("unexpected fman size", 1, fman.getSharedFileList().getNumBytes());
        assertEquals("unexpected number of files", 1, fman.getSharedFileList().getNumFiles());
        responses=keywordIndex.query(queryRequestFactory.createQuery("unit", (byte)3));
        assertEquals("unexpected response length", 1, responses.length);
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals("unexpected files length", 1, sharedFiles.size());
        assertEquals("files differ", sharedFiles.get(0).getFile(), f1);
    }
    
    // TODO: Race condition pertaining to listener timing out before the event is received
    //        with addIfShared
    public void testAddAnotherSharedFileDifferentIndex() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        f3 = createNewTestFile(11);
        fman.removeFileIfSharedOrStore(f2);

        //Add a new second file, with new index.
        FileManagerEvent result = addIfShared(f3);
        assertTrue(result.toString(), result.isAddEvent());
        assertNotNull(result.getFileDescs()[0]);
        assertEquals("unexpected file size", 12, fman.getSharedFileList().getNumBytes());
        assertEquals("unexpedted number of files", 2, fman.getSharedFileList().getNumFiles());
        responses=keywordIndex.query(queryRequestFactory.createQuery("unit", (byte)3));
        assertEquals("unexpected response length", 2, responses.length);
        assertNotEquals("unexpected response[0] index", 1, responses[0].getIndex());
        assertNotEquals("unexpected response[1] index", 1, responses[1].getIndex());
        fman.getSharedFileList().get(0);
        fman.getSharedFileList().get(2);
        assertNull("should be null (unshared)", fman.getSharedFileList().get(1));
        try {
            fman.getSharedFileList().get(3);
            fail("should not have gotten anything");
        } catch (IndexOutOfBoundsException e) { }
        
        assertFalse("should not be valid", fman.getSharedFileList().isValidSharedIndex(3));
        assertTrue("should be valid", fman.getSharedFileList().isValidSharedIndex(0));
        assertTrue("should be valid (was at one time)", fman.getSharedFileList().isValidSharedIndex(1));

        responses=keywordIndex.query(queryRequestFactory.createQuery("*unit*", (byte)3));
        assertEquals("unexpected responses length", 2, responses.length);

        sharedFiles = fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals("unexpected files length", 2, sharedFiles.size());
        assertSharedFiles(sharedFiles, f1, f3);
        sharedFiles= fman.getSharedFileList().getAllFileDescs();
        assertEquals("unexpected files length", 2, fman.getSharedFileList().getNumFiles());
        assertSharedFiles(sharedFiles, f1, f3);            
    }
    
    public void testRenameSharedFiles() throws Exception {
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        f3 = createNewTestFile(11);
        fman.removeFileIfSharedOrStore(f2);
        addIfShared(f3);

        FileManagerEvent result = renameFile(f2, new File("c:\\asdfoih"));
        assertTrue(result.toString(), result.getType() == FileManagerEvent.Type.RENAME_FILE_FAILED);
        assertEquals(f2, result.getFiles()[0]);
        
        result = renameFile(f1, f2);
        assertTrue(result.toString(), result.isRenameEvent());
        assertEquals(f1, result.getFileDescs()[0].getFile());
        assertEquals(f2, result.getFileDescs()[1].getFile());
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals("unexpected files length", 2, sharedFiles.size());
        assertSharedFiles(sharedFiles, f2, f3);
        
        result = renameFile(f2, new File("C\\garbage.XSADF"));
        assertTrue(result.toString(), result.getType() == FileManagerEvent.Type.REMOVE_FILE);
        assertEquals(f2, result.getFileDescs()[0].getFile());
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals("unexpected files length", 1, sharedFiles.size());
        assertEquals("files differ", sharedFiles.get(0).getFile(), f3);
    }
    
    public void testFileChangedNoFD() throws Exception {
        waitForLoad();
        f1 = createNewNameStoreTestFile("name", _sharedDir);
        
        FileManagerEvent result = fileChanged(f1);
        assertEquals(result.getType(), FileManagerEvent.Type.CHANGE_FILE_FAILED);
    }
    
    public void testChangeSharedFile() throws Exception {
        f1 = createNewNameStoreTestFile("name", _sharedDir);
        waitForLoad();
        
        assertEquals(1, fman.getSharedFileList().getNumFiles());
        
        FileDesc fd = fman.getFileDescForFile(f1);

        CreationTimeCache cache = injector.getInstance(CreationTimeCache.class);

        cache.addTime(fd.getSHA1Urn(), 1234);
        long time = cache.getCreationTimeAsLong(fd.getSHA1Urn()); 

        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString("title=\"Alive\""));
        fd.addLimeXMLDocument(d1);
        
        FileManagerEvent result = fileChanged(f1);
        assertEquals(result.getType(), FileManagerEvent.Type.CHANGE_FILE);    
        fd = fman.getFileDescForFile(f1);

        assertEquals(time, cache.getCreationTimeAsLong(fd.getSHA1Urn()));
    }
    
    public void testChangeStoreFile() throws Exception {

        waitForLoad();
        
        f1 = createNewTestStoreFile();
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(f1, l1);
        
        assertEquals(result.getType(), FileManagerEvent.Type.ADD_FILE);
        assertEquals(1, fman.getStoreFileList().getNumFiles());
        
        FileDesc fd = fman.getFileDescForFile(f1);

        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString("title=\"Alive 2\""));
        fd.addLimeXMLDocument(d2);
        
        result = fileChanged(f1);
        assertEquals(result.getType(), FileManagerEvent.Type.CHANGE_FILE); 
    }
    
    public void testIgnoreHugeFiles() throws Exception {
        
        f3 = createNewTestFile(11);   
        waitForLoad();
        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        
        //Try to add a huge file.  (It will be ignored.)
        f4 = createFakeTestFile(MAX_FILE_SIZE+1l);
        FileManagerEvent result = addIfShared(f4);
        assertTrue(result.toString(), result.isFailedAddEvent());
        assertEquals(f4, result.getFiles()[0]);
        assertEquals("unexpected number of files", 1, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected fman size", 11, fman.getSharedFileList().getNumBytes());
        //Add really big files.
        f5=createFakeTestFile(MAX_FILE_SIZE-1);
        f6=createFakeTestFile(MAX_FILE_SIZE);
        result = addIfShared(f5);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(f5, result.getFileDescs()[0].getFile());
        result = addIfShared(f6);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(f6, result.getFileDescs()[0].getFile());
        assertEquals("unexpected number of files", 3, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected fman size", Integer.MAX_VALUE, fman.getSharedFileList().getNumBytes());
    }
    
    /**
     * Tests adding incomplete files to the FileManager.
     */
    public void testAddIncompleteFile() throws Exception {
        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        
        assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());    
        
        // add one incomplete file and make sure the numbers go up.
        Set<URN> urns = new UrnSet();
        urns.add( UrnHelper.URNS[0] );
        fman.addIncompleteFile(new File("a"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 1, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        // add another incomplete file with the same hash and same
        // name and make sure it's not added.
        fman.addIncompleteFile(new File("a"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 1, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        // add another incomplete file with another hash, it should be added.
        urns = new UrnSet();
        urns.add( UrnHelper.URNS[1] );
        fman.addIncompleteFile(new File("c"), urns, "c", 0, verifyingFileFactory.createVerifyingFile(0));

        assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 2, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
    }
    
    public void testShareIncompleteFile() throws Exception {
        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        
        assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());

        File f;
        VerifyingFile vf;
        UrnSet urns;
        IncompleteFileDesc ifd;
        Mockery mockery = new Mockery();
        final QueryRequest qrDesiring = mockery.mock(QueryRequest.class);
        final QueryRequest notDesiring = mockery.mock(QueryRequest.class);
        mockery.checking(MessageTestUtils.createDefaultMessageExpectations(qrDesiring, QueryRequest.class));
        mockery.checking(MessageTestUtils.createDefaultMessageExpectations(notDesiring, QueryRequest.class));
        mockery.checking(MessageTestUtils.createDefaultQueryExpectations(qrDesiring));
        mockery.checking(MessageTestUtils.createDefaultQueryExpectations(notDesiring));
        mockery.checking(new Expectations(){{
            atLeast(1).of(qrDesiring).getQuery();
            will(returnValue("asdf"));
            atLeast(1).of(notDesiring).getQuery();
            will(returnValue("asdf"));
            atLeast(1).of(qrDesiring).desiresPartialResults();
            will(returnValue(true));
            atLeast(1).of(notDesiring).desiresPartialResults();
            will(returnValue(false));
            allowing(qrDesiring).shouldIncludeXMLInResponse();
            will(returnValue(true));
        }});
        
        // a) single urn, not enough data written -> not shared
        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
        vf.addInterval(Range.createRange(0,100));
        assertEquals(101,vf.getBlockSize());
        urns = new UrnSet();
        urns.add(UrnHelper.URNS[0]);
        f = new File("asdf");
        
        fman.addIncompleteFile(f, urns, "asdf", 1024 * 1024, vf);
        ifd = (IncompleteFileDesc) fman.getFileDescForUrn(UrnHelper.URNS[0]);
        assertFalse(ifd.hasUrnsAndPartialData());
        assertEquals(0,keywordIndex.query(qrDesiring).length);
        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
        
        // b) single urn, enough data written -> not shared
        fman.removeFileIfSharedOrStore(f);
        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
        vf.addInterval(Range.createRange(0,1024 * 512));
        assertGreaterThan(102400,vf.getBlockSize());
        urns = new UrnSet();
        urns.add(UrnHelper.URNS[0]);
        f = new File("asdf");
        
        fman.addIncompleteFile(f, urns, "asdf", 1024 * 1024, vf);
        ifd = (IncompleteFileDesc) fman.getFileDescForUrn(UrnHelper.URNS[0]);
        assertFalse(ifd.hasUrnsAndPartialData());
        assertEquals(0,keywordIndex.query(qrDesiring).length);
        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
        
        // c) two urns, not enough data written -> not shared
        fman.removeFileIfSharedOrStore(f);
        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
        vf.addInterval(Range.createRange(0,1024 ));
        assertLessThan(102400,vf.getBlockSize());
        urns = new UrnSet();
        urns.add(UrnHelper.URNS[0]);
        urns.add(UrnHelper.TTROOT);
        f = new File("asdf");
        
        fman.addIncompleteFile(f, urns, "asdf", 1024 * 1024, vf);
        ifd = (IncompleteFileDesc) fman.getFileDescForUrn(UrnHelper.URNS[0]);
        assertFalse(ifd.hasUrnsAndPartialData());
        assertEquals(0,keywordIndex.query(qrDesiring).length);
        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));

        // d) two urns, enough data written -> shared
        fman.removeFileIfSharedOrStore(f);
        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
        vf.addInterval(Range.createRange(0,1024 * 512));
        assertGreaterThan(102400,vf.getBlockSize());
        urns = new UrnSet();
        urns.add(UrnHelper.URNS[0]);
        urns.add(UrnHelper.TTROOT);
        assertGreaterThan(1, urns.size());
        f = new File("asdf");
        
        fman.addIncompleteFile(f, urns, "asdf", 1024 * 1024, vf);
        ifd = (IncompleteFileDesc) fman.getFileDescForUrn(UrnHelper.URNS[0]);
        assertTrue(ifd.hasUrnsAndPartialData());
        assertGreaterThan(0,keywordIndex.query(qrDesiring).length);
        assertEquals(0,keywordIndex.query(notDesiring).length);
        assertTrue(qrpUpdater.getQRT().contains(qrDesiring));
        double qrpFull = qrpUpdater.getQRT().getPercentFull();
        assertGreaterThan(0,qrpFull);
        
        // now remove the file and qrt should get updated
        fman.removeFileIfSharedOrStore(f);
        assertEquals(0,keywordIndex.query(qrDesiring).length);
        assertEquals(0,keywordIndex.query(notDesiring).length);
        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
        assertLessThan(qrpFull,qrpUpdater.getQRT().getPercentFull());
        
        // e) two urns, enough data written, sharing disabled -> not shared
        SharingSettings.ALLOW_PARTIAL_SHARING.setValue(false);
        fman.removeFileIfSharedOrStore(f);
        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
        vf.addInterval(Range.createRange(0,1024 * 512));
        assertGreaterThan(102400,vf.getBlockSize());
        urns = new UrnSet();
        urns.add(UrnHelper.URNS[0]);
        urns.add(UrnHelper.TTROOT);
        assertGreaterThan(1, urns.size());
        f = new File("asdf");
        
        fman.addIncompleteFile(f, urns, "asdf", 1024 * 1024, vf);
        ifd = (IncompleteFileDesc) fman.getFileDescForUrn(UrnHelper.URNS[0]);
        assertTrue(ifd.hasUrnsAndPartialData());
        assertEquals(0,keywordIndex.query(qrDesiring).length);
        assertEquals(0,keywordIndex.query(notDesiring).length);
        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
        SharingSettings.ALLOW_PARTIAL_SHARING.setValue(true);
        
        // f) start with one urn, add a second one -> becomes shared
        fman.removeFileIfSharedOrStore(f);
        vf = verifyingFileFactory.createVerifyingFile(1024 * 1024);
        vf.addInterval(Range.createRange(0,1024 * 512));
        assertGreaterThan(102400,vf.getBlockSize());
        urns = new UrnSet();
        urns.add(UrnHelper.URNS[0]);
        f = new File("asdf");
        
        fman.addIncompleteFile(f, urns, "asdf", 1024 * 1024, vf);
        ifd = (IncompleteFileDesc) fman.getFileDescForUrn(UrnHelper.URNS[0]);
        assertFalse(ifd.hasUrnsAndPartialData());
        assertEquals(0,keywordIndex.query(qrDesiring).length);
        assertFalse(qrpUpdater.getQRT().contains(qrDesiring));
        
        ifd.setTTRoot(UrnHelper.TTROOT);
        assertTrue(ifd.hasUrnsAndPartialData());
        fman.fileURNSUpdated(ifd);
        assertGreaterThan(0,keywordIndex.query(qrDesiring).length);
        assertEquals(0,keywordIndex.query(notDesiring).length);
        assertTrue(qrpUpdater.getQRT().contains(qrDesiring));
        
        // g) start with two urns, add data -> becomes shared
        // actually this is on the one scenario that won't work
        // because we do not have a callback mechanism for file
        // verification.  However, given that the default chunks size
        // we request is 128kb, we're bound to have more data downloaded
        // by the time we get the tree root.
        // This will change once we start using roots from replies.
    }
    
    /**
     * Tests the removeFileIfShared for incomplete files.
     */
    public void testRemovingIncompleteFiles() {
        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        
        assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        Set<URN> urns = new UrnSet();
        urns.add( UrnHelper.URNS[0] );
        fman.addIncompleteFile(new File("a"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));
        urns = new UrnSet();
        urns.add( UrnHelper.URNS[1] );
        fman.addIncompleteFile(new File("b"), urns, "b", 0, verifyingFileFactory.createVerifyingFile(0));
        assertEquals("unexpected shared incomplete", 2, fman.getSharedFileList().getNumIncompleteFiles());
            
        fman.removeFileIfSharedOrStore( new File("a") );
        assertEquals("unexpected shared incomplete", 1, fman.getSharedFileList().getNumIncompleteFiles());
        
        fman.removeFileIfSharedOrStore( new File("c") );
        assertEquals("unexpected shared incomplete", 1, fman.getSharedFileList().getNumIncompleteFiles());
        
        fman.removeFileIfSharedOrStore( new File("b") );
        assertEquals("unexpected shared incomplete", 0, fman.getSharedFileList().getNumIncompleteFiles());
    }
    
    /**
     * Tests that responses are not returned for zero size IncompleteFiles.
     */
    public void testQueryRequestsDoNotReturnZeroSizeIncompleteFiles() {
        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
            
        Set<URN> urns = new UrnSet();
        URN urn = UrnHelper.URNS[0];
        urns.add( urn );
        fman.addIncompleteFile(new File("sambe"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));
        assertEquals("unexpected shared incomplete", 1, fman.getSharedFileList().getNumIncompleteFiles());            
            
        QueryRequest qr = queryRequestFactory.createQuery(urn, "sambe");
        assertTrue(qr.desiresPartialResults());
        Response[] hits = keywordIndex.query(qr);
        assertNotNull(hits);
        assertEquals("unexpected number of resp.", 0, hits.length);
    }
    
    /**
     * Tests that IncompleteFileDescs are returned for FileDescForUrn only
     * if there are no complete files.
     */
    public void testGetFileDescForUrn() throws Exception {
        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        
		assertEquals("unexected shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete",
            0, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending",
            0, fman.getNumPendingFiles());
            
        Set<URN> urns = new UrnSet();
        URN urn = UrnHelper.URNS[0];
        urns.add( urn );
        fman.addIncompleteFile(new File("sambe"), urns, "a", 0, verifyingFileFactory.createVerifyingFile(0));
        assertEquals("unexpected shared incomplete", 1, fman.getSharedFileList().getNumIncompleteFiles());
            
        // First test that we DO get this IFD.
        FileDesc fd = fman.getFileDescForUrn(urn);
        assertEquals( urns, fd.getUrns() );
        
        // add a file to the library and load it up.
        f3 = createNewTestFile(11);   
        waitForLoad();
        assertEquals("unexected shared files", 1, fman.getSharedFileList().getNumFiles());
        assertEquals("unexpected shared incomplete", 0, fman.getSharedFileList().getNumIncompleteFiles());
        assertEquals("unexpected pending", 0, fman.getNumPendingFiles());
        
        // ensure it got shared.
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals( f3, sharedFiles.get(0).getFile() );
        fd = fman.getSharedFileList().get(0);
        urn = fd.getSHA1Urn();
        urns = fd.getUrns();
        
        // now add an ifd with those urns.
        fman.addIncompleteFile(new File("sam"), urns, "b", 0, verifyingFileFactory.createVerifyingFile(0));
        
        FileDesc retFD = fman.getSharedFileList().getFileDesc(urn);    
        assertNotNull(retFD);
        assertNotInstanceof(IncompleteFileDesc.class, retFD);
        assertEquals(retFD, fd);
    }
        
        
	/**
	 * Tests URN requests on the FileManager.
	 */
	public void testUrnRequests() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        
	    addFilesToLibrary();

		for(int i = 0; i < fman.getSharedFileList().getNumFiles(); i++) {
			FileDesc fd = fman.getSharedFileList().get(i);
			Response testResponse = responseFactory.createResponse(fd);
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
				QueryRequest qr = queryRequestFactory.createQuery(queryUrnSet);
				Response[] hits = keywordIndex.query(qr);
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
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        
	    addFilesToLibrary();
	    
	    boolean checked = false;
		for(int i = 0; i < fman.getSharedFileList().getNumFiles(); i++) {
			FileDesc fd = fman.getSharedFileList().get(i);
			Response testResponse = responseFactory.createResponse(fd);
			URN urn = fd.getSHA1Urn();
			String name = I18NConvert.instance().getNorm(fd.getFileName());
            
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = queryRequestFactory.createQuery(name);
			Response[] hits = keywordIndex.query(qr);
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
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
	    addFilesToLibrary();

	    List<FileDesc> fds = fman.getSharedFileList().getAllFileDescs();
	    for(FileDesc fd : fds) {
	        URN urn = fd.getSHA1Urn();
	        for(int j = 0; j < MAX_LOCATIONS + 5; j++) {
	            altLocManager.add(alternateLocationFactory.create("1.2.3." + j, urn), null);
	        }
	    }
        
        boolean checked = false;
		for(int i = 0; i < fman.getSharedFileList().getNumFiles(); i++) {
			FileDesc fd = fman.getSharedFileList().get(i);
			Response testResponse = responseFactory.createResponse(fd);
			String name = I18NConvert.instance().getNorm(fd.getFileName());
            
            char[] illegalChars = SearchSettings.ILLEGAL_CHARS.getValue();
            Arrays.sort(illegalChars);

            if (name.length() > SearchSettings.MAX_QUERY_LENGTH.getValue()
                    || StringUtils.containsCharacters(name, illegalChars)) {
                continue;
            }
            
			QueryRequest qr = queryRequestFactory.createQuery(name);
			Response[] hits = keywordIndex.query(qr);
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
        altLocManager.purge();
    }	
    
    /**
     * tests for the QRP thats kept by the FileManager 
     * tests that the function getQRP of FileManager returns
     * the correct table after addition, removal, and renaming
     * of shared files.
     */
    public void testFileManagerQRP() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        f1 = createNewNamedTestFile(10, "hello");
        f2 = createNewNamedTestFile(10, "\u5bae\u672c\u6b66\u8535\u69d8");
        f3 = createNewNamedTestFile(10, "\u00e2cc\u00e8nts");
        waitForLoad(); 

        //get the QRT from QRTUpdater
        QueryRouteTable qrt = qrpUpdater.getQRT();

        //test that QRT doesn't contain random keyword
        QueryRequest qr = queryRequestFactory.createQuery("asdfasdf");
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
        fman.removeFileIfSharedOrStore(f3);
        
        qrt = qrpUpdater.getQRT();
        
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
        FileManagerEvent result = renameFile(f2, f4);
        assertTrue(result.toString(), result.isRenameEvent());
        fman.renameFileIfSharedOrStore(f2, f4);
        qrt = qrpUpdater.getQRT();
        
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
        assertEquals(2,fman.getSharedFileList().getNumFiles());
        assertTrue(fman.getSharedFileList().contains(shared));
        assertTrue(fman.getSharedFileList().contains(sessionShared));
        assertFalse(fman.getSharedFileList().contains(notShared));
        assertNotNull(fman.getFileDescForFile(shared));
        assertNotNull(fman.getFileDescForFile(sessionShared));
        assertNull(fman.getFileDescForFile(notShared));
        
        // simulate restart
        fman = new FileManagerImpl(
                injector.getProvider(SimppManager.class),
                injector.getProvider(UrnCache.class),
                injector.getProvider(CreationTimeCache.class),
                injector.getProvider(ContentManager.class),
                injector.getProvider(AltLocManager.class),
                injector.getProvider(ActivityCallback.class), 
                injector.getInstance(
                        Key.get(ScheduledExecutorService.class, Names.named("backgroundExecutor"))),
                injector.getInstance(LimeXMLDocumentFactory.class),
                injector.getInstance(MetaDataReader.class),
                injector.getInstance(FileEventListenerProvider.class).fileEventListener());
        waitForLoad();
        
        //  assert that "shared" is shared
        assertEquals(1,fman.getSharedFileList().getNumFiles());
        assertTrue(fman.getSharedFileList().contains(shared));
        assertNotNull(fman.getFileDescForFile(shared));
        
        // but sessionShared is no more.
        assertFalse(fman.getSharedFileList().contains(sessionShared));
        assertFalse(fman.getSharedFileList().contains(notShared));
        assertNull(fman.getFileDescForFile(notShared));
        assertNull(fman.getFileDescForFile(notShared));
    }
    
    public void testSpecialApplicationShare() throws Exception{
        File specialShare = createNewNamedTestFile(10, "shared", SharingUtils.APPLICATION_SPECIAL_SHARE);
        MultiListener listener = new MultiListener();
        fman.addFileEventListener(listener);
        waitForLoad();
        //should not have dipatched an event for the folder
        for(FileManagerEvent fevt: listener.getFileManagerEventList()) {
            if(fevt.isAddFolderEvent()) {
                File[] files = fevt.getFiles();
                for(int i = 0 ; i < files.length; i++) {
                    if(files[i] != null) {
                        assertFalse(files[i].getParent().equals(SharingUtils.APPLICATION_SPECIAL_SHARE));
                    }
                }
            }
        }
        specialShare = createNewNamedTestFile(10, "shared2", SharingUtils.APPLICATION_SPECIAL_SHARE);
        FileManagerEvent evt = addFileForSession(specialShare);
        assertTrue(evt.isAddEvent());
        //should have shared file
        assertTrue(fman.getSharedFileList().contains(specialShare));
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
	    assertEquals(0, fman.getSharedFileList().getNumFiles());
	    assertTrue(fman.isLoadFinished());
	    
		// test if too large files are not shared
		File tooLarge = createFakeTestFile(MAX_FILE_SIZE+1l);
		FileManagerEvent result = addAlways(tooLarge);
		assertTrue(result.toString(), result.isFailedAddEvent());
		assertEquals(tooLarge, result.getFiles()[0]);
		
		// test if files in shared directories are still shared
		File test = createNewTestFile(5);
		result = addAlways(test);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(test, result.getFileDescs()[0].getFile());
		assertEquals(1, fman.getSharedFileList().getNumFiles());
		
		// try again, it will fail because it's already shared.
		result = addAlways(test);
		assertTrue(result.toString(), result.isAlreadySharedEvent());
		assertEquals(test, result.getFiles()[0]);
		
		// test that non-existent files are not shared
		test = new File("non existent file").getCanonicalFile();
		result = addAlways(test);
		assertTrue(result.toString(), result.isFailedAddEvent());
		assertEquals(test, result.getFiles()[0]);
		
		// test that file in non shared directory is shared
		File dir = createNewBaseDirectory("notshared");
		dir.mkdir();
		dir.deleteOnExit();
		test = createNewNamedTestFile(500, "specially shared", dir);
		result = addAlways(test);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(test, result.getFileDescs()[0].getFile());
        assertEquals(2, fman.getSharedFileList().getNumFiles());
        
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
		assertTrue(result.toString(), result.isFailedAddEvent());
		assertEquals(nonshareable, result.getFiles()[0]);
		nonshareable.delete();
		
		// not in shared directory and not specially shared, but valid extension
		SharingSettings.EXTENSIONS_TO_SHARE.setValue(SHARE_EXTENSION);
		waitForLoad(); // set the new extensions
		File validExt = createNewNamedTestFile(10, "valid extension", _sharedDir.getParentFile());
		result = addIfShared(validExt);
		assertTrue(result.toString(), result.isFailedAddEvent());
		assertEquals(validExt, result.getFiles()[0]);

		// nonexistent in shared directory
		File nonexistent = new File(_sharedDir, "test." + SHARE_EXTENSION);
		result = addIfShared(nonexistent);
		assertTrue(result.toString(), result.isFailedAddEvent());
		assertEquals(nonexistent, result.getFiles()[0]);
		
		// nonexistent in non shared directory
		nonexistent = new File("nonexistent." + SHARE_EXTENSION).getCanonicalFile();
		result = addIfShared(nonexistent);
		assertTrue(result.toString(), result.isFailedAddEvent());
		assertEquals(nonexistent, result.getFiles()[0]);
		
		// nonexistent, but specially shared
		result = addAlways(nonexistent);
		assertTrue(result.toString(), result.isFailedAddEvent());
		assertEquals(nonexistent, result.getFiles()[0]);
		
		// shareable files:
		
		// files with shareable extension in shared directory
		File shareable = createNewNamedTestFile(10, "shareable");
		result = addIfShared(shareable);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(shareable, result.getFileDescs()[0].getFile());
		assertEquals(1, fman.getSharedFileList().getNumFiles());
		assertEquals(result.getFileDescs()[0], fman.getSharedFileList().get(0));
		
		// files with shareable extension, specially shared
		File speciallyShareable = createNewNamedTestFile(10, "specially shareable", _sharedDir.getParentFile());
		result = addAlways(speciallyShareable);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(speciallyShareable, result.getFileDescs()[0].getFile());
		assertEquals(2, fman.getSharedFileList().getNumFiles());
		assertEquals(result.getFileDescs()[0], fman.getSharedFileList().get(1));
		
		// files with non shareable extension, specially shared
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("abc");
		File speciallyShared = createNewNamedTestFile(10, "speciall shared", _sharedDir.getParentFile());
		result = addAlways(speciallyShared);
		assertTrue(result.toString(), result.isAddEvent());
		assertEquals(speciallyShared, result.getFileDescs()[0].getFile());
		assertEquals(3, fman.getSharedFileList().getNumFiles());
		assertEquals(result.getFileDescs()[0], fman.getSharedFileList().get(2));
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
			assertEquals(0, fman.getSharedFileList().getFilesInDirectory(dir).size());
		}
		
		// assert others were shared
		for (File dir : whiteList) {
			assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dir).size());
		}
	}
	

    public void testSymlinksAreResolvedInBlacklist() throws Exception {
        if (OSUtils.isWindows()) {
            return;
        }
        
        File[] dirs = LimeTestUtils.createTmpDirs(
                "resolvedshare",
                "resolvedshare/resolvedsub",
                "resolvedshare/other/shared"
        );
        // create files in all folders, so we can see if they were shared
        for (int i = 0; i < dirs.length; i++) {
            createNewNamedTestFile(i + 1, "shared" + i, dirs[i]);
        }
        File[] pointedTo = LimeTestUtils.createTmpDirs(
                "notshared",
                "notshared/sub",
                "notshared/other/sub"
        );
        // create files in all folders, so we can see if they were shared
        for (int i = 0; i < dirs.length; i++) {
            createNewNamedTestFile(i + 1, "linkshared" + i, pointedTo[i]);
        }
        // add symlinks in shared folders to pointedTo
        for (int i = 0; i < dirs.length; i++) {
            createSymLink(dirs[i], "link", pointedTo[i]);
        }
        // create blacklist set
        Set<File> blackListSet = new HashSet<File>();
        for (File dir : dirs) {
            blackListSet.add(new File(dir, "link"));
        }
        fman.addSharedFolders(Collections.singleton(dirs[0]), blackListSet);
        waitForLoad();
        
        // assert blacklisted were not shared
        for (File excluded : blackListSet) {
            assertEquals("excluded was shared: " + excluded, 0, fman.getSharedFileList().getFilesInDirectory(excluded).size());
        }
        // same for pointed to
        for (File excluded : pointedTo) {
            assertEquals(0, fman.getSharedFileList().getFilesInDirectory(excluded).size());
        }
        // ensure other files were shared
        for (File shared: dirs) {
            assertEquals(1, fman.getSharedFileList().getFilesInDirectory(shared).size());
        }
        
        // clean up
        for (File dir : dirs) {
            cleanFiles(dir, true);
        }
        for (File dir : pointedTo) {
            cleanFiles(dir, true);
        }
    }
    
    private static void createSymLink(File parentDir, String name, File pointedTo) throws Exception {
        assertEquals(0, 
                Runtime.getRuntime().exec(new String[] { 
                        "ln", 
                        "-s",
                        pointedTo.getAbsolutePath(),
                        parentDir.getAbsolutePath() + File.separator + name
                }).waitFor());
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
        
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[0]).size());
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[1]).size());
        
        // Now unshare sub1
        fman.removeFolderIfShared(dirs[1]);
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[0]).size());
        assertEquals(0, fman.getSharedFileList().getFilesInDirectory(dirs[1]).size());
        
        // Now reload fman and make sure it's still not shared!
        FileManagerTestUtils.waitForLoad(fman,10000);

        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[0]).size());
        assertEquals(0, fman.getSharedFileList().getFilesInDirectory(dirs[1]).size());
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
        
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[0]).size());
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[1]).size());
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[2]).size());
        
        // Now unshare sub2
        fman.removeFolderIfShared(dirs[2]);
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[0]).size());
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[1]).size());
        assertEquals(0, fman.getSharedFileList().getFilesInDirectory(dirs[2]).size());
        assertFalse(fman.isFolderShared(dirs[2]));
        
        // Now reload fman and make sure it's still not shared!
        FileManagerTestUtils.waitForLoad(fman, 10000);

        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[0]).size());
        assertEquals(1, fman.getSharedFileList().getFilesInDirectory(dirs[1]).size());
        assertEquals(0, fman.getSharedFileList().getFilesInDirectory(dirs[2]).size());
    }

    
    /**
     * Tests whether the SharingUtils.isSensitiveDirectory(File) function is functioning properly. 
     */
    public void testSensitiveDirectoryPredicate() throws Exception {
        //  check defensive programming
        File file = null;
        assertFalse("null directory should not be a sensitive directory", SharingUtils.isSensitiveDirectory(file));
        file = new File("lksfjlsakjfsldfjak.slfkjs");
        assertFalse("random file should not be a sensitive directory", SharingUtils.isSensitiveDirectory(file));
        
        //  check that the user's home dir is a sensitive directory
        String userHome = System.getProperty("user.home");
        assertTrue("user's home directory should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(userHome)));
        
        //  check for OS-specific directories:
        String realOS = System.getProperty("os.name");
        
        try {
            
        setOSName("Windows XP");
        assertTrue(OSUtils.isWindowsXP());
        assertTrue("Documents and Settings should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Documents and Settings")));
        assertTrue("My Documents should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "My Documents")));
        assertTrue("Desktop should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Desktop")));
        assertTrue("Program Files should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Program Files")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Windows")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "WINNT")));
        
        setOSName("Windows NT");
        assertTrue(OSUtils.isWindowsNT());
        assertTrue("Documents and Settings should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Documents and Settings")));
        assertTrue("My Documents should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "My Documents")));
        assertFalse("My Documents in c:\\ should NOT be a sensitive directory", SharingUtils.isSensitiveDirectory(new File("c:\\Documents")));
        assertTrue("Desktop should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Desktop")));
        assertTrue("Program Files should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Program Files")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Windows")));
        assertFalse("Windows should not be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "NOTWINNT")));
                
        setOSName("Windows Vista");
        assertTrue(OSUtils.isWindowsVista());
        assertTrue("Documents and Settings should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Documents and Settings")));
        assertTrue("My Documents should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(userHome + File.separator + "Documents")));
        assertFalse("My Documents in c:\\ should NOT be a sensitive directory", SharingUtils.isSensitiveDirectory(new File("c:\\Documents")));
        assertTrue("Desktop should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Desktop")));
        assertTrue("Program Files should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Program Files")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Windows")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "WINNT")));

        
        setOSName("Windows 95");
        assertTrue(OSUtils.isWindows95());
        assertTrue("Documents and Settings should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Documents and Settings")));
        assertTrue("My Documents should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(userHome + File.separator + "My Documents")));
        assertTrue("Desktop should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(userHome+ File.separator + "Desktop")));
        assertTrue("Program Files should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Program Files")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Windows")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "WINNT")));
        
        setOSName("Windows 98");
        assertTrue(OSUtils.isWindows98());
        assertTrue("Documents and Settings should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Documents and Settings")));
        assertTrue("My Documents should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(userHome + File.separator + "My Documents")));
        assertTrue("Desktop should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Desktop")));
        assertTrue("Program Files should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Program Files")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Windows")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "WINNT")));
        
        setOSName("Windows ME");
        assertTrue(OSUtils.isWindowsMe());
        assertTrue("Documents and Settings should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Documents and Settings")));
        assertTrue("My Documents should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(userHome+ File.separator + "My Documents")));
        assertTrue("Desktop should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Desktop")));
        assertTrue("Program Files should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Program Files")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Windows")));
        assertTrue("Windows should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "WINNT")));
        
        setOSName("Mac OS X");
        assertTrue(OSUtils.isMacOSX());
        assertTrue("Users should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Users")));
        assertTrue("System should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "System")));
        assertTrue("System Folder should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "System Folder")));
        assertTrue("Previous Systems should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Previous Systems")));
        assertTrue("private should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "private")));
        assertTrue("Volumes should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Volumes")));
        assertTrue("Desktop should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Desktop")));
        assertTrue("Applications should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Applications")));
        assertTrue("Applications (Mac OS 9) should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Applications (Mac OS 9)")));
        assertTrue("Network should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "Network")));
        
        setOSName("Linux");
        assertTrue(OSUtils.isLinux());
        assertTrue("bin should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "bin")));
        assertTrue("boot should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "boot")));
        assertTrue("dev should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "dev")));
        assertTrue("etc should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "etc")));
        assertTrue("home should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "home")));
        assertTrue("mnt should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "mnt")));
        assertTrue("opt should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "opt")));
        assertTrue("proc should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "proc")));
        assertTrue("root should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "root")));
        assertTrue("sbin should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "sbin")));
        assertTrue("usr should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "usr")));
        assertTrue("var should be a sensitive directory", SharingUtils.isSensitiveDirectory(new File(File.separator + "var")));
        
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
        LimeXMLDocument document = injector.getInstance(LimeXMLDocumentFactory.class).createLimeXMLDocument("<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "  <audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
        "</audios>");

        f1 = createNewTestFile(1);
        f2 = createNewTestFile(3);
        waitForLoad();
        fman.getSharedFileList().get(0).addLimeXMLDocument(document);
        fman.getSharedFileList().get(1).addLimeXMLDocument(document);
        f3 = createNewTestFile(11);

        assertEquals(2, fman.getSharedFileList().getNumFiles());
        Iterator<FileDesc> it = fman.getIndexingIterator();
        FileDesc response = it.next();
        assertEquals(response.getFileName(), f1.getName());
        assertNotNull(response.getXMLDocument());
        response = it.next();
        assertEquals(response.getFileName(), f2.getName());
        assertNotNull(response.getXMLDocument());
        assertFalse(it.hasNext());
        try {
            response = it.next();
            fail("Expected NoSuchElementException, got: " + response);
        } catch (NoSuchElementException e) {
        }
        
        it = fman.getIndexingIterator();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        fman.removeFileIfSharedOrStore(f2);
        assertFalse(it.hasNext());

        it = fman.getIndexingIterator();
        response = it.next();
        assertNotNull(response.getXMLDocument());
        assertFalse(it.hasNext());
    }
    
    /**
     * Tests adding a single store file to the store directory which is not a shared directory.
     * Attempts various sharing of the store files
     */
    public void testAddOneStoreFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        
        waitForLoad();
        
        // create a store audio file with limexmldocument preventing sharing
        store1 = createNewTestStoreFile();
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        //create a file after the load
        store2 = createNewTestFile(5); 

        // fman should only have loaded store1
        assertEquals("Unexpected number of store files", 
            1, fman.getStoreFileList().getNumFiles());
            
        
        // it is important to check the query at all bounds,
        // including tests for case.
        // IMPORTANT: the store files should never show up in any of these
        responses=keywordIndex.query(queryRequestFactory.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery(
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);        
                
        
        // should not be able to unshared a store file thats not shared
        assertNull("Unexpected unsharing action", fman.stopSharingFile(store1));
        
        // should not be able to remove unadded file
        assertNull("should have not been able to remove f3", 
                   fman.removeFileIfSharedOrStore(store2));

        // try sharing the store file
        fman.addFileIfShared(store1);
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
        // try forcing the sharing
        fman.addFileAlways(store1);
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
        // try adding sharing for temp session
        fman.addFileForSession(store1);
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());


        // no files should be shareable
        List<FileDesc> files=fman.getSharedFileList().getFilesInDirectory(_storeDir);
        assertEquals("unexpected length of shared files", 0, files.size());
        files=fman.getSharedFileList().getFilesInDirectory(_storeDir.getParentFile());
        assertEquals("file manager listed shared files in file's parent dir",
            0, files.size());
    }
    
       
    /**
     * Creates a store folder with both store files and non store files. Since it is the selected download
     * directory of LWS and is not shared, only the LWS store songs should be displayed and none of the files
     * should be shared
     */
    public void testNonSharedStoreFolder() throws Exception { 
        assertEquals("Unexpected number of store files", 0, fman.getSharedFileList().getNumFiles());

        // load the files into the manager
        waitForLoad();
        
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        store2 = createNewTestStoreFile();
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d2);
        result = addIfShared(store2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        // create normal files in LWS directory (these are not in a shared directory)
        f1 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
        f2 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
        
        // fman should only have loaded the two store files into list
        assertEquals("Unexpected number of store files", 2, fman.getStoreFileList().getNumFiles());
        // fman should only have loaded two shared files
        assertEquals("Unexpected number of shared files",0, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
    }
    
    /**
     * Tests store files that are placed in a shared directory. They should NOT be shared
     * but should rather be extracted to the specialStoreFiles list instead
     */
    public void testSharedFolderWithStoreFiles() throws Exception { 
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        assertEquals("Unexpected number of store files", 0, fman.getStoreFileList().getNumFiles());

        // create normal share files
        f1 = createNewTestFile(4);
        f2 = createNewTestFile(5);
        // load the files into the manager
        waitForLoad();
        
        // create a file from LWS
        store1 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
        
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";

        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d2);
        result = addIfShared(store2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        

        // fman should only have loaded two shared files
        assertEquals("Unexpected number of shared files",2, fman.getSharedFileList().getNumFiles());
        // fman should only have loaded the two store files into list
        assertEquals("Unexpected number of individual store files",2,fman.getIndividualStoreFiles().length);
        assertEquals("Unexpected number of store files", 2, fman.getStoreFileList().getNumFiles());
            
        
        // it is important to check the query at all bounds,
        // including tests for case.
        // IMPORTANT: the store files should never show up in any of these
        responses=keywordIndex.query(queryRequestFactory.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery(
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);        
                
        
        // should not be able to unshared a store file thats not shared
        assertNull("Unexpected unsharing action", fman.stopSharingFile(store1));

        // try sharing the store file
        fman.addFileIfShared(store1);
        fman.addFileIfShared(store2);
        assertEquals("Unexpected number of shared files", 2, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
        // try forcing the sharing
        fman.addFileAlways(store1);
        fman.addFileAlways(store2);
        assertEquals("Unexpected number of shared files", 2, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
        // try adding sharing for temp session
        fman.addFileForSession(store1);
        fman.addFileForSession(store2);
        assertEquals("Unexpected number of shared files", 2, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());

        fman.addFileAlways(f1);
        fman.addFileAlways(f2);
        

        // no store files should be shareable in the file descriptors
        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_sharedDir);
        assertEquals("Unexpected length of shared files", 2, sharedFiles.size());
        assertNotEquals("Unexpected store file in share", sharedFiles.get(0).getFile(), store1);
        assertNotEquals("Unexpected store file in share", sharedFiles.get(0).getFile(), store2);
        assertNotEquals("Unexpected store file in share", sharedFiles.get(1).getFile(), store1);
        assertNotEquals("Unexpected store file in share", sharedFiles.get(1).getFile(), store2);
        
        // check the list of individual store files (only the two store files should be displayed)
        //  any LWS files loaded into a shared directory will be returned here
        File[] storeFiles = fman.getIndividualStoreFiles();
        assertEquals("Unexpected number of store files", 2, storeFiles.length);
        assertTrue("Unexpected store file", storeFiles[0].equals(store2) || storeFiles[0].equals(store1) );
        assertTrue("Unexpected store file", storeFiles[1].equals(store2) || storeFiles[1].equals(store1));

        sharedFiles=fman.getSharedFileList().getFilesInDirectory(_storeDir.getParentFile());
        assertEquals("file manager listed shared files in file's parent dir",
            0, sharedFiles.size());
    }
    
    //TODO: not sure how to run these now without mocking out FM
//    /**
//     * Creates store files in both the store folder and the shared folder, creates non store files
//     * in both the store folder and the shared folder. Initially only non-LWS files in the shared folder
//     * are shared and all LWS files are displayed. After sharing the store folder, all non-LWS files are
//     * shared and all store files remain unshared and displayed
//     */
//    public void testSharedFolderAlsoStoreFolder() throws Exception {
//        
//        assertEquals("Unexpected number of store files", 0, fman.getStoreFileList().getNumFiles());
//       
//        // create normal files in LWS directory (these are not in a shared directory)
//        f1 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
//        
//        // create normal share files
//        f2 = createNewTestFile(4);
//        f3 = createNewTestFile(5);
//
//        // load the files into the manager
//        waitForLoad();
//        
//        // create files in the store folder
//        store1 = createNewTestStoreFile();
//        // create a file from LWS in shared directory
//        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
//        
//
//        
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
////        String sharedAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\"";
//
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(store1, l1);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(d2);
//        result = addIfShared(store2, l2);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
////        LimeXMLDocument d3 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(sharedAudio));
////        List<LimeXMLDocument> l3 = new ArrayList<LimeXMLDocument>();
////        l3.add(d3);
////        result = addIfShared(f1, l3);
////        assertTrue(result.toString(), result.isAddEvent());
////        assertEquals(d3, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        // all three store files should be displayed
//        assertEquals("Unexpected number of store files", 2, fman.getStoreFileList().getNumFiles());
//        // fman should only have loaded two shared files
//        assertEquals("Unexpected number of shared files",2, fman.getSharedFileList().getNumFiles());
//        // one of the store files is in a shared directory so it is also individual store
//        assertEquals("Unexpected number of individual store files", 2, fman.getIndividualStoreFiles().length);
//        assertEquals("Unexpected number of inidividual share files", 0, fman.getIndividualFiles().length);
//        System.out.println("adding store directory to share");
//        // start sharing the store directory
//        fman.addSharedFolder(_storeDir);
//        
//        // all LWS files are displayed
//        assertEquals("Unexpected number of store files", 2, fman.getStoreFileList().getNumFiles());
//        // all non LWS files are shared
//        assertEquals("Unexpected number of shared files",3, fman.getSharedFileList().getNumFiles());
//        assertEquals("Unexpected number of individual store files", 2, fman.getIndividualStoreFiles().length);
//        assertEquals("Unexpected number of inidividual share files", 0, fman.getIndividualFiles().length);
//        
//        fman.removeFolderIfShared(_storeDir);
//        
//    }
//    
//    /**
//     * Tests what happens when LWS songs are located in a shared directory that is not
//     * the store directory. After unsharing that shared directory the store files are no
//     * longer visible
//     */
//    public void testUnshareFolderContainingStoreFiles() throws Exception {
//
//        // create a file from LWS in shared directory
//        f1 = createNewTestFile(4);
//        
//        // create a file from the LWS in the store directory
//        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _storeDir);
//        // load the files into the manager
//        waitForLoad();
//        
//        store1 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(store1, l1);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        // should only be sharing one file
//        assertEquals("Unexpected number of shared files", 1, fman.getSharedFileList().getNumFiles());
//        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
//       
//        // check lws files, individual store files
//        assertEquals("Unexpected number of store files", 2, fman.getStoreFileList().getNumFiles());
//        assertEquals("Unexpeected number of individual store files", 1, fman.getIndividualStoreFiles().length);
//        
//        // unshare the shared directory
//        fman.removeFolderIfShared(_sharedDir);       
//        
//        // should not share any files
//        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
//        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
//        
//        // check lws files, individual store files
//        assertEquals("Unexpected number of store files", 1, fman.getStoreFileList().getNumFiles());
//        assertEquals("Unexpected number of individual store files", 0, fman.getIndividualStoreFiles().length);
//        
//        fman.addSharedFolder(_sharedDir);
//    }
//    
//    /**
//     * Tests what files are displayed from the LWS when you switch to a new directory to save
//     * LWS downloads to. Previously displayed files in the old directory are no longer
//     * displayed, just LWS files in the new directory are displayed
//     */
//    public void testChangeStoreFolder() throws Exception {assertEquals("Unexpected number of store files", 0, fman.getStoreFileList().getNumFiles());
//        
//        // create alternative store directory to switch saving files to
//        File newStoreFolder = new File(_baseDir, "store2");
//        newStoreFolder.deleteOnExit();        
//        newStoreFolder.mkdirs();
//        
//        // create a file from LWS
//        store1 = createNewTestStoreFile();
//        store2 = createNewTestStoreFile();
//        
//        //create a file after the load
//        store3 = createNewNameStoreTestFile("FileManager_unit_store_test", newStoreFolder);
//        // load the files into the manager
//        waitForLoad();
//
//
//        // fman should only have loaded the two store files into list
//        assertEquals("Unexpected number of store files", 2, fman.getStoreFileList().getNumFiles());
//        // fman should only have loaded no shared files
//        assertEquals("Unexpected number of shared files",0, fman.getSharedFileList().getNumFiles());
//                
//        
//        // change the store save directory
//        SharingSettings.setSaveLWSDirectory(newStoreFolder);
//        // load the files into the manager
//        waitForLoad();
//
//       // fman should only have loaded the two store files into list
//        assertEquals("Unexpected number of store files", 1, fman.getStoreFileList().getNumFiles());
//        // fman should only have loaded two shared files
//        assertEquals("Unexpected number of shared files",0, fman.getSharedFileList().getNumFiles());
// 
//        // check the list of individual store files (only the two store files should be displayed)
//        //  any LWS files loaded into a shared directory will be returned here
//        File[] storeFiles = fman.getIndividualStoreFiles();
//        assertEquals("Unexpected number of store files", 0, storeFiles.length);
//        
//        SharingSettings.setSaveLWSDirectory(_storeDir);
//        newStoreFolder.delete();
//    }
    /**
     * Checks that removing a store file really removes the store file from the view
     */
    public void testRemoveOneStoreFile() throws Exception {
        
        waitForLoad();
        
        assertEquals(0, fman.getStoreFileList().getNumFiles()); 
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        store2 = createNewTestStoreFile();
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d2);
        result = addIfShared(store2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));

    
        //Remove file that's shared.  Back to 1 file.                   
        assertEquals(2, fman.getStoreFileList().getNumFiles());     
        assertNotNull("should have been able to remove shared file", fman.removeFileIfSharedOrStore(store2));
        assertEquals("unexpected number of files", 1, fman.getStoreFileList().getNumFiles());
        assertNull(fman.getFileDescForFile(store2));
    }

    
    /**
     * Creates store files in both a shared directory and the store directory
     * and tries to explicitly share them
     */
    public void testAddFileAlwaysStoreFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        waitForLoad();
        
        assertEquals(0, fman.getStoreFileList().getNumFiles()); 
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        store2 = createNewTestStoreFile();
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d2);
        result = addIfShared(store2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));

        // try sharing the store file
        fman.addFileIfShared(store1);
        fman.addFileIfShared(store2);
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
        // try forcing the sharing
        fman.addFileAlways(store1);
        fman.addFileAlways(store2);
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
        // try adding sharing for temp session
        fman.addFileForSession(store1);
        fman.addFileForSession(store2);
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumForcedFiles());
        
        // it is important to check the query at all bounds,
        // including tests for case.
        // IMPORTANT: the store files should never show up in any of these
        responses=keywordIndex.query(queryRequestFactory.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=keywordIndex.query(queryRequestFactory.createQuery(
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);  
    }

    //TODO: Not sure how to test this now without mocking out parts of FM
//    /**
//     * Try renaming a file in the store
//     */
//    public void testRenameStoreFile() throws Exception {
//        
//        waitForLoad();
//        
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//
//        // create a file from LWS
//        store1 = createNewTestStoreFile();
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(store1, l1);
//        assertTrue(result.toString(), result.isAddEvent());
//        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//
//        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
//        assertEquals("Unexpected number of store files", 1, fman.getStoreFileList().getNumFiles());
//        
//        // create a third store file but it not added anywhere
//        store3 = createNewTestStoreFile();
//    
//        // try renaming unadded file, should fail
//        result = renameFile(store3, new File("c:\\asdfoih.mp3"));
//        assertTrue(result.toString(), result.getType() == FileManagerEvent.Type.RENAME_FILE_FAILED);
//
//        // rename a valid store file
//        result = renameFile(store1, store3);
//        assertTrue(result.toString(), result.isRenameEvent());
//        assertEquals("Unexpected number of shared files", 0, fman.getSharedFileList().getNumFiles());
//        assertEquals("Unexpected number of store files", 1, fman.getStoreFileList().getNumFiles());
//        assertEquals("Unexpected file renamed", store1, result.getFileDescs()[0].getFile());
//        assertEquals("Unexpected file added", store3, result.getFileDescs()[1].getFile());
//
//        // renamed file should not be found, new name file should be found
//        assertFalse(fman.getStoreFileList().contains(store1));
//        assertTrue(fman.getStoreFileList().contains(store3));
//        // still only two store files
//        assertEquals("Unexpected number of store files", 1, fman.getStoreFileList().getNumFiles());
//    }
    
    // end Store Files Test
    
    public void testMetaQueriesWithConflictingMatches() throws Exception {
        waitForLoad();
        
        // test a query where the filename is meaningless but XML matches.
        File f1 = createNewNamedTestFile(10, "meaningless");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(
            "artist=\"Sammy B\" album=\"Jazz in G\""));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>(); 
        l1.add(d1);
        FileManagerEvent result = addIfShared(f1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        Response[] r1 = keywordIndex.query(queryRequestFactory.createQuery("sam",
                                    buildAudioXMLString("artist=\"sam\"")));
        assertNotNull(r1);
        assertEquals(1, r1.length);
        assertEquals(d1.getXMLString(), r1[0].getDocument().getXMLString());
        
        // test a match where 50% matches -- should get no matches.
        Response[] r2 = keywordIndex.query(queryRequestFactory.createQuery("sam jazz in c",
                                   buildAudioXMLString("artist=\"sam\" album=\"jazz in c\"")));
        assertNotNull(r2);
        assertEquals(0, r2.length);
            
            
        // test where the keyword matches only.
        Response[] r3 = keywordIndex.query(queryRequestFactory.createQuery("meaningles"));
        assertNotNull(r3);
        assertEquals(1, r3.length);
        assertEquals(d1.getXMLString(), r3[0].getDocument().getXMLString());
                                  
        // test where keyword matches, but xml doesn't.
        Response[] r4 = keywordIndex.query(queryRequestFactory.createQuery("meaningles",
                                   buildAudioXMLString("artist=\"bob\"")));
        assertNotNull(r4);
        assertEquals(0, r4.length);
            
        // more ambiguous tests -- a pure keyword search for "jazz in d"
        // will work, but a keyword search that included XML will fail for
        // the same.
        File f2 = createNewNamedTestFile(10, "jazz in d");
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(
            "album=\"jazz in e\""));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>(); l2.add(d2);
        result = addIfShared(f2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        // pure keyword.
        Response[] r5 = keywordIndex.query(queryRequestFactory.createQuery("jazz in d"));
        assertNotNull(r5);
        assertEquals(1, r5.length);
        assertEquals(d2.getXMLString(), r5[0].getDocument().getXMLString());
        
        // keyword, but has XML to check more efficiently.
        Response[] r6 = keywordIndex.query(queryRequestFactory.createQuery("jazz in d",
                                   buildAudioXMLString("album=\"jazz in d\"")));
        assertNotNull(r6);
        assertEquals(0, r6.length);
                            
        
                                   
    }
    
    public void testMetaQueriesStoreFiles() throws Exception{
    
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        
        waitForLoad();
        
        // create a store audio file with limexmldocument preventing sharing
        File f1 = createNewNamedTestFile(12, "small town hero");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(f1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        //create a query with just a file name match, should get no responses
        Response[] r0 = keywordIndex.query(queryRequestFactory.createQuery("small town hero"));
        assertNotNull(r0);
        assertEquals(0, r0.length);
        
        // create a query where keyword matches and partial xml matches, should get no
        // responses
        Response[] r1 = keywordIndex.query(queryRequestFactory.createQuery("small town hero",
                                    buildAudioXMLString("title=\"Alive\"")));    
        assertNotNull(r1);
        assertEquals(0, r1.length);
        
        // test 100% matches, should get no results
        Response[] r2 = keywordIndex.query(queryRequestFactory.createQuery("small town hero",
                                   buildAudioXMLString(storeAudio)));
        assertNotNull(r2);
        assertEquals(0, r2.length);
        
        // test xml matches 100% but keyword doesn't, should get no matches
        Response[] r3 = keywordIndex.query(queryRequestFactory.createQuery("meaningless",
                                   buildAudioXMLString(storeAudio)));
        assertNotNull(r3);
        assertEquals(0, r3.length);
        
        //test where nothing matches, should get no results
        Response[] r4 = keywordIndex.query(queryRequestFactory.createQuery("meaningless",
                                   buildAudioXMLString("title=\"some title\" artist=\"unknown artist\" album=\"this album name\" genre=\"Classical\"")));
        assertNotNull(r4);
        assertEquals(0, r4.length);
        
        
        // create a store audio file with xmldocument preventing sharing with video xml attached also
        File f2 = createNewNamedTestFile(12, "small town hero 2");
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        LimeXMLDocument d3 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString("director=\"francis loopola\" title=\"Alive\""));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d3);
        l2.add(d2);
        FileManagerEvent result2 = addIfShared(f2, l2);
        assertTrue(result2.toString(), result2.isAddEvent());
            
          //create a query with just a file name match, should get no responses
        Response[] r5 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2"));
        assertNotNull(r5);
        assertEquals(0, r5.length);
        
        // query with videoxml matching. This SHOULDNT return results. The new Meta-data parsing
        //  is fixed to disallow adding new XML docs to files
        Response[] r6 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2",
                                    buildVideoXMLString("director=\"francis loopola\" title=\"Alive\"")));
        assertNotNull(r6);
        assertEquals(0, r6.length);
        
        // query with videoxml partial matching. This in SHOULDNT return results. The new Meta-data parsing
        //  is fixed to disallow adding new XML docs to files, this in theory shouldn't be 
        //  possible
        Response[] r7 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2",
                                    buildVideoXMLString("title=\"Alive\"")));
        assertNotNull(r7);
        assertEquals(0, r7.length);
        
        // test 100% matches minus VideoXxml, should get no results
        Response[] r8 = keywordIndex.query(queryRequestFactory.createQuery("small town hero 2",
                                    buildAudioXMLString(storeAudio)));
        assertNotNull(r8);
        assertEquals(0, r8.length);
        
        fman.removeFileIfSharedOrStore(f2);
    }

    public void testMetaQRT() throws Exception {
        String dir2 = "director=\"francis loopola\"";

        File f1 = createNewNamedTestFile(10, "hello");
        QueryRouteTable qrt = qrpUpdater.getQRT();
        assertFalse("should not be in QRT", qrt.contains(get_qr(f1)));
        waitForLoad();
        
        //make sure QRP contains the file f1
        qrt = qrpUpdater.getQRT();
        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));

        //now test xml metadata in the QRT
        File f2 = createNewNamedTestFile(11, "metadatafile2");
        LimeXMLDocument newDoc2 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(newDoc2);
        
        FileManagerEvent result = addIfShared(f2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(newDoc2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        qrt = qrpUpdater.getQRT();
        
        assertTrue("expected in QRT", qrt.contains (get_qr(buildVideoXMLString(dir2))));
        assertFalse("should not be in QRT", qrt.contains(get_qr(buildVideoXMLString("director=\"sasami juzo\""))));
        
        //now remove the file and make sure the xml gets deleted.
        fman.removeFileIfSharedOrStore(f2);
        qrt = qrpUpdater.getQRT();
       
        assertFalse("should not be in QRT", qrt.contains(get_qr(buildVideoXMLString(dir2))));
    }
    
    public void testMetaQRTStoreFiles() throws Exception {
        
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        
        // share a file
        File f1 = createNewNamedTestFile(10, "hello");
        QueryRouteTable qrt = qrpUpdater.getQRT();
        assertFalse("should not be in QRT", qrt.contains(get_qr(f1)));
        waitForLoad();
        
        //make sure QRP contains the file f1
        qrt = qrpUpdater.getQRT();
        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));
        
        // create a store audio file with xml preventing sharing
        File f2 = createNewNamedTestFile(12, "small town hero");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        
        FileManagerEvent result = addIfShared(f2, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        qrt = qrpUpdater.getQRT();
        
        assertFalse("should not be in QRT", qrt.contains (get_qr(buildAudioXMLString(storeAudio))));
   
        waitForLoad();
   
        //store file should not be in QRT table
        qrt = qrpUpdater.getQRT();
        assertFalse("should not be in QRT", qrt.contains (get_qr(buildAudioXMLString(storeAudio))));
        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));
    }

    public void testMetaQueries() throws Exception {
        waitForLoad();
        String dir1 = "director=\"loopola\"";

        //make sure there's nothing with this xml query
        Response[] res = keywordIndex.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        
        assertEquals("there should be no matches", 0, res.length);
        
        File f1 = createNewNamedTestFile(10, "test_this");
        
        LimeXMLDocument newDoc1 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir1));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(newDoc1);


        String dir2 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f2 = createNewNamedTestFile(11, "hmm");

        LimeXMLDocument newDoc2 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(newDoc2);

        
        String dir3 = "director=\"\u5bae\u672c\u6b66\u8535\u69d8\"";
        File f3 = createNewNamedTestFile(12, "testtesttest");
        
        LimeXMLDocument newDoc3 = 
            limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir3));
        List<LimeXMLDocument> l3 = new ArrayList<LimeXMLDocument>();
        l3.add(newDoc3);
        
        //add files and check they are returned as responses
        FileManagerEvent result = addIfShared(f1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(newDoc1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        result = addIfShared(f2, l2);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(newDoc2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        result = addIfShared(f3, l3);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(newDoc3, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        Thread.sleep(100);
        res = keywordIndex.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be one match", 1, res.length);

        res = keywordIndex.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);
        
        //remove a file
        fman.removeFileIfSharedOrStore(f1);

        res = keywordIndex.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be no matches", 0, res.length);
        
        //make sure the two other files are there
        res = keywordIndex.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);

        //remove another and check we still have on left
        fman.removeFileIfSharedOrStore(f2);
        res = keywordIndex.query(queryRequestFactory.createQuery("",buildVideoXMLString(dir3)));
        assertEquals("there should be one match", 1, res.length);

        //remove the last file and make sure we get no replies
        fman.removeFileIfSharedOrStore(f3);
        res = keywordIndex.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir3)));
        assertEquals("there should be no matches", 0, res.length);
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
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        String norm = I18NConvert.instance().getNorm(f.getName());
        norm = StringUtils.replace(norm, "_", " ");
        return queryRequestFactory.createQuery(norm);
    }
	
	protected void addFilesToLibrary() throws Exception {
		String dirString = "com/limegroup/gnutella";
		File testDir = TestUtils.getResourceFile(dirString);
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
			    _sharedDir, testFiles[i].getName() + "." + SHARE_EXTENSION);
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
            testFiles.length, fman.getSharedFileList().getNumFiles() );
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
		File file = File.createTempFile(name, "." + SHARE_EXTENSION, directory);
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

    protected void waitForLoad() throws Exception {
        FileManagerTestUtils.waitForLoad(fman, 10000);
    }    
    
    public static class Listener implements FileEventListener {
        public FileManagerEvent evt;
        public synchronized void handleFileEvent(FileManagerEvent fme) {
            switch(fme.getType()) {
                case LOAD_FILE:
                case REMOVE_URN:
                case REMOVE_FD:
                    return;
            }

            evt = fme;
            notify();
            fme.getFileManager().removeFileEventListener(this);
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
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addFileIfShared(f, LimeXMLDocument.EMPTY_LIST);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent addIfShared(File f, List<LimeXMLDocument> l) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addFileIfShared(f, l);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent addAlways(File f) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addFileAlways(f);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent renameFile(File f1, File f2) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.renameFileIfSharedOrStore(f1, f2);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent addFileForSession(File f1) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addFileForSession(f1);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected FileManagerEvent fileChanged(File f1) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized (fel) {
            fman.fileChanged(f1);
            fel.wait(5000);
        }
        return fel.evt;
    }
    
    protected void assertSharedFiles(List<FileDesc> shared, File... expected) {
        List<File> files = new ArrayList<File>(shared.size());
        synchronized(shared) {
            for(FileDesc fd: shared) {
                files.add(fd.getFile());
            }
        }
        assertEquals(files.size(), expected.length);
        for(File file : expected) {
            assertTrue(files.contains(file));
            files.remove(file);
        }
        assertTrue(files.toString(), files.isEmpty());
    }
    
    private QueryRequest get_qr(String xml) {
        return queryRequestFactory.createQuery("", xml);
    }

    // build xml string for video
    private String buildVideoXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\"><video " 
            + keyname 
            + "></video></videos>";
    }
    
    private String buildAudioXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio " 
            + keyname 
            + "></audio></audios>";
    }    
    
    protected File createNewTestStoreFile() throws Exception{
        return createNewNameStoreTestFile("FileManager_unit_store_test", _storeDir);
    }
    
    protected File createNewNameStoreTestFile(String name, File directory) throws Exception { 
        File file = File.createTempFile(name, ".mp3", directory);
        file.deleteOnExit();

        OutputStream out = new FileOutputStream(file);
        out.write(new byte[5]);
        out.flush();
        out.close();

        return FileUtils.getCanonicalFile(file);
    }
}


