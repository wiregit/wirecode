package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.FileManagerTest;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

/**
 * Tests the MetaFileManager.  Subclass FileManagerTest so that
 * the same tests ran for SimpleFileManager can be run for 
 * MetaFileManager.
 */
public class MetaFileManagerTest extends FileManagerTest {

    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    private QueryRequestFactory queryRequestFactory;
    
    public MetaFileManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MetaFileManagerTest.class);
    }
    
    @Override
	protected void setUp() throws Exception {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue(EXTENSION);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
       
	    cleanFiles(_sharedDir, false);
	    
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        });

        injector.getInstance(Acceptor.class).setAddress(InetAddress.getLocalHost());

        fman = (FileManagerImpl)injector.getInstance(FileManager.class);

        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }
    
    //TODO: These Store File tests should really be part of FileManagerTest
    //      The problem is we need to mock out fake store files without actually reading
    //      real files to save overhead. To do this we need to fake xml data which can't
    //      be done in FileManagerTest without mocking out FileManagerImpl 
    
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
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        //create a file after the load
        store2 = createNewTestFile(5); 

        // fman should only have loaded store1
        assertEquals("Unexpected number of store files", 
            1, fman.getNumStoreFiles());
            
        
        // it is important to check the query at all bounds,
        // including tests for case.
        // IMPORTANT: the store files should never show up in any of these
        responses=fman.query(queryRequestFactory.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery(
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);        
                
        
        // should not be able to unshared a store file thats not shared
        assertNull("Unexpected unsharing action", fman.stopSharingFile(store1));
        
        // should not be able to remove unadded file
        assertNull("should have not been able to remove f3", 
                   fman.removeFileIfSharedOrStore(store2));

        // try sharing the store file
        fman.addFileIfShared(store1);
        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // try forcing the sharing
        fman.addFileAlways(store1);
        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // try adding sharing for temp session
        fman.addFileForSession(store1);
        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());


        // no files should be shareable
        List<FileDesc> files=fman.getSharedFilesInDirectory(_storeDir);
        assertEquals("unexpected length of shared files", 0, files.size());
        files=fman.getSharedFilesInDirectory(_storeDir.getParentFile());
        assertEquals("file manager listed shared files in file's parent dir",
            0, files.size());
    }
    
    /**
     * Creates a store folder with both store files and non store files. Since it is the selected download
     * directory of LWS and is not shared, only the LWS store songs should be displayed and none of the files
     * should be shared
     */
    public void testNonSharedStoreFolder() throws Exception { 
        assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());

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
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        store2 = createNewTestStoreFile();
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d2);
        result = addIfShared(store2, l2);
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        // create normal files in LWS directory (these are not in a shared directory)
        f1 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
        f2 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
        
        // fman should only have loaded the two store files into list
        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
        // fman should only have loaded two shared files
        assertEquals("Unexpected number of shared files",0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
    }
    
