package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
                allowing(friendShareListEventListener).handleEvent(
                        with(any(FriendShareListEvent.class)));
            }
        });
        ShareListManagerImpl shareListManagerImpl = new ShareListManagerImpl(fileManager,
                coreLocalFileItemFactory, friendShareListEventListener);
        shareListManagerImpl.register(listenerSupport);

        FriendFileList testFriendFileList1 = shareListManagerImpl.getFriendShareList(friend1);
        assertNull(testFriendFileList1);

        testFriendFileList1 = shareListManagerImpl.getOrCreateFriendShareList(friend1);
        assertNotNull(testFriendFileList1);
        
        context.assertIsSatisfied();
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
