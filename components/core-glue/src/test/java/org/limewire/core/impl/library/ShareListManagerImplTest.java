package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;

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

        final FileManager fileManager = context.mock(FileManager.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final EventListener<FriendShareListEvent> friendShareListEventListener = context
                .mock(EventListener.class);
        final TestListenerSupport listenerSupport = new TestListenerSupport();

        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "1";

        final com.limegroup.gnutella.library.FriendFileList friendFileList1 = context
                .mock(com.limegroup.gnutella.library.FriendFileList.class);
        final Lock lock1 = new ReentrantLock();
        final Iterator<FileDesc> iterator1 = new ArrayList<FileDesc>().iterator();

        context.checking(new Expectations() {
            {
                one(fileManager).getGnutellaFileList();
                allowing(fileManager).getOrCreateFriendFileList(friendId1);
                will(returnValue(friendFileList1));
                allowing(fileManager).getFriendFileList(friendId1);
                will(returnValue(friendFileList1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
                one(friendFileList1).addFileListListener(with(any(EventListener.class)));
                allowing(friendFileList1).getReadLock();
                will(returnValue(lock1));
                one(friendFileList1).iterator();
                will(returnValue(iterator1));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileManager,
                coreLocalFileItemFactory, friendShareListEventListener);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl.getFriendShareList(friend1);
        assertNull(testFriendFileList1);

        context.checking(new Expectations() {
            {
                one(friendShareListEventListener)
                        .handleEvent(
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

        final FileManager fileManager = context.mock(FileManager.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final EventListener<FriendShareListEvent> friendShareListEventListener = context
                .mock(EventListener.class);
        final TestListenerSupport listenerSupport = new TestListenerSupport();

        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "1";

        final com.limegroup.gnutella.library.FriendFileList friendFileList1 = context
                .mock(com.limegroup.gnutella.library.FriendFileList.class);
        final Lock lock1 = new ReentrantLock();
        final Iterator<FileDesc> iterator1 = new ArrayList<FileDesc>().iterator();

        context.checking(new Expectations() {
            {
                one(fileManager).getGnutellaFileList();
                allowing(fileManager).getOrCreateFriendFileList(friendId1);
                will(returnValue(friendFileList1));
                allowing(fileManager).getFriendFileList(friendId1);
                will(returnValue(friendFileList1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
                one(friendFileList1).addFileListListener(with(any(EventListener.class)));
                allowing(friendFileList1).getReadLock();
                will(returnValue(lock1));
                one(friendFileList1).iterator();
                will(returnValue(iterator1));
                one(friendShareListEventListener)
                        .handleEvent(
                                with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                        FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, null,
                                        friend1))));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileManager,
                coreLocalFileItemFactory, friendShareListEventListener);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl
                .getOrCreateFriendShareList(friend1);
        assertNotNull(testFriendFileList1);

        final FriendFileList friendFileListForEvent = testFriendFileList1;
        context.checking(new Expectations() {
            {
                one(fileManager).unloadFilesForFriend(friendId1);
                one(friendShareListEventListener).handleEvent(
                        with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED,
                                friendFileListForEvent, friend1))));
                one(friendFileList1).removeFileListListener(with(any(EventListener.class)));
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

        final FileManager fileManager = context.mock(FileManager.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final EventListener<FriendShareListEvent> friendShareListEventListener = context
                .mock(EventListener.class);
        final TestListenerSupport listenerSupport = new TestListenerSupport();

        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "1";

        final com.limegroup.gnutella.library.FriendFileList friendFileList1 = context
                .mock(com.limegroup.gnutella.library.FriendFileList.class);
        final Lock lock1 = new ReentrantLock();
        final Iterator<FileDesc> iterator1 = new ArrayList<FileDesc>().iterator();

        context.checking(new Expectations() {
            {
                one(fileManager).getGnutellaFileList();
                allowing(fileManager).getOrCreateFriendFileList(friendId1);
                will(returnValue(friendFileList1));
                allowing(fileManager).getFriendFileList(friendId1);
                will(returnValue(friendFileList1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
                one(friendFileList1).addFileListListener(with(any(EventListener.class)));
                allowing(friendFileList1).getReadLock();
                will(returnValue(lock1));
                one(friendFileList1).iterator();
                will(returnValue(iterator1));
                one(friendShareListEventListener)
                        .handleEvent(
                                with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                        FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, null,
                                        friend1))));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileManager,
                coreLocalFileItemFactory, friendShareListEventListener);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl
                .getOrCreateFriendShareList(friend1);
        assertNotNull(testFriendFileList1);

        final FriendFileList friendFileListForEvent = testFriendFileList1;

        context.checking(new Expectations() {
            {
                one(fileManager).removeFriendFileList(friendId1);
                one(friendShareListEventListener).handleEvent(
                        with(new FriendShareListEventMatcher(new FriendShareListEvent(
                                FriendShareListEvent.Type.FRIEND_SHARE_LIST_DELETED,
                                friendFileListForEvent, friend1))));
                one(friendFileList1).removeFileListListener(with(any(EventListener.class)));
            }
        });
        listenerSupport.fireEvent(new FriendEvent(friend1, FriendEvent.Type.DELETE));

        testFriendFileList1 = shareListManagerImpl.getFriendShareList(friend1);
        assertNull(testFriendFileList1);
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
