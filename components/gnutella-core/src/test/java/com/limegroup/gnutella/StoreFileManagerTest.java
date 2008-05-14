package com.limegroup.gnutella;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.util.FileUtils;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.SimpleFileManager;

public class StoreFileManagerTest extends FileManagerTest {

    protected static final String EXTENSION = SHARE_EXTENSION+";mp3";
    
    private File store1 = null;
    private File store2 = null;
    private File store3 = null;
    
    public StoreFileManagerTest(String name) {
        super(name);
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
                bind(FileManager.class).to(SimpleFileManager.class);
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        });
        fman = (FileManagerImpl)injector.getInstance(FileManager.class);
    }
    
    @Override
    protected void tearDown() {
        super.tearDown();
        
        if(store1!=null) store1.delete();
        if(store2!=null) store2.delete();
        if(store3!=null) store3.delete();
    }

    /**
     * Tests adding a single store file to the store directory which is not a shared directory.
     * Attempts various sharing of the store files
     */
    public void testAddOneStoreFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        // load the files into the manager
        waitForLoad();
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
                   fman.removeStoreFile(store2, false));

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
        sharedFiles=fman.getSharedFilesInDirectory(_storeDir);
        assertEquals("unexpected length of shared files", 0, sharedFiles.size());
        sharedFiles=fman.getSharedFilesInDirectory(_storeDir.getParentFile());
        assertEquals("file manager listed shared files in file's parent dir",
            0, sharedFiles.size());
    }
    
       
    /**
     * Creates a store folder with both store files and non store files. Since it is the selected download
     * directory of LWS and is not shared, only the LWS store songs should be displayed and none of the files
     * should be shared
     */
    public void testNonSharedStoreFolder() throws Exception { 
        assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());

        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        store2 = createNewTestStoreFile();
        // create normal files in LWS directory (these are not in a shared directory)
        f1 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
        f2 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
        // load the files into the manager
        waitForLoad();
        
        // fman should only have loaded the two store files into list
        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
        // fman should only have loaded two shared files
        assertEquals("Unexpected number of shared files",0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
    }
    
    /**
     * Tests store files that are placed in a shared directory. They should NOT be shared
     * but should rather be extracted to the specialStoreFiles list instead
     */
    public void testSharedFolderWithStoreFiles() throws Exception { 
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());
        // create a file from LWS
        store1 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
        // create normal share files
        f1 = createNewTestFile(4);
        f2 = createNewTestFile(5);
        // load the files into the manager
        waitForLoad();

        // fman should only have loaded the two store files into list
        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
        // fman should only have loaded two shared files
        assertEquals("Unexpected number of shared files",2, fman.getNumFiles());
            
        
        // it is important to check the query at all bounds,
        // including tests for case.
        // IMPORTANT: the store files should never show up in any of these
        responses=fman.query(queryRequestFactory.createQuery("unit",(byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("FileManager", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("test", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=fman.query(queryRequestFactory.createQuery("file", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);
        responses=fman.query(queryRequestFactory.createQuery(
            "FileManager UNIT tEsT", (byte)3));
        assertEquals("Unexpected number of responses", 2, responses.length);        
                
        
        // should not be able to unshared a store file thats not shared
        assertNull("Unexpected unsharing action", fman.stopSharingFile(store1));

        // try sharing the store file
        fman.addFileIfShared(store1);
        fman.addFileIfShared(store2);
        assertEquals("Unexpected number of shared files", 2, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // try forcing the sharing
        fman.addFileAlways(store1);
        fman.addFileAlways(store2);
        assertEquals("Unexpected number of shared files", 2, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // try adding sharing for temp session
        fman.addFileForSession(store1);
        fman.addFileForSession(store2);
        assertEquals("Unexpected number of shared files", 2, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());

        fman.addFileAlways(f1);
        fman.addFileAlways(f2);
        

        // no store files should be shareable in the file descriptors
        sharedFiles=fman.getSharedFilesInDirectory(_sharedDir);
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

        sharedFiles=fman.getSharedFilesInDirectory(_storeDir.getParentFile());
        assertEquals("file manager listed shared files in file's parent dir",
            0, sharedFiles.size());
    }
    
    /**
     * Creates store files in both the store folder and the shared folder, creates non store files
     * in both the store folder and the shared folder. Initially only non-LWS files in the shared folder
     * are shared and all LWS files are displayed. After sharing the store folder, all non-LWS files are
     * shared and all store files remain unshared and displayed
     */
    public void testSharedFolderAlsoStoreFolder() throws Exception {
        
        assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());
        // create files in the store folder
        store1 = createNewTestStoreFile();
        store2 = createNewTestStoreFile();
        // create normal files in LWS directory (these are not in a shared directory)
        f1 = createNewNamedTestFile(4, "FileManager_unit_test", _storeDir);
        
        // create normal share files
        f2 = createNewTestFile(4);
        f3 = createNewTestFile(5);
        // create a file from LWS in shared directory
        store3 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);

        
        // load the files into the manager
        waitForLoad();
        
        // all three store files should be displayed
        assertEquals("Unexpected number of store files", 3, fman.getNumStoreFiles());
        // fman should only have loaded two shared files
        assertEquals("Unexpected number of shared files",2, fman.getNumFiles());
        // one of the store files is in a shared directory so it is also individual store
        assertEquals("Unexpected number of individual store files", 1, fman.getIndividualStoreFiles().length);
        assertEquals("Unexpected number of inidividual share files", 0, fman.getIndividualFiles().length);
        
        // start sharing the store directory
        fman.addSharedFolder(_storeDir);
        // load the files into the manager
        waitForLoad();
        
        // all LWS files are displayed
        assertEquals("Unexpected number of store files", 3, fman.getNumStoreFiles());
        // all non LWS files are shared
        assertEquals("Unexpected number of shared files",3, fman.getNumFiles());
        assertEquals("Unexpected number of individual store files", 3, fman.getIndividualStoreFiles().length);
        assertEquals("Unexpected number of inidividual share files", 0, fman.getIndividualFiles().length);
        
        fman.removeFolderIfShared(_storeDir);
        
    }
    
    /**
     * Tests what happens when LWS songs are located in a shared directory that is not
     * the store directory. After unsharing that shared directory the store files are no
     * longer visible
     */
    public void testUnshareFolderContainingStoreFiles() throws Exception {

        // create a file from LWS in shared directory
        store1 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
        f1 = createNewTestFile(4);
        
        // create a file from the LWS in the store directory
        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _storeDir);
        // load the files into the manager
        waitForLoad();
        
        // should only be sharing one file
        assertEquals("Unexpected number of shared files", 1, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
       
        // check lws files, individual store files
        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
        assertEquals("Unexpeected number of individual store files", 1, fman.getIndividualStoreFiles().length);
        
        // unshare the shared directory
        fman.removeFolderIfShared(_sharedDir);       
        // load the files into the manager
        waitForLoad();
        
        // should not share any files
        assertEquals("Unexpected number of shared files", 0, fman.getNumFiles());
        assertEquals("Unexpected number of shared files", 0, fman.getNumForcedFiles());
        
        // check lws files, individual store files
        assertEquals("Unexpected number of store files", 1, fman.getNumStoreFiles());
        assertEquals("Unexpected number of individual store files", 0, fman.getIndividualStoreFiles().length);
        
        fman.addSharedFolder(_sharedDir);
    }
    
    /**
     * Tests what files are displayed from the LWS when you switch to a new directory to save
     * LWS downloads to. Previously displayed files in the old directory are no longer
     * displayed, just LWS files in the new directory are displayed
     */
    public void testChangeStoreFolder() throws Exception {assertEquals("Unexpected number of store files", 0, fman.getNumStoreFiles());
        
        // create alternative store directory to switch saving files to
        File newStoreFolder = new File(_baseDir, "store2");
        newStoreFolder.deleteOnExit();        
        newStoreFolder.mkdirs();
        
        // create a file from LWS
        store1 = createNewTestStoreFile();
        store2 = createNewTestStoreFile();
        
        //create a file after the load
        store3 = createNewNameStoreTestFile("FileManager_unit_store_test", newStoreFolder);
        // load the files into the manager
        waitForLoad();


        // fman should only have loaded the two store files into list
        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
        // fman should only have loaded no shared files
        assertEquals("Unexpected number of shared files",0, fman.getNumFiles());
                
        
        // change the store save directory
        SharingSettings.setSaveLWSDirectory(newStoreFolder);
        // load the files into the manager
        waitForLoad();

       // fman should only have loaded the two store files into list
        assertEquals("Unexpected number of store files", 1, fman.getNumStoreFiles());
        // fman should only have loaded two shared files
        assertEquals("Unexpected number of shared files",0, fman.getNumFiles());
 
        // check the list of individual store files (only the two store files should be displayed)
        //  any LWS files loaded into a shared directory will be returned here
        File[] storeFiles = fman.getIndividualStoreFiles();
        assertEquals("Unexpected number of store files", 0, storeFiles.length);
        
        SharingSettings.setSaveLWSDirectory(_storeDir);
        newStoreFolder.delete();
    }
    /**
     * Checks that removing a store file really removes the store file from the view
     */
    public void testRemoveOneStoreFile() throws Exception {
          
        assertEquals(0, fman.getNumStoreFiles()); 
        store1 = createNewTestStoreFile();
        store2 = createNewTestStoreFile();
        waitForLoad();
    
        //Remove file that's shared.  Back to 1 file.                   
        assertEquals(2, fman.getNumStoreFiles());     
        assertNotNull("should have been able to remove shared file", fman.removeStoreFile(store2, true));
        assertEquals("unexpected number of files", 1, fman.getNumStoreFiles());
    }

    
    /**
     * Creates store files in both a shared directory and the store directory
     * and tries to explicitly share them
     */
    public void testAddFileAlwaysStoreFile() throws Exception {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        // create a file from LWS in shared directory
        store1 = createNewNameStoreTestFile("FileManager_unit_store_test", _sharedDir);
        // create a file from the LWS in the store directory
        store2 = createNewNameStoreTestFile("FileManager_unit_store_test", _storeDir);
        // load the files into the manager
        waitForLoad();

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
        
        // add two store files
        store1 = createNewTestStoreFile();
        store2 = createNewTestStoreFile();
        waitForLoad();
        // create a third store file but it not added anywhere
        store3 = createNewTestStoreFile();
    
        // try renaming unadded file, should fail
        FileManagerEvent result = renameFile(store3, new File("c:\\asdfoih.mp3"));
        assertTrue(result.toString(), result.isFailedEvent());

        // rename a valid store file
        result = renameFile(store1, store3);
        assertTrue(result.toString(), result.isRenameEvent());
        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
        assertEquals("Unexpected file renamed", store1, result.getFileDescs()[0].getFile());
        assertEquals("Unexpected file added", store3, result.getFileDescs()[1].getFile());

        // renamed file should not be found, new name file should be found
        assertFalse(fman.isStoreFileLoaded(store1));
        assertTrue(fman.isStoreFileLoaded(store3));
        // still only two store files
        assertEquals("Unexpected number of store files", 2, fman.getNumStoreFiles());
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
