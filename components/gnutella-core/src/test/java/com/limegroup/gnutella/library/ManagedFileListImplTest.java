package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAddFails;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAdds;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAddsFolder;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertChangeExtensions;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertContainsFiles;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileChangedFails;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileChanges;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileRenameFails;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileRenames;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertLoads;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertSetManagedDirectories;
import static com.limegroup.gnutella.library.FileManagerTestUtils.change;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createFakeTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewExtensionTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewNamedTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.getUrn;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.settings.ContentSettings;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.StubContentResponseObserver;
import com.limegroup.gnutella.auth.UrnValidator;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.util.LimeTestCase;

public class ManagedFileListImplTest extends LimeTestCase {

    private ManagedFileListImpl fileList;
    private UrnValidator urnValidator;
    private Injector injector;

    private File f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15;
    private List<FileDesc> fds;

    public ManagedFileListImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ManagedFileListImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        fileList = (ManagedFileListImpl) injector.getInstance(ManagedFileList.class);
        urnValidator = injector.getInstance(UrnValidator.class);
        fileList.initialize();
    }

    public void testNoManagedFiles() {
        assertEquals(0, fileList.size());
        assertFalse(fileList.iterator().hasNext());
        assertFalse(fileList.pausableIterable().iterator().hasNext());
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

        assertAddFails("Couldn't create FD", fileList, f1);
        assertAdds(fileList, f2, f3, f4);

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
        assertAddFails("Couldn't create FD", fileList, f2);
        assertFalse("shouldn't be shared", fileList.contains(f2));
    }
    
    public void testOneManagedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1);
        assertEquals(1, fileList.size());
        assertFalse(fileList.remove(f3));
        assertEquals(f1, fileList.getFileDescForIndex(0).getFile());
        
        assertLoads(fileList);
        assertEquals(1, fileList.size());
        assertEquals(f1, fileList.getFileDescForIndex(0).getFile());       
    }

    public void testRemovingOneFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);

        // Remove file that's shared. Back to 1 file.
        assertEquals(2, fileList.size());
        assertFalse(fileList.remove(f3));
        assertTrue(fileList.remove(f2));
        assertEquals(1, fileList.size());
        
        assertLoads(fileList);
        assertEquals(1, fileList.size());
        assertTrue(fileList.contains(f1));
    }

    public void testAddAnotherFileDifferentIndex() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);

        assertAdds(fileList, f1, f2);
        assertTrue(fileList.remove(f2));
        assertAdds(fileList, f3);

        assertEquals(2, fileList.size());
        assertNotNull(fileList.getFileDescForIndex(0));
        assertNotNull(fileList.getFileDescForIndex(2));
        assertNull(fileList.getFileDescForIndex(1));
        
        fds = CollectionUtils.listOf(fileList);
        assertContainsFiles(fds, f1, f3);
        
        assertLoads(fileList);
        fds = CollectionUtils.listOf(fileList);
        assertContainsFiles(fds, f1, f3);        
    }

    public void testRenameFiles() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f3);
        assertEquals(2, fileList.size());        
        
        assertFileRenameFails("File isn't physically manageable", fileList, f1, new File(_scratchDir, "!<invalid file>"));
        assertEquals(1, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f3);
        
        assertFileRenames(fileList, f3, f2);
        assertEquals(1, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f2);

        assertFileRenameFails("Old file wasn't managed", fileList, f1, f3);
        
        assertLoads(fileList);
        assertEquals(1, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f2);        
    }

    public void testChangeFile() throws Exception {
        f1 = createNewNamedTestFile(100, "name", _scratchDir);
        f2 = createNewTestFile(10, _scratchDir);
        assertAdds(fileList, f1);
        assertEquals(1, fileList.size());
        
        FileDesc fd = fileList.getFileDesc(f1);
        URN urn = fd.getSHA1Urn();
        assertSame(fd, fileList.getFileDescsMatching(urn).get(0));
        
        change(f1);
        assertFileChanges(fileList, f1);
        assertEquals(1, fileList.size());
        assertNotEquals(urn, fileList.getFileDesc(f1).getSHA1Urn());
        assertNotSame(fd, fileList.getFileDesc(f1));
        assertNotSame(fd, fileList.getFileDescsMatching(fileList.getFileDesc(f1).getSHA1Urn()).get(0));
        
        f1.delete();
        assertFileChangedFails("File isn't physically manageable", fileList, f1);
        assertEquals(0, fileList.size());
        
        assertFileChangedFails("Old file wasn't managed", fileList, f2);
        assertEquals(0, fileList.size());
        
        assertLoads(fileList);
        assertEquals(0, fileList.size());     
    }

    public void testIgnoreHugeFiles() throws Exception {
        long maxSize = 0xFFFFFFFFFFL; // 1TB.

        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(11, _scratchDir);
        assertAdds(fileList, f1, f2);

        // Try to add a huge file. (It will be ignored.)
        f3 = createFakeTestFile(maxSize + 1l, _scratchDir);
        assertAddFails("File isn't physically manageable", fileList, f3);
        
        // Add really big files.
        f4 = createFakeTestFile(maxSize - 1, _scratchDir);
        f5 = createFakeTestFile(maxSize, _scratchDir);
        assertAdds(fileList, f4, f5);
        
        assertEquals(4, fileList.size());
        
        assertLoads(fileList);
        assertEquals(4, fileList.size());
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f2, f4, f5);
    }

    public void testPausableIterator() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);

        assertEquals(2, fileList.size());
        Iterator<FileDesc> it = fileList.pausableIterable().iterator();
        FileDesc fd = it.next();
        assertEquals(fd.getFileName(), f1.getName());
        fd = it.next();
        assertEquals(fd.getFileName(), f2.getName());
        assertFalse(it.hasNext());
        try {
            fd = it.next();
            fail("Expected NoSuchElementException, got: " + fd);
        } catch (NoSuchElementException expected) {}

        it = fileList.pausableIterable().iterator();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        it.next();
        fileList.remove(f2);
        assertFalse(it.hasNext());

        it = fileList.pausableIterable().iterator();
        fd = it.next();
        assertFalse(it.hasNext());
        
        assertAdds(fileList, f3);
        it = fileList.pausableIterable().iterator();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        assertLoads(fileList);
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("should have thrown");
        } catch(NoSuchElementException expected) {}
    }
    
    public void testAddFolder() throws Exception {
        f1 = createNewExtensionTestFile(1,  "tmp", _scratchDir);
        f2 = createNewExtensionTestFile(3,  "tmp", _scratchDir);
        f3 = createNewExtensionTestFile(11, "tmp2", _scratchDir);
        
        File dir1 = new File(_scratchDir, "sub1");        
        File dir2 = new File(_scratchDir, "sub2");
        dir1.mkdirs();
        dir2.mkdirs();
        
        f4 = createNewExtensionTestFile(15, "tmp", dir1);
        f5 = createNewExtensionTestFile(15, "tmp2", dir2);

        fileList.setManagedExtensions(Collections.singletonList("tmp"));
        
        f6 = createNewExtensionTestFile(15, "tmp", _scratchDir);
        f7 = createNewExtensionTestFile(15, "tmp2", _scratchDir);
        assertAdds(fileList, f6, f7);
        
        assertAddsFolder(fileList, _scratchDir);
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f2, f4, f6, f7);
        assertFalse(fileList.contains(f3));
        
        assertLoads(fileList);
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f2, f4, f6, f7);
    }
    
    public void testChangeExtensions() throws Exception {
        f1 = createNewExtensionTestFile(1, "tmp1", _scratchDir);
        f2 = createNewExtensionTestFile(1, "tmp1", _scratchDir);
        f3 = createNewExtensionTestFile(1, "tmp2", _scratchDir);
        
        File dir1 = new File(_scratchDir, "sub1");        
        File dir2 = new File(_scratchDir, "sub2");
        File dir3 = new File(_scratchDir, "sub3");
        File dir4 = new File(_scratchDir, "sub4");
        File dir5 = new File(_scratchDir, "sub5");
        dir1.mkdirs();
        dir2.mkdirs();
        dir3.mkdirs();
        dir4.mkdirs();
        dir5.mkdirs();
        
        f4  = createNewExtensionTestFile(1, "tmp1", dir1);
        f5  = createNewExtensionTestFile(1, "tmp2", dir2);
        
        f6  = createNewExtensionTestFile(1, "tmp3", _scratchDir);
        f7  = createNewExtensionTestFile(1, "tmp3", dir3);
        
        f8  = createNewExtensionTestFile(1, "tmp1", dir4);
        f9  = createNewExtensionTestFile(1, "tmp2", dir4);
        f10 = createNewExtensionTestFile(1, "tmp3", dir4);
        
        fileList.setManagedExtensions(Arrays.asList("tmp1", "tmp3"));
        
        f11 = createNewExtensionTestFile(1, "tmp4", _scratchDir);
        f12 = createNewExtensionTestFile(1, "tmp4", dir4);
        f13 = createNewExtensionTestFile(1, "tmp4", dir5);
        f14 = createNewExtensionTestFile(1, "tmp1", dir5);
        f15 = createNewExtensionTestFile(1, "tmp2", dir5);
        assertAdds(fileList, f11, f12, f13, f14, f15);
        
        assertAddsFolder(fileList, _scratchDir);
        assertContainsFiles(CollectionUtils.listOf(fileList), f1, f2, f4, f6, f7, f8, f10, f11, f12, f13, f14, f15);
        assertFalse(fileList.contains(f3));
        
        assertChangeExtensions(fileList, "tmp2", "tmp3");
        assertContainsFiles(CollectionUtils.listOf(fileList), f3, f5, f6, f7, f9, f10, f11, f12, f13, f15);
        
        assertLoads(fileList);
        assertContainsFiles(CollectionUtils.listOf(fileList), f3, f5, f6, f7, f9, f10, f11, f12, f13, f15);
    }
    
    public void testSetManagedDirectories() throws Exception {
        File s1   = new File(_scratchDir, "sub1");        
        File s2   = new File(_scratchDir, "sub2");
        File s1a  = new File(s1, "ssub");
        File s2a  = new File(s2, "ssub");
        File s1as = new File(s1a, "sssub");
        File s2as = new File(s2a, "sssub");

        s1.mkdirs();
        s2.mkdirs();
        s1a.mkdirs();
        s2a.mkdirs();
        s1as.mkdirs();
        s2as.mkdirs();
        
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(1, s1);
        f3 = createNewTestFile(1, s2);
        f4  = createNewTestFile(1, s1a);
        f5 = createNewTestFile(1, s2a);
        f6 = createNewTestFile(1, s1as);
        f7 = createNewTestFile(1, s2as);
        
        assertChangeExtensions(fileList, "tmp");
        assertEquals(0, fileList.size());
        
        List<File> emptyList = Collections.emptyList();        
        List<FileDesc> fdList; // Will contain the list of newly added FDs.
        
        // Add initial directory (no subdirs)
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s2as), emptyList);
        assertContainsFiles(fdList, f7); // Assert new additions.
        assertContainsFiles(fileList, f7); // Assert complete contents.
        
        // Switch to a parent dir, assert subdir stays.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s2a), emptyList);
        assertContainsFiles(fdList, f5);
        assertContainsFiles(fileList, f5, f7);
        
        // Now restrict subdir.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s2a), Arrays.asList(s2as));
        assertContainsFiles(fileList, f5);
        
        // Add even further toplevel, restrict different subdir.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s2), Arrays.asList(s2a));
        assertContainsFiles(fdList, f3);
        assertContainsFiles(fileList, f3);
        
        // No changes!
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s2), Arrays.asList(s2a));
        assertEmpty(fdList);
        assertContainsFiles(fileList, f3);
        
        // Unrestrict subdirs.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s2), emptyList);
        assertContainsFiles(fdList, f5, f7);
        assertContainsFiles(fileList, f3, f5, f7);
        
        // Add topmost dir, restrict two different levels of subdirs.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(_scratchDir), Arrays.asList(s1a, s2as));
        assertContainsFiles(fdList, f1, f2);
        assertContainsFiles(fileList, f1, f2, f3, f5);
        
        // Change restriction in to different level in one branch of subdir.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(_scratchDir), Arrays.asList(s2a));
        assertContainsFiles(fdList, f4, f6);
        assertContainsFiles(fileList, f1, f2, f3, f4, f6);
        
        // Unrestrict everything.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(_scratchDir), emptyList);
        assertContainsFiles(fdList, f5, f7);
        assertContainsFiles(fileList, f1, f2, f3, f4, f5, f6, f7);
    }
    
    public void testSetManagedDirectoriesWithExplicitlyAddedFiles() throws Exception {
        File s1   = new File(_scratchDir, "sub1");  
        s1.mkdirs();
        
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(1, s1);
        f4 = createNewExtensionTestFile(1, "tmp1", s1);
        f5 = createNewExtensionTestFile(1, "tmp", s1);
        f6 = createNewExtensionTestFile(1, "tmp", _scratchDir);
        f7 = createNewExtensionTestFile(1, "tmp1", _scratchDir);
        
        assertChangeExtensions(fileList, "tmp");
        assertEquals(0, fileList.size());
        
        assertAdds(fileList, f4);
        
        List<File> emptyList = Collections.emptyList();        
        List<FileDesc> fdList; // Will contain the list of newly added FDs.
        
        // Add directory that contains a special file.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s1), emptyList);
        assertContainsFiles(fdList, f5, f2); // Assert new additions.
        assertContainsFiles(fileList, f5, f4, f2); // Assert complete contents.
        
        // Remove directory that contains shared & added.
        fdList = assertSetManagedDirectories(fileList, emptyList, emptyList);
        assertEmpty(fdList); // Nothing added.
        assertContainsFiles(fileList, f4); // f4 remains -- added w/o managed extension.
        
        // Add a file that is a managed extension.
        assertAdds(fileList, f5);
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s1), emptyList); 
        assertContainsFiles(fdList, f2);
        assertContainsFiles(fileList, f5, f4, f2);
        
        // Now when we remove it, it is removed with that directory.
        fdList = assertSetManagedDirectories(fileList, emptyList, emptyList);
        assertEmpty(fdList); // Nothing added.
        assertContainsFiles(fileList, f4); // f5 is removed!
        
        // Add a managed & unmanaged file outside of the managed dir.
        assertAdds(fileList, f6, f7);
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(s1), emptyList); 
        assertContainsFiles(fdList, f2, f5);
        assertContainsFiles(fileList, f6, f7, f5, f4, f2);   
        
        // Remove and make sure both varieties of added files outside
        // the managed dirs are kept.
        fdList = assertSetManagedDirectories(fileList, emptyList, emptyList);
        assertEmpty(fdList); // Nothing added.
        assertContainsFiles(fileList, f6, f7, f4);        
        
        // Assert reloading gives us the right stuff.
        fdList = assertLoads(fileList);
        assertContainsFiles(fdList, f6, f7, f4);
        assertContainsFiles(fileList, f6, f7, f4);
        
        // Now manage -- things should be added.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(_scratchDir), emptyList);
        assertContainsFiles(fdList, f1, f2, f5);
        assertContainsFiles(fileList, f1, f2, f4, f5, f6, f7);
        
        // Remove toplevel -- only non-managed extensions should remain.
        fdList = assertSetManagedDirectories(fileList, emptyList, emptyList);
        assertEmpty(fdList);
        assertContainsFiles(fileList, f4, f7);
        
        // Some additional checks w/ exclusions, just to make sure they work.
        fdList = assertSetManagedDirectories(fileList, Arrays.asList(_scratchDir), Arrays.asList(s1));
        assertContainsFiles(fdList, f1, f6);
        assertContainsFiles(fileList, f1, f4, f6, f7);
        
        fdList = assertSetManagedDirectories(fileList, emptyList, emptyList);
        assertEmpty(fdList);
        assertContainsFiles(fileList, f4, f7);     
    }
    

}
