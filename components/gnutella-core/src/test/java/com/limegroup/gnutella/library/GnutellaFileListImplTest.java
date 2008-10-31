package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.library.FileManagerTestUtils.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.settings.ContentSettings;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.StubContentResponseObserver;
import com.limegroup.gnutella.auth.UrnValidator;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.util.LimeTestCase;

public class GnutellaFileListImplTest extends LimeTestCase {

    private ManagedFileList managedList;
    private GnutellaFileList fileList;
    private UrnValidator urnValidator;
    private Injector injector;

    private File f1, f2, f3, f4;
    private List<FileDesc> sharedFiles;

    public GnutellaFileListImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GnutellaFileListImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        managedList = injector.getInstance(FileManager.class).getManagedFileList();
        fileList = injector.getInstance(FileManager.class).getGnutellaSharedFileList();
        urnValidator = injector.getInstance(UrnValidator.class);
        injector.getInstance(ServiceRegistry.class).initialize();
    }

    public void testNoSharedFiles() {
        assertEquals(0, fileList.size());
        assertFalse(fileList.iterator().hasNext());
        assertFalse(fileList.pausableIterable().iterator().hasNext());
        assertEquals(0, fileList.getFilesInDirectory(_scratchDir).size());
        assertEquals(0, fileList.getNumBytes());
        assertFalse(fileList.hasApplicationSharedFiles());
    }

    public void testContentManagerActive() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        f4 = createNewTestFile(23, _scratchDir);

        URN u1 = getUrn(f1);
        URN u2 = getUrn(f2);
        URN u3 = getUrn(f3);
        URN u4 = getUrn(f4);

        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);
        ContentManager cm = injector.getInstance(ContentManager.class);
        cm.start();

        // request the urn so we can use the response.
        cm.request(u1, new StubContentResponseObserver(), 1000);
        cm.handleContentResponse(new ContentResponse(u1, false));

        addFail("Couldn't create FD", fileList, f1);
        add(fileList, f2, f3, f4);

        assertEquals("unexpected # of files", 3, fileList.size());
        assertFalse(fileList.contains(f1));
        assertTrue(fileList.contains(f2));
        assertTrue(fileList.contains(f3));
        assertTrue(fileList.contains(f4));

        // test invalid content response.
        urnValidator.validate(u2);
        cm.handleContentResponse(new ContentResponse(u2, false));
        assertFalse(fileList.contains(f2));
        assertEquals(2, fileList.size());

        // test valid content response.
        urnValidator.validate(u3);
        cm.handleContentResponse(new ContentResponse(u3, true));
        assertTrue(fileList.contains(f3));
        assertEquals(2, fileList.size());

        // test valid content response.
        urnValidator.validate(u4);
        Thread.sleep(10000);
        assertTrue(fileList.contains(f4));
        assertEquals(2, fileList.size());

        // Make sure adding a new file to be shared doesn't work if it
        // returned bad before.
        addFail("Couldn't create FD", fileList, f2);
        assertFalse("shouldn't be shared", fileList.contains(f2));
    }
    
    public void testOneSharedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);

        add(fileList, f1);
        assertEquals(1, managedList.size());
        assertEquals(1, fileList.size());
        assertFalse(fileList.remove(f3));
        assertEquals(f1, fileList.getFileDescForIndex(0).getFile());
        assertEquals(f1, managedList.getFileDescForIndex(0).getFile());

        sharedFiles = fileList.getFilesInDirectory(_scratchDir);
        assertEquals(1, sharedFiles.size());
        assertEquals(sharedFiles.get(0).getFile(), f1);
        sharedFiles = fileList.getFilesInDirectory(_scratchDir.getParentFile());
        assertEquals(0, sharedFiles.size());
    }
    
    public void testRemovingOneFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        add(fileList, f1, f2);

        // Remove file that's shared. Back to 1 file.
        assertEquals(2, fileList.size());
        assertEquals(2, managedList.size());
        assertFalse(fileList.remove(f3));
        assertTrue(fileList.remove(f2));
        assertEquals(1, fileList.size());
        assertEquals(2, managedList.size());
    }

    public void testAddAnotherSharedFileDifferentIndex() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);

        add(fileList, f1, f2);
        assertTrue(fileList.remove(f2));
        add(fileList, f3);
        
        assertEquals(12, fileList.getNumBytes());
        assertEquals(2, fileList.size());
        assertEquals(3, managedList.size());
        
        assertNotNull(fileList.getFileDescForIndex(0));
        assertNotNull(fileList.getFileDescForIndex(2));
        assertNull(fileList.getFileDescForIndex(1));
        assertNotNull(managedList.getFileDescForIndex(1));

        sharedFiles = fileList.getFilesInDirectory(_scratchDir);
        assertEquals("unexpected files length", 2, sharedFiles.size());
        assertContainsFiles(sharedFiles, f1, f3);
        
        sharedFiles = CollectionUtils.listOf(fileList);
        assertContainsFiles(sharedFiles, f1, f3);
        
        sharedFiles = CollectionUtils.listOf(managedList);
        assertContainsFiles(sharedFiles, f1, f2, f3);
    }
    
    public void testRenameFiles() throws Exception {
        f1 = createNewNamedTestFile(1, "1111", _scratchDir);
        f2 = createNewNamedTestFile(3, "2222", _scratchDir);
        f3 = createNewNamedTestFile(11, "3333", _scratchDir);
        
        add(fileList, f1, f3);
        assertEquals(2, fileList.size());        
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f3);
        
        try {
            managedList.fileRenamed(f1, new File("c:\\asdfoih")).get(1, TimeUnit.SECONDS);
            fail("should have failed");
        } catch(ExecutionException expected) {}
        assertContainsFiles(CollectionUtils.listOf(fileList), f3);
        
        fileRenamed(managedList, f3, f2);        
        assertContainsFiles(CollectionUtils.listOf(fileList), f2);
        
        assertNull(managedList.fileRenamed(f1, f3).get(1, TimeUnit.SECONDS));        
        assertContainsFiles(CollectionUtils.listOf(fileList), f2);
    }
    
    public void testChangeSharedFile() throws Exception {
        f1 = createNewNamedTestFile(100, "name", _scratchDir);
        f2 = createNewTestFile(10, _scratchDir);
        add(fileList, f1);
        assertEquals(1, fileList.size());
        
        FileDesc fd = fileList.getFileDesc(f1);
        URN urn = fd.getSHA1Urn();
        assertSame(fd, fileList.getFileDesc(urn));
        
        change(f1);
        fileChanged(managedList, f1);
        assertEquals(1, fileList.size());
        assertNotEquals(urn, getUrn(f1));
        assertEquals(getUrn(f1), fileList.getFileDesc(f1).getSHA1Urn());
        assertEquals(urn, fileList.getFileDesc(f1).getSHA1Urn());
        assertNotSame(fd, fileList.getFileDesc(f1));
        assertNotSame(fd, fileList.getFileDesc(fileList.getFileDesc(f1).getSHA1Urn()));
        
        f1.delete();
        fileChangedFailed("File isn't physically manageable", managedList, f1);
        assertEquals(0, fileList.size());
        
        fileChangedFailed(null, managedList, f2);
        assertEquals(0, fileList.size());
    }
}
