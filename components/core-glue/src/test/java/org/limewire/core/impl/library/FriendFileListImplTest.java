package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.SharedFileCollection;

public class FriendFileListImplTest extends BaseTestCase {
    
    private Mockery context = null;

    private CoreLocalFileItemFactory coreLocalFileItemFactory = null;

    private FileCollectionManager fileCollectionManager = null;
    private FileViewManager fileViewManager = null;

    private CombinedShareList combinedShareList = null;

    private FriendFileListImpl friendFileListImpl = null;

    private SharedFileCollection testFileCollection = null;
    private FileView testFileView = null;

    private EventList<LocalFileItem> subList = null;

    private AtomicReference<EventListener<FileViewChangeEvent>> fileListChangeListener = null;

    private final String name = "name";
    
    public FriendFileListImplTest(String name) {
        super(name);
        
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        coreLocalFileItemFactory = context.mock(CoreLocalFileItemFactory.class);
        fileCollectionManager = context.mock(FileCollectionManager.class);
        fileViewManager = context.mock(FileViewManager.class);
        combinedShareList = context.mock(CombinedShareList.class);

        subList = new BasicEventList<LocalFileItem>();
        context.checking(new Expectations() {
            {
                one(combinedShareList).createMemberList();
                will(returnValue(subList));
                one(combinedShareList).addMemberList(subList);
            }
        });
        friendFileListImpl = new FriendFileListImpl(coreLocalFileItemFactory, fileCollectionManager, fileViewManager, name,
                combinedShareList);

        testFileCollection = context.mock(SharedFileCollection.class);
        testFileView = context.mock(FileView.class);

        fileListChangeListener = new AtomicReference<EventListener<FileViewChangeEvent>>();

        context.checking(new Expectations() {
            {
                one(fileViewManager).getFileViewForId(name);
                will(returnValue(testFileView));
                one(testFileView).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<FileViewChangeEvent>>(
                        fileListChangeListener, 0));
                allowing(testFileView).getReadLock();
                will(returnValue(new ReentrantLock()));
                one(testFileView).iterator();
                will(returnValue(Collections.EMPTY_LIST.iterator()));
            }
        });

        friendFileListImpl.commit();

    }

    public void testDispose() {
        context.checking(new Expectations() {
            {
                one(combinedShareList).removeMemberList(subList);
                one(testFileView).removeListener(fileListChangeListener.get());
            }
        });
        friendFileListImpl.dispose();
        context.assertIsSatisfied();
    }

    public void testGetCoreFileList() {
        context.checking(new Expectations() {
            {
                one(fileCollectionManager).getOrCreateCollectionByName(name);
                will(returnValue(testFileCollection));
                one(testFileCollection).addFriend(name);
            }
        });
        assertEquals(testFileCollection, friendFileListImpl.getMutableCollection());
        context.assertIsSatisfied();
    }

    
    public void testAddFile() {
        final File file1 = new File("file1");
        context.checking(new Expectations() {
            {
                one(fileCollectionManager).getOrCreateCollectionByName(name);
                will(returnValue(testFileCollection));
                one(testFileCollection).addFriend(name);
                one(testFileCollection).add(file1);
            }
        });
        friendFileListImpl.addFile(file1);
        context.assertIsSatisfied();
    }

    public void testRemoveFile() {
        final File file1 = new File("file1");
        context.checking(new Expectations() {
            {
                one(testFileView).contains(file1);
                will(returnValue(false));
            }
        });
        friendFileListImpl.removeFile(file1);
        
        context.checking(new Expectations() {
            {   
                one(testFileView).contains(file1);
                will(returnValue(true));
                one(fileCollectionManager).getOrCreateCollectionByName(name);
                will(returnValue(testFileCollection));
                one(testFileCollection).addFriend(name);
                one(testFileCollection).remove(file1);
            }
        });
        friendFileListImpl.removeFile(file1);
        context.assertIsSatisfied();
    }

    public void testAddFolder() {
        final File folder1 = new File("folder1");
        context.checking(new Expectations() {
            {
                one(fileCollectionManager).getOrCreateCollectionByName(name);
                will(returnValue(testFileCollection));
                one(testFileCollection).addFriend(name);
                one(testFileCollection).addFolder(folder1);
            }
        });
        friendFileListImpl.addFolder(folder1);
        context.assertIsSatisfied();
    }

    public void testContainsFile() {
        final File file1 = new File("file1");
        context.checking(new Expectations() {
            {
                one(testFileView).contains(file1);
                will(returnValue(true));
            }
        });
        assertTrue(friendFileListImpl.contains(file1));

        final File file2 = new File("file2");
        context.checking(new Expectations() {
            {
                one(testFileView).contains(file2);
                will(returnValue(false));
            }
        });
        assertFalse(friendFileListImpl.contains(file2));
        context.assertIsSatisfied();
    }
}
