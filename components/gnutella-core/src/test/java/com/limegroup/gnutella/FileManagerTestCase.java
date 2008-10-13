package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.util.FileManagerTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * Tests for FileManager and handling of queries.
 */
public class FileManagerTestCase extends LimeTestCase {

    public static final String SHARE_EXTENSION = "XYZ";
    public static final String EXTENSION = SHARE_EXTENSION + ";mp3";

    protected File f1 = null;
    protected File f2 = null;
    protected File f3 = null;
    protected File f4 = null;
    protected File f5 = null;
    protected File f6 = null;

    protected File store1 = null;
    protected File store2 = null;
    protected File store3 = null;

    // protected so that subclasses can
    // use these variables as well.
    protected volatile FileManagerImpl fman = null;
    protected QRPUpdater qrpUpdater = null;
    protected Object loaded = new Object();
    protected Response[] responses;
    protected List<FileDesc> sharedFiles;
    protected Injector injector;
    protected SharedFilesKeywordIndex keywordIndex;
    protected LimeXMLDocumentFactory limeXMLDocumentFactory;
    protected CreationTimeCache creationTimeCache;
    protected QueryRequestFactory queryRequestFactory;

    public FileManagerTestCase(String name) {
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
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        });

        fman = (FileManagerImpl)injector.getInstance(FileManager.class);
        keywordIndex = injector.getInstance(SharedFilesKeywordIndex.class);
        creationTimeCache = injector.getInstance(CreationTimeCache.class);
        qrpUpdater = injector.getInstance(QRPUpdater.class);
        SchemaReplyCollectionMapper schemaMapper = injector.getInstance(SchemaReplyCollectionMapper.class);

        fman.addFileEventListener(keywordIndex);
        fman.addFileEventListener(qrpUpdater);
        fman.addFileEventListener(schemaMapper);
        fman.addFileEventListener(creationTimeCache);

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


    ////////////////////////////////////////////////////////////////////////////////////
    // helper methods for file creation
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name, in the default shared directory.
     */
    protected File createNewNamedTestFile(int size, String name) throws Exception {
        return createNewNamedTestFile(size, name, _sharedDir);
    }

    protected File createNewNamedTestFile(int size, String name, File directory) throws Exception {
        return FileManagerTestUtils.createNewNamedTestFile(size, name, SHARE_EXTENSION, directory);
    }

    protected File createNewTestFile(int size) throws Exception {
        return createNewNamedTestFile(size, "FileManager_unit_test", _sharedDir);
    }

    protected QueryRequest get_qr(String xml) {
        return queryRequestFactory.createQuery("", xml);
    }

    protected File createNewTestStoreFile() throws Exception{
        return FileManagerTestUtils.createNewNameStoreTestFile("FileManager_unit_store_test", _storeDir);
    }

    protected File createNewTestStoreFile2() throws Exception {
        return FileManagerTestUtils.createNewNameStoreTestFile2("FileManager_unit_store_test", _storeDir);
    }

    protected URN getURN(File f) throws Exception {
        return URN.createSHA1Urn(f);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // additional helper methods
    ////////////////////////////////////////////////////////////////////////////////////

    //helper function to create queryrequest with I18N
    protected QueryRequest get_qr(File f) {
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
            testFiles.length, fman.getGnutellaSharedFileList().size() );
    }


    protected void waitForLoad() throws Exception {
        FileManagerTestUtils.waitForLoad(fman, 10000);
    }

    protected boolean responsesContain(LimeXMLDocument... doc) {
        for (LimeXMLDocument myDoc : doc) {
            boolean cuurentDocExistsInResponses = false;
            for (Response response : responses) {
                LimeXMLDocument respDoc = response.getDocument();
                if ((respDoc != null) &&
                    (myDoc.getXMLString().equals(respDoc.getXMLString()))) {
                    cuurentDocExistsInResponses = true;
                }
            }
            if (!cuurentDocExistsInResponses) {
                return false;
            }
        }
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // classes and methods related to adding files to file manager
    ////////////////////////////////////////////////////////////////////////////////////

    public static class Listener implements EventListener<FileManagerEvent> {
        public FileManagerEvent evt;
        public synchronized void handleEvent(FileManagerEvent fme) {
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

    protected FileManagerEvent addIfShared(File f) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addSharedFile(f, LimeXMLDocument.EMPTY_LIST);
            fel.wait(5000);
        }
        return fel.evt;
    }

    protected FileManagerEvent addIfShared(File f, List<LimeXMLDocument> l) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addSharedFile(f, l);
            fel.wait(5000);
        }
        return fel.evt;
    }

    protected FileManagerEvent addAlways(File f) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addSharedFileAlways(f);
            fel.wait(5000);
        }
        return fel.evt;
    }

    protected FileManagerEvent renameFile(File f1, File f2) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.renameFile(f1, f2);
            fel.wait(5000);
        }
        return fel.evt;
    }

    protected FileManagerEvent addFileForSession(File f1) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized(fel) {
            fman.addSharedFileForFession(f1);
            fel.wait(5000);
        }
        return fel.evt;
    }

    protected FileManagerEvent fileChanged(File f1) throws Exception {
        Listener fel = new Listener();
        fman.addFileEventListener(fel);
        synchronized (fel) {
            fman.fileChanged(f1, LimeXMLDocument.EMPTY_LIST);
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
}
