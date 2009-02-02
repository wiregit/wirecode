package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAdds;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAddsFolder;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewTestFile;

import java.io.File;

import junit.framework.Test;

import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * FriendFileList tests
 */
public class FriendFileListTest extends LimeTestCase {

    private ManagedFileListImpl managedList;
    private FriendFileListImpl friendList;
    private Injector injector;

    private File f1;
    
    public FriendFileListTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FriendFileListTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        friendList = (FriendFileListImpl)injector.getInstance(FileManager.class).getOrCreateFriendFileList("test");
        managedList = (ManagedFileListImpl)injector.getInstance(FileManager.class).getManagedFileList();
        injector.getInstance(ServiceRegistry.class).initialize();
    }

    public void testNoFiles() {
        assertEquals(0, friendList.size());
        assertFalse(friendList.iterator().hasNext());
        assertFalse(friendList.pausableIterable().iterator().hasNext());
    }
    
    /**
     * Tests adding a single file to a friendList
     */
    public void testAddFile() throws Exception {
        assertEquals(friendList.size(), 0);
        
        f1 = createNewTestFile(1, _scratchDir);
        
        assertAdds(friendList, f1);
        assertTrue(friendList.contains(f1));
        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
        assertEquals(friendList.size(), 1);
        assertTrue(friendList.iterator().hasNext());
        assertTrue(friendList.pausableIterable().iterator().hasNext());
    }
    
    /**
     * Tests adding a FileDesc to friend list that is already managed
     */
    public void testAddFileDesc() throws Exception {
        assertEquals(friendList.size(), 0);
        
        f1 = createNewTestFile(1, _scratchDir);
        
        assertAdds(managedList, f1);
        
        FileDesc fd = managedList.getFileDesc(f1);
        assertTrue(friendList.add(fd));
        
        assertTrue(friendList.contains(fd));
        assertEquals(fd, friendList.getFileDesc(f1));
        assertEquals(friendList.size(), 1);
        assertTrue(friendList.iterator().hasNext());
        assertTrue(friendList.pausableIterable().iterator().hasNext());
    }
    
    /**
     * Test removing a single file from friendList.
     */
    public void testRemoveFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        
        assertFalse(friendList.remove(f1));
        
        assertAdds(friendList, f1);
        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
        assertEquals(1, friendList.size());
        
        assertTrue(friendList.contains(f1));
        assertTrue(friendList.remove(f1));
        assertNull(friendList.getFileDescForIndex(0));
        assertEquals(0, friendList.size());
        assertFalse(friendList.contains(f1));
        assertFalse(friendList.iterator().hasNext());
        assertFalse(friendList.pausableIterable().iterator().hasNext());        
    }
    
    /**
     * Test removing a FileDesc from a friendList.
     */
    public void testRemoveFileDesc() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        
        assertFalse(friendList.remove(f1));
        
        assertAdds(friendList, f1);
        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
        assertEquals(1, friendList.size());
        
        FileDesc fd = friendList.getFileDesc(f1);
        
        assertTrue(friendList.contains(fd));
        
        assertTrue(friendList.remove(fd));
        assertNull(friendList.getFileDescForIndex(0));
        assertEquals(0, friendList.size());        
        assertFalse(friendList.contains(fd));
    }
    
    public void testAddFolder() throws Exception {
        assertEquals(friendList.size(), 0);
        
        f1 = createNewTestFile(1, _scratchDir);
//        f2 = createNewTestFile(3, _scratchDir);
        
        assertAddsFolder(friendList, _scratchDir);

//        assertEquals(2, friendList.size());
//        assertEquals(f1, friendList.getFileDescForIndex(0).getFile());
//        assertEquals(f2, friendList.getFileDescForIndex(1).getFile());
//        assertTrue(friendList.iterator().hasNext());
//        assertTrue(friendList.pausableIterable().iterator().hasNext());
    }
    
}