//      TODO: these tests need to be fixed. The only way I can think to fix them properlly is to 
//              read a local file with store metadata in it
//    /**
//     * Tests store files that are placed in a shared directory. They should NOT be shared
//     * but should rather be extracted to the specialStoreFiles list instead
//     */
//    public void testSharedFolderWithStoreFiles() throws Exception { 
//        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
//        
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
//        
//        // create normal share files
//        f1 = createNewTestFile(4);
//        f2 = createNewTestFile(5);
//        
//        // load the files into the manager
//        waitForLoad();
//        
//        assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());
//        // create a file from LWS
//        store1 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(store1, l1);
////        assertTrue(result.toString(), result.isAddStoreEvent());
////        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
//        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(d2);
//        result = addIfShared(store2, l2);
////        assertTrue(result.toString(), result.isAddStoreEvent());
////        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//
//
//        // fman should only have loaded the two store files into list
////        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
//        // fman should only have loaded two shared files
//        assertEquals("Unexpected number of shared files",2, fman.getNumFiles());
//            
//        
//        // it is important to check the query at all bounds,
//        // including tests for case.
//        // IMPORTANT: the store files should never show up in any of these
//        responses=fman.query(queryRequestFactory.createQuery("unit",(byte)3));
//        assertEquals("Unexpected number of responses", 2, responses.length);
//        responses=fman.query(queryRequestFactory.createQuery("FileManager", (byte)3));
//        assertEquals("Unexpected number of responses", 2, responses.length);
//        responses=fman.query(queryRequestFactory.createQuery("test", (byte)3));
//        assertEquals("Unexpected number of responses", 2, responses.length);
//        responses=fman.query(queryRequestFactory.createQuery("file", (byte)3));
//        assertEquals("Unexpected number of responses", 2, responses.length);
//        responses=fman.query(queryRequestFactory.createQuery(
//            "FileManager UNIT tEsT", (byte)3));
//        assertEquals("Unexpected number of responses", 2, responses.length);        
//                
//        
//        // should not be able to unshared a store file thats not shared
//        assertNull("Unexpected unsharing action", fman.stopSharingFile(store1));
//
//        // try sharing the store file
//        fman.addFileIfShared(store1);
//        fman.addFileIfShared(store2);
//        assertEquals("Unexpected number of shared files", 2, fman.getNumFiles());
//        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
//        
//        // try forcing the sharing
//        fman.addFileAlways(store1);
//        fman.addFileAlways(store2);
//        assertEquals("Unexpected number of shared files", 2, fman.getNumFiles());
//        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
//        
//        // try adding sharing for temp session
//        fman.addFileForSession(store1);
//        fman.addFileForSession(store2);
//        assertEquals("Unexpected number of shared files", 2, fman.getNumFiles());
//        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
//
//        fman.addFileAlways(f1);
//        fman.addFileAlways(f2);
//        
//
//        // no store files should be shareable in the file descriptors
//        List<FileDesc> files=fman.getSharedFilesInDirectory(_sharedDir);
//        assertEquals("Unexpected length of shared files", 2, files.size());
//        assertNotEquals("Unexpected store file in share", files.get(0).getFile(), store1);
//        assertNotEquals("Unexpected store file in share", files.get(0).getFile(), store2);
//        assertNotEquals("Unexpected store file in share", files.get(1).getFile(), store1);
//        assertNotEquals("Unexpected store file in share", files.get(1).getFile(), store2);
//        
//        // check the list of individual store files (only the two store files should be displayed)
//        //  any LWS files loaded into a shared directory will be returned here
//        File[] storeFiles = fman.getIndividualStoreFiles();
//        assertEquals("Unexpected number of store files", 2, storeFiles.length);
//        assertTrue("Unexpected store file", storeFiles[0].equals(store2) || storeFiles[0].equals(store1) );
//        assertTrue("Unexpected store file", storeFiles[1].equals(store2) || storeFiles[1].equals(store1));
//
//        files=fman.getSharedFilesInDirectory(_storeDir.getParentFile());
//        assertEquals("file manager listed shared files in file's parent dir",
//            0, files.size());
//    }
//    
//    /**
//     * Creates store files in both the store folder and the shared folder, creates non store files
//     * in both the store folder and the shared folder. Initially only non-LWS files in the shared folder
//     * are shared and all LWS files are displayed. After sharing the store folder, all non-LWS files are
//     * shared and all store files remain unshared and displayed
//     */
//    public void testSharedFolderAlsoStoreFolder() throws Exception {
//        
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
//        String storeAudio3 = "title=\"Alive3\" artist=\"small town hero\" album=\"yet another album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2005\"";
//        
//        
//        assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());
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
//        // create a file from LWS
//        store1 = createNewTestStoreFile();
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(store1, l1);
//        assertTrue(result.toString(), result.isAddStoreEvent());
//        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        store2 = createNewTestStoreFile();
//        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(d2);
//        result = addIfShared(store2, l2);
//        assertTrue(result.toString(), result.isAddStoreEvent());
//        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        // create a file from LWS in shared directory
//        store3 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
//        LimeXMLDocument d3 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio3));
//        List<LimeXMLDocument> l3 = new ArrayList<LimeXMLDocument>();
//        l3.add(d3);
//        result = addIfShared(store3, l3);
////        assertTrue(result.toString(), result.isAddStoreEvent());
////        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        // all three store files should be displayed
//        assertEquals("Unexpected number of store files", 3, fman.getNumStoreFiles());
//        // fman should only have loaded two shared files
//        assertEquals("Unexpected number of shared files",2, fman.getNumFiles());
//        // one of the store files is in a shared directory so it is also individual store
//        assertEquals("Unexpected number of individual store files", 1, fman.getIndividualStoreFiles().length);
//        assertEquals("Unexpected number of inidividual share files", 0, fman.getIndividualFiles().length);
//        
//        // start sharing the store directory
//        fman.addSharedFolder(_storeDir);
//        // load the files into the manager
//        waitForLoad();
//        
//        // all LWS files are displayed
//        assertEquals("Unexpected number of store files", 3, fman.getNumStoreFiles());
//        // all non LWS files are shared
//        assertEquals("Unexpected number of shared files",3, fman.getNumFiles());
//        assertEquals("Unexpected number of individual store files", 3, fman.getIndividualStoreFiles().length);
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
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
//        
//        
//        // create a file from LWS in shared directory
////        store1 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
//        f1 = createNewTestFile(4);
//        
//        waitForLoad();
//        
//        // create a file from the LWS in the store directory
////        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _storeDir);
//        // create a file from LWS
//        store2 = createNewTestStoreFile();
//        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(d2);
//        FileManagerEvent result = addIfShared(store2, l2);
//        assertTrue(result.toString(), result.isAddStoreEvent());
//        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        // load the files into the manager
//
//        
//        // should only be sharing one file
//        assertEquals("Unexpected number of shared files", 1, fman.getNumFiles());
//        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
//       
//        // check lws files, individual store files
//        assertEquals("Unexpected number of store files", 1, fman.getNumStoreFiles());
//        assertEquals("Unexpeected number of individual store files", 1, fman.getIndividualStoreFiles().length);
//        
//        // unshare the shared directory
//        fman.removeFolderIfShared(_sharedDir);       
//        // load the files into the manager
////        waitForLoad();
//        
//        // should not share any files
//        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
//        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
//        
//        // check lws files, individual store files
//        assertEquals("Unexpected number of store files", 1, fman.getNumStoreFiles());
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
//    public void testChangeStoreFolder() throws Exception {assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());
//        
//        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
//        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
//        String storeAudio3 = "title=\"Alive3\" artist=\"small town hero\" album=\"yet another album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2005\"";
//    
//        // load the files into the manager
//        waitForLoad();
//    
//        // create alternative store directory to switch saving files to
//        File newStoreFolder = new File(_baseDir, "store2");
//        newStoreFolder.deleteOnExit();        
//        newStoreFolder.mkdirs();
//        
//        // create a file from LWS
//        store1 = createNewTestStoreFile();
//        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
//        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
//        l1.add(d1);
//        FileManagerEvent result = addIfShared(store1, l1);
//        assertTrue(result.toString(), result.isAddStoreEvent());
//        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//        store2 = createNewTestStoreFile();
//        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
//        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
//        l2.add(d2);
//        result = addIfShared(store2, l2);
//        assertTrue(result.toString(), result.isAddStoreEvent());
//        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
//        
//
//
//
//        // fman should only have loaded the two store files into list
//        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
//        // fman should only have loaded no shared files
//        assertEquals("Unexpected number of shared files",0, fman.getNumFiles());
//                
//        
//        // change the store save directory
//        SharingSettings.setSaveLWSDirectory(newStoreFolder);
//        // load the files into the manager
//        waitForLoad();
//        
//        //create a file after the load
//        store3 = createNewNameStoreTestFile("FileManager_unit_store_test", newStoreFolder);
//        LimeXMLDocument d3 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio3));
//        List<LimeXMLDocument> l3 = new ArrayList<LimeXMLDocument>();
//        l2.add(d3);
//        result = addIfShared(store3, l3);
//
//       // fman should only have loaded the two store files into list
//        assertEquals("Unexpected number of store files", 1, fman.getNumStoreFiles());
//        // fman should only have loaded two shared files
//        assertEquals("Unexpected number of shared files",0, fman.getNumFiles());
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
        
        assertEquals(0, fman.getNumStoreFiles()); 
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        store2 = createNewTestStoreFile();
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d2);
        result = addIfShared(store2, l2);
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));

    
        //Remove file that's shared.  Back to 1 file.                   
        assertEquals(2, fman.getNumStoreFiles());     
        assertNotNull("should have been able to remove shared file", fman.removeFileIfSharedOrStore(store2));
        assertEquals("unexpected number of files", 1, fman.getNumStoreFiles());
        assertNull(fman.getFileDescForFile(store2));
    }

    
    /**
     * Creates store files in both a shared directory and the store directory
     * and tries to explicitly share them
     */
    public void testAddFileAlwaysStoreFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        waitForLoad();
        
        assertEquals(0, fman.getNumStoreFiles()); 
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        String storeAudio2 = "title=\"Alive\" artist=\"small town hero\" album=\"some other album name\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2006\"";
        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        
        store2 = createNewTestStoreFile();
        LimeXMLDocument d2 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(d2);
        result = addIfShared(store2, l2);
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));

        // try sharing the store file
        fman.addFileIfShared(store1);
        fman.addFileIfShared(store2);
        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // try forcing the sharing
        fman.addFileAlways(store1);
        fman.addFileAlways(store2);
        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // try adding sharing for temp session
        fman.addFileForSession(store1);
        fman.addFileForSession(store2);
        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // it is important to check the query at all bounds,
        // including tests for case.
        // IMPORTANT: the store files should never show up in any of these
        responses=fman.query(queryRequestFactory.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);
        responses=fman.query(queryRequestFactory.createQuery(
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 0, responses.length);  
    }
    
    /**
     * Try renaming a file in the store
     */
    public void testRenameStoreFile() throws Exception {
        
        waitForLoad();
        
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";

        // create a file from LWS
        store1 = createNewTestStoreFile();
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(store1, l1);
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));

        // create a third store file but it not added anywhere
        store3 = createNewTestStoreFile();
    
        // try renaming unadded file, should fail
        result = renameFile(store3, new File("c:\\asdfoih.mp3"));
        assertTrue(result.toString(), result.isFailedEvent());

        // rename a valid store file
        result = renameFile(store1, store3);
        assertTrue(result.toString(), result.isRenameEvent());
        assertEquals("Unexpected number of store files", 1, fman.getNumStoreFiles());
        assertEquals("Unexpected file renamed", store1, result.getFileDescs()[0].getFile());
        assertEquals("Unexpected file added", store3, result.getFileDescs()[1].getFile());

        // renamed file should not be found, new name file should be found
        assertFalse(fman.isStoreFileLoaded(store1));
        assertTrue(fman.isStoreFileLoaded(store3));
        // still only two store files
        assertEquals("Unexpected number of store files", 1, fman.getNumStoreFiles());
    }

    
    // TODO: end Store Files Test
	
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
	    
	    Response[] r1 = fman.query(queryRequestFactory.createQuery("sam",
	                                buildAudioXMLString("artist=\"sam\"")));
        assertNotNull(r1);
        assertEquals(1, r1.length);
        assertEquals(d1.getXMLString(), r1[0].getDocument().getXMLString());
        
        // test a match where 50% matches -- should get no matches.
        Response[] r2 = fman.query(queryRequestFactory.createQuery("sam jazz in c",
                                   buildAudioXMLString("artist=\"sam\" album=\"jazz in c\"")));
        assertNotNull(r2);
        assertEquals(0, r2.length);
            
            
        // test where the keyword matches only.
        Response[] r3 = fman.query(queryRequestFactory.createQuery("meaningles"));
        assertNotNull(r3);
        assertEquals(1, r3.length);
        assertEquals(d1.getXMLString(), r3[0].getDocument().getXMLString());
                                  
        // test where keyword matches, but xml doesn't.
        Response[] r4 = fman.query(queryRequestFactory.createQuery("meaningles",
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
        Response[] r5 = fman.query(queryRequestFactory.createQuery("jazz in d"));
        assertNotNull(r5);
        assertEquals(1, r5.length);
        assertEquals(d2.getXMLString(), r5[0].getDocument().getXMLString());
        
        // keyword, but has XML to check more efficiently.
        Response[] r6 = fman.query(queryRequestFactory.createQuery("jazz in d",
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
		assertTrue(result.toString(), result.isAddStoreEvent());
		assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
		
		//create a query with just a file name match, should get no responses
        Response[] r0 = fman.query(queryRequestFactory.createQuery("small town hero"));
        assertNotNull(r0);
        assertEquals(0, r0.length);
		
		// create a query where keyword matches and partial xml matches, should get no
        // responses
	    Response[] r1 = fman.query(queryRequestFactory.createQuery("small town hero",
                                    buildAudioXMLString("title=\"Alive\"")));    
        assertNotNull(r1);
        assertEquals(0, r1.length);
        
        // test 100% matches, should get no results
        Response[] r2 = fman.query(queryRequestFactory.createQuery("small town hero",
                                   buildAudioXMLString(storeAudio)));
        assertNotNull(r2);
        assertEquals(0, r2.length);
        
        // test xml matches 100% but keyword doesn't, should get no matches
        Response[] r3 = fman.query(queryRequestFactory.createQuery("meaningless",
                                   buildAudioXMLString(storeAudio)));
        assertNotNull(r3);
        assertEquals(0, r3.length);
        
        //test where nothing matches, should get no results
        Response[] r4 = fman.query(queryRequestFactory.createQuery("meaningless",
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
		assertTrue(result2.toString(), result2.isAddStoreEvent());
			
	      //create a query with just a file name match, should get no responses
        Response[] r5 = fman.query(queryRequestFactory.createQuery("small town hero 2"));
        assertNotNull(r5);
        assertEquals(0, r5.length);
		
        // query with videoxml matching. This SHOULDNT return results. The new Meta-data parsing
        //  is fixed to disallow adding new XML docs to files
        Response[] r6 = fman.query(queryRequestFactory.createQuery("small town hero 2",
                                    buildVideoXMLString("director=\"francis loopola\" title=\"Alive\"")));
        assertNotNull(r6);
        assertEquals(0, r6.length);
        
        // query with videoxml partial matching. This in SHOULDNT return results. The new Meta-data parsing
        //  is fixed to disallow adding new XML docs to files, this in theory shouldn't be 
        //  possible
        Response[] r7 = fman.query(queryRequestFactory.createQuery("small town hero 2",
                                    buildVideoXMLString("title=\"Alive\"")));
        assertNotNull(r7);
        assertEquals(0, r7.length);
        
        // test 100% matches minus VideoXxml, should get no results
        Response[] r8 = fman.query(queryRequestFactory.createQuery("small town hero 2",
                                    buildAudioXMLString(storeAudio)));
        assertNotNull(r8);
        assertEquals(0, r8.length);
        
        fman.removeFileIfSharedOrStore(f2);
	}

    public void testMetaQRT() throws Exception {
        String dir2 = "director=\"francis loopola\"";

        File f1 = createNewNamedTestFile(10, "hello");
        QueryRouteTable qrt = fman.getQRT();
        assertFalse("should not be in QRT", qrt.contains(get_qr(f1)));
        waitForLoad();
        
        //make sure QRP contains the file f1
        qrt = fman.getQRT();
        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));

        //now test xml metadata in the QRT
        File f2 = createNewNamedTestFile(11, "metadatafile2");
        LimeXMLDocument newDoc2 = limeXMLDocumentFactory.createLimeXMLDocument(buildVideoXMLString(dir2));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(newDoc2);
        
	    FileManagerEvent result = addIfShared(f2, l2);
	    assertTrue(result.toString(), result.isAddEvent());
	    assertEquals(newDoc2, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        qrt = fman.getQRT();
        
        assertTrue("expected in QRT", qrt.contains (get_qr(buildVideoXMLString(dir2))));
        assertFalse("should not be in QRT", qrt.contains(get_qr(buildVideoXMLString("director=\"sasami juzo\""))));
        
        //now remove the file and make sure the xml gets deleted.
        fman.removeFileIfSharedOrStore(f2);
        qrt = fman.getQRT();
       
        assertFalse("should not be in QRT", qrt.contains(get_qr(buildVideoXMLString(dir2))));
    }
    
    public void testMetaQRTStoreFiles() throws Exception {
        
        String storeAudio = "title=\"Alive\" artist=\"small town hero\" album=\"some album name\" genre=\"Rock\" licensetype=\"LIMEWIRE_STORE_PURCHASE\" year=\"2007\"";
        
        // share a file
        File f1 = createNewNamedTestFile(10, "hello");
        QueryRouteTable qrt = fman.getQRT();
        assertFalse("should not be in QRT", qrt.contains(get_qr(f1)));
        waitForLoad();
        
        //make sure QRP contains the file f1
        qrt = fman.getQRT();
        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));
        
        // create a store audio file with xml preventing sharing
        File f2 = createNewNamedTestFile(12, "small town hero");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(buildAudioXMLString(storeAudio));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        
        FileManagerEvent result = addIfShared(f2, l1);
        assertTrue(result.toString(), result.isAddStoreEvent());
        assertEquals(d1, result.getFileDescs()[0].getLimeXMLDocuments().get(0));
        qrt = fman.getQRT();
        
        assertFalse("should not be in QRT", qrt.contains (get_qr(buildAudioXMLString(storeAudio))));
   
        waitForLoad();
   
        //store file should not be in QRT table
        qrt = fman.getQRT();
        assertFalse("should not be in QRT", qrt.contains (get_qr(buildAudioXMLString(storeAudio))));
        assertTrue("expected in QRT", qrt.contains(get_qr(f1)));
    }

    public void testMetaQueries() throws Exception {
        waitForLoad();
        String dir1 = "director=\"loopola\"";

        //make sure there's nothing with this xml query
        Response[] res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        
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
        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be one match", 1, res.length);

        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);
        
        //remove a file
        fman.removeFileIfSharedOrStore(f1);

        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir1)));
        assertEquals("there should be no matches", 0, res.length);
        
        //make sure the two other files are there
        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir2)));
        assertEquals("there should be two matches", 2, res.length);

        //remove another and check we still have on left
        fman.removeFileIfSharedOrStore(f2);
        res = fman.query(queryRequestFactory.createQuery("",buildVideoXMLString(dir3)));
        assertEquals("there should be one match", 1, res.length);

        //remove the last file and make sure we get no replies
        fman.removeFileIfSharedOrStore(f3);
        res = fman.query(queryRequestFactory.createQuery("", buildVideoXMLString(dir3)));
        assertEquals("there should be no matches", 0, res.length);
    }

    private QueryRequest get_qr(File f) {
        return queryRequestFactory.createQuery(I18NConvert.instance().getNorm(f.getName()));
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
    
    protected FileManagerEvent addIfShared(File f, List<LimeXMLDocument> l) throws Exception {
        Listener fel = new Listener();
        synchronized(fel) {
            fman.addFileIfShared(f, l, fel);
            fel.wait(5000);
        }
        return fel.evt;
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


