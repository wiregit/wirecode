package org.limewire.core.impl.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.event.ListEventAssembler;
import ca.odell.glazedlists.util.concurrent.LockFactory;

import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.SharedFileCollection;

public class ShareListManagerImplTest extends BaseTestCase {

    public ShareListManagerImplTest(String name) {
        super(name);
    }

    /**
     * Tests methods for getting and creating share lists for friends.
     */
    @SuppressWarnings("unchecked")
    public void testFriendShareListMethods() {
        Mockery context = new Mockery();

        final FileCollectionManager fileCollectionManager = context.mock(FileCollectionManager.class);
        final FileViewManager fileViewManager = context.mock(FileViewManager.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final EventBroadcaster<FriendShareListEvent> friendShareListEventBroadcaster = context
                .mock(EventBroadcaster.class);
        final TestListenerSupport listenerSupport = new TestListenerSupport();
        final LibraryManager libraryManager = context.mock(LibraryManager.class);

        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "1";

        final SharedFileCollection friendFileList1 = context
                .mock(SharedFileCollection.class);
        final Lock lock1 = new ReentrantLock();
        final Iterator<FileDesc> iterator1 = new ArrayList<FileDesc>().iterator();

        context.checking(new Expectations() {
            {
                one(libraryManager).getLibraryListEventPublisher();
                will(returnValue(ListEventAssembler.createListEventPublisher()));
                one(libraryManager).getReadWriteLock();
                will(returnValue(LockFactory.DEFAULT.createReadWriteLock()));
                allowing(gnutellaFileCollection).addListener(with(any(EventListener.class)));
                
                allowing(fileViewManager).getFileViewForId(friendId1);
                will(returnValue(friendFileList1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
                one(friendFileList1).addListener(with(any(EventListener.class)));
                allowing(friendFileList1).getReadLock();
                will(returnValue(lock1));
                one(friendFileList1).iterator();
                will(returnValue(iterator1));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileCollectionManager, fileViewManager,
                coreLocalFileItemFactory, friendShareListEventBroadcaster, libraryManager, gnutellaFileCollection);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl.getFriendShareList(friend1);
        assertNull(testFriendFileList1);

        context.checking(new Expectations() {
            {
                one(friendShareListEventBroadcaster)
                        .broadcast(
                                with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                        FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, null,
                                        friend1))));
            }
        });
        testFriendFileList1 = shareListManagerImpl.getOrCreateFriendShareList(friend1);
        assertNotNull(testFriendFileList1);

        context.assertIsSatisfied();
    }

    /**
     * Tests that when a friend is removed their share files are unloaded from
     * memory. And unloaded from the file manager.
     */
    @SuppressWarnings("unchecked")
    public void testFriendRemoved() {
        Mockery context = new Mockery();

        final FileCollectionManager fileCollectionManager = context.mock(FileCollectionManager.class);
        final FileViewManager fileViewManager = context.mock(FileViewManager.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final EventBroadcaster<FriendShareListEvent> friendShareListEventBroadcaster = context
                .mock(EventBroadcaster.class);
        final TestListenerSupport listenerSupport = new TestListenerSupport();

        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "1";

        final SharedFileCollection friendFileList1 = context
                .mock(SharedFileCollection.class);
        final Lock lock1 = new ReentrantLock();
        final Iterator<FileDesc> iterator1 = new ArrayList<FileDesc>().iterator();

        final LibraryManager libraryManager = context.mock(LibraryManager.class);
        
        context.checking(new Expectations() {
            {
                one(libraryManager).getLibraryListEventPublisher();
                will(returnValue(ListEventAssembler.createListEventPublisher()));
                one(libraryManager).getReadWriteLock();
                will(returnValue(LockFactory.DEFAULT.createReadWriteLock()));
                allowing(gnutellaFileCollection).addListener(with(any(EventListener.class)));
                
                allowing(fileViewManager).getFileViewForId(friendId1);
                will(returnValue(friendFileList1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
                one(friendFileList1).addListener(with(any(EventListener.class)));
                allowing(friendFileList1).getReadLock();
                will(returnValue(lock1));
                one(friendFileList1).iterator();
                will(returnValue(iterator1));
                one(friendShareListEventBroadcaster)
                        .broadcast(
                                with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                        FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, null,
                                        friend1))));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileCollectionManager, fileViewManager,
                coreLocalFileItemFactory, friendShareListEventBroadcaster, libraryManager, gnutellaFileCollection);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl
                .getOrCreateFriendShareList(friend1);
        assertNotNull(testFriendFileList1);

        final FriendFileList friendFileListForEvent = testFriendFileList1;
        context.checking(new Expectations() {
            {
                one(fileCollectionManager).unloadCollectionByName(friendId1);
                one(friendShareListEventBroadcaster).broadcast(
                        with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED,
                                friendFileListForEvent, friend1))));
                one(friendFileList1).removeListener(with(any(EventListener.class)));
            }
        });
        listenerSupport.fireEvent(new FriendEvent(friend1, FriendEvent.Type.REMOVED));

        testFriendFileList1 = shareListManagerImpl.getFriendShareList(friend1);
        assertNull(testFriendFileList1);
        context.assertIsSatisfied();
    }

    /**
     * Tests that when a friend is removed their share files are unloaded from
     * memory. And removed from the file manager.
     */
    @SuppressWarnings("unchecked")
    public void testFriendDeleted() {
        Mockery context = new Mockery();

        final FileCollectionManager fileCollectionManager = context.mock(FileCollectionManager.class);
        final FileViewManager fileViewManager = context.mock(FileViewManager.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final EventBroadcaster<FriendShareListEvent> friendShareListEventBroadcaster = context
                .mock(EventBroadcaster.class);
        final TestListenerSupport listenerSupport = new TestListenerSupport();

        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "1";

        final SharedFileCollection friendFileList1 = context
                .mock(SharedFileCollection.class);
        final Lock lock1 = new ReentrantLock();
        final Iterator<FileDesc> iterator1 = new ArrayList<FileDesc>().iterator();

        final LibraryManager libraryManager = context.mock(LibraryManager.class);
        
        context.checking(new Expectations() {
            {
                one(libraryManager).getLibraryListEventPublisher();
                will(returnValue(ListEventAssembler.createListEventPublisher()));
                one(libraryManager).getReadWriteLock();
                will(returnValue(LockFactory.DEFAULT.createReadWriteLock()));
                allowing(gnutellaFileCollection).addListener(with(any(EventListener.class)));
                
                allowing(fileViewManager).getFileViewForId(friendId1);
                will(returnValue(friendFileList1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
                one(friendFileList1).addListener(with(any(EventListener.class)));
                allowing(friendFileList1).getReadLock();
                will(returnValue(lock1));
                one(friendFileList1).iterator();
                will(returnValue(iterator1));
                one(friendShareListEventBroadcaster)
                        .broadcast(
                                with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                        FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, null,
                                        friend1))));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileCollectionManager, fileViewManager,
                coreLocalFileItemFactory, friendShareListEventBroadcaster, libraryManager, gnutellaFileCollection);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl
                .getOrCreateFriendShareList(friend1);
        assertNotNull(testFriendFileList1);

        final FriendFileList friendFileListForEvent = testFriendFileList1;

        context.checking(new Expectations() {
            {
                one(fileCollectionManager).removeCollectionByName(friendId1);
                one(friendShareListEventBroadcaster).broadcast(
                        with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                FriendShareListEvent.Type.FRIEND_SHARE_LIST_DELETED,
                                friendFileListForEvent, friend1))));
                one(friendFileList1).removeListener(with(any(EventListener.class)));
            }
        });
        listenerSupport.fireEvent(new FriendEvent(friend1, FriendEvent.Type.DELETE));

        testFriendFileList1 = shareListManagerImpl.getFriendShareList(friend1);
        assertNull(testFriendFileList1);
        context.assertIsSatisfied();
    }

    /**
     * Tests that files added to various friend libraries are all combined into the combined share list.
     */
    @SuppressWarnings("unchecked")
    public void testCombinedShareList() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final FileCollectionManager fileCollectionManager = context.mock(FileCollectionManager.class);
        final FileViewManager fileViewManager = context.mock(FileViewManager.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final EventBroadcaster<FriendShareListEvent> friendShareListEventBroadcaster = context
                .mock(EventBroadcaster.class);
        final TestListenerSupport listenerSupport = new TestListenerSupport();

        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "1";

        final SharedFileCollection friendFileList1 = context
                .mock(SharedFileCollection.class);

        final Lock lock1 = new ReentrantLock();
        final Iterator<FileDesc> iterator1 = new ArrayList<FileDesc>().iterator();

        final LibraryManager libraryManager = context.mock(LibraryManager.class);
        
        final AtomicReference<EventListener<FileViewChangeEvent>> internalListListener = new AtomicReference<EventListener<FileViewChangeEvent>>();
        context.checking(new Expectations() {
            {
                one(libraryManager).getLibraryListEventPublisher();
                will(returnValue(ListEventAssembler.createListEventPublisher()));
                one(libraryManager).getReadWriteLock();
                will(returnValue(LockFactory.DEFAULT.createReadWriteLock()));
                allowing(gnutellaFileCollection).addListener(with(any(EventListener.class)));
                
                allowing(fileViewManager).getFileViewForId(friendId1);
                will(returnValue(friendFileList1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
                one(friendFileList1).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<FileViewChangeEvent>>(
                        internalListListener, 0));
                allowing(friendFileList1).getReadLock();
                will(returnValue(lock1));
                one(friendFileList1).iterator();
                will(returnValue(iterator1));
                one(friendShareListEventBroadcaster)
                        .broadcast(
                                with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                        FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, null,
                                        friend1))));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileCollectionManager, fileViewManager,
                coreLocalFileItemFactory, friendShareListEventBroadcaster, libraryManager, gnutellaFileCollection);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl
                .getOrCreateFriendShareList(friend1);
        assertNotNull(testFriendFileList1);

        FileList<LocalFileItem> combinedShareList = shareListManagerImpl.getCombinedShareList();
        assertNotNull(combinedShareList);
        assertEmpty(combinedShareList.getModel());

        EventListener<FileViewChangeEvent> fileListChangeEventListener = internalListListener
                .get();
        final FileDesc fileDesc1 = context.mock(FileDesc.class);
        final File file1 = new File("file1");
        final CoreLocalFileItem localFileItem1 = context.mock(CoreLocalFileItem.class);

        context.checking(new Expectations() {
            {
                allowing(fileDesc1).getFile();
                will(returnValue(file1));
                one(fileDesc1).getClientProperty(LocalFileList.FILE_ITEM_PROPERTY);
                will(returnValue(null));
                one(coreLocalFileItemFactory).createCoreLocalFileItem(fileDesc1);
                will(returnValue(localFileItem1));
                one(fileDesc1).putClientProperty(LocalFileList.FILE_ITEM_PROPERTY, localFileItem1);
            }
        });
        fileListChangeEventListener.handleEvent(new FileViewChangeEvent(friendFileList1,
                FileViewChangeEvent.Type.FILE_ADDED, fileDesc1));

        assertEquals(1, testFriendFileList1.size());
        assertContains(testFriendFileList1.getModel(), localFileItem1);

        assertEquals(1, combinedShareList.size());
        assertContains(combinedShareList.getModel(), localFileItem1);

        final Friend friend2 = context.mock(Friend.class);
        final String friendId2 = "2";

        final SharedFileCollection friendFileList2 = context
                .mock(SharedFileCollection.class);

        final Iterator<FileDesc> iterator2 = new ArrayList<FileDesc>().iterator();
        
        context.checking(new Expectations() {
            {
                allowing(fileViewManager).getFileViewForId(friendId2);
                will(returnValue(friendFileList2));
                allowing(friend2).getId();
                will(returnValue(friendId2));
                one(friendFileList2).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<FileViewChangeEvent>>(
                        internalListListener, 0));
                allowing(friendFileList2).getReadLock();
                will(returnValue(lock1));
                one(friendFileList2).iterator();
                will(returnValue(iterator2));
                one(friendShareListEventBroadcaster)
                        .broadcast(
                                with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                        FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, null,
                                        friend2))));
            }
        });

        FriendFileList testFriendFileList2 = shareListManagerImpl
                .getOrCreateFriendShareList(friend2);
        assertNotNull(testFriendFileList2);

        assertEquals(1, testFriendFileList1.size());
        assertContains(testFriendFileList1.getModel(), localFileItem1);

        assertEquals(1, combinedShareList.size());
        assertContains(combinedShareList.getModel(), localFileItem1);

        final FileDesc fileDesc2 = context.mock(FileDesc.class);
        final File file2 = new File("file2");
        final CoreLocalFileItem localFileItem2 = context.mock(CoreLocalFileItem.class);

        context.checking(new Expectations() {
            {
                allowing(fileDesc2).getFile();
                will(returnValue(file2));
                one(fileDesc2).getClientProperty(LocalFileList.FILE_ITEM_PROPERTY);
                will(returnValue(null));
                one(coreLocalFileItemFactory).createCoreLocalFileItem(fileDesc2);
                will(returnValue(localFileItem2));
                one(fileDesc2).putClientProperty(LocalFileList.FILE_ITEM_PROPERTY, localFileItem2);
                allowing(localFileItem2).getFile();
                will(returnValue(file2));
                allowing(localFileItem1).getFile();
                will(returnValue(file1));
            }
        });
        fileListChangeEventListener = internalListListener
        .get();
        fileListChangeEventListener.handleEvent(new FileViewChangeEvent(friendFileList2,
                FileViewChangeEvent.Type.FILE_ADDED, fileDesc2));

        assertEquals(1, testFriendFileList2.size());
        assertContains(testFriendFileList2.getModel(), localFileItem2);

        assertEquals(2, combinedShareList.size());
        assertContains(combinedShareList.getModel(), localFileItem1);
        assertContains(combinedShareList.getModel(), localFileItem1);

        context.assertIsSatisfied();
    }

    private final class FriendShareListEventMatcher extends BaseMatcher<FriendShareListEvent> {
        private final FriendShareListEvent friendShareListEvent;

        public FriendShareListEventMatcher(FriendShareListEvent friendShareListEvent) {
            this.friendShareListEvent = friendShareListEvent;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Makes sure the called event matches the given event.");
        }

        @Override
        public boolean matches(Object item) {

            if (!FriendShareListEvent.class.isInstance(item)) {
                return false;
            }
            FriendShareListEvent friendShareListEvent = (FriendShareListEvent) item;

            if (!(this.friendShareListEvent.getType() == friendShareListEvent.getType())) {
                return false;
            }

            if (!(this.friendShareListEvent.getFriend() == friendShareListEvent.getFriend())) {
                return false;
            }

            if (friendShareListEvent.getType() != FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
                if (!(this.friendShareListEvent.getFileList() == friendShareListEvent.getFileList())) {
                    return false;
                }
            }

            return true;
        }
    }

    private class TestListenerSupport implements ListenerSupport<FriendEvent> {
        private final CopyOnWriteArrayList<EventListener<FriendEvent>> listenerList;

        public TestListenerSupport() {
            listenerList = new CopyOnWriteArrayList<EventListener<FriendEvent>>();
        }

        @Override
        public void addListener(EventListener<FriendEvent> listener) {
            listenerList.add(listener);
        }

        @Override
        public boolean removeListener(EventListener<FriendEvent> listener) {
            return listenerList.remove(listener);
        }

        public void fireEvent(FriendEvent event) {
            for (EventListener<FriendEvent> listener : listenerList) {
                listener.handleEvent(event);
            }
        }
    }
}
