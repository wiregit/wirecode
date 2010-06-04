package org.limewire.core.impl.library;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.CollectionUtils;
import org.limewire.common.URN;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibrary;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryState;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;
import static org.limewire.core.impl.library.PresenceLibraryImplTest.RemoteLibraryEventMatcher;

public class RemoteLibraryManagerImplTest extends BaseTestCase {

    public RemoteLibraryManagerImplTest(String name) {
        super(name);
    }

    public void testPresenceLibraryMethods() {
        Mockery context = new Mockery();
        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "presence1";
        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "friend1";
        context.checking(new Expectations() {
            {
                allowing(friend1).isAnonymous();
                will(returnValue(false));
                allowing(friendPresence1).getFriend();
                will(returnValue(friend1));
                allowing(friendPresence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(friend1).getId();
                will(returnValue(friendId1));
            }
        });

        RemoteLibraryManagerImpl remoteLibraryManagerImpl = new RemoteLibraryManagerImpl();

        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        boolean added = remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(added);
        PresenceLibrary presenceLibrary1 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence1);

        assertNotNull(presenceLibrary1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        added = remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertFalse(added);

        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        remoteLibraryManagerImpl.removePresenceLibrary(friendPresence1);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        added = remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(added);
        presenceLibrary1 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence1);
        
        assertNotNull(presenceLibrary1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        context.assertIsSatisfied();
    }

    public void testFriendLibraryMethods() {
        Mockery context = new Mockery();
        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "presence1";
        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "friend1";

        final FriendPresence friendPresence2 = context.mock(FriendPresence.class);
        final String presenceId2 = "presence2";
        final Friend friend2 = context.mock(Friend.class);
        final String friendId2 = "friend2";
        context.checking(new Expectations() {
            {
                allowing(friend1).isAnonymous();
                will(returnValue(false));
                allowing(friend2).isAnonymous();
                will(returnValue(false));
                
                allowing(friendPresence1).getFriend();
                will(returnValue(friend1));
                allowing(friendPresence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(friend1).getId();
                will(returnValue(friendId1));

                allowing(friendPresence2).getFriend();
                will(returnValue(friend2));
                allowing(friendPresence2).getPresenceId();
                will(returnValue(presenceId2));
                allowing(friend2).getId();
                will(returnValue(friendId2));
            }
        });

        RemoteLibraryManagerImpl remoteLibraryManagerImpl = new RemoteLibraryManagerImpl();

        List<FriendLibrary> friendLibraries = remoteLibraryManagerImpl.getFriendLibraryList();
        assertEmpty(friendLibraries);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        remoteLibraryManagerImpl.removePresenceLibrary(friendPresence1);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEmpty(friendLibraries);

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        // test that removal of all presence libraries removes friend library
        remoteLibraryManagerImpl.removePresenceLibrary(friendPresence1);
        assertFalse(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEmpty(friendLibraries);

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(1, friendLibraries.size());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence2);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend2));
        assertEquals(2, friendLibraries.size());

        Iterator<FriendLibrary> friendLibraryIterator = friendLibraries.iterator();
        assertEquals(friend1, friendLibraryIterator.next().getFriend());
        assertEquals(friend2, friendLibraryIterator.next().getFriend());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend1));
        assertEquals(2, friendLibraries.size());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence2);
        assertTrue(remoteLibraryManagerImpl.hasFriendLibrary(friend2));
        assertEquals(2, friendLibraries.size());

        friendLibraryIterator = friendLibraries.iterator();
        assertEquals(friend1, friendLibraryIterator.next().getFriend());
        assertEquals(friend2, friendLibraryIterator.next().getFriend());

        context.assertIsSatisfied();
    }

    public void testAllFriendsFileList() {
        Mockery context = new Mockery();

        final FriendPresence friendPresence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "presence1";
        final Friend friend1 = context.mock(Friend.class);
        final String friendId1 = "friend1";

        final FriendPresence friendPresence2 = context.mock(FriendPresence.class);
        final String presenceId2 = "presence2";
        final Friend friend2 = context.mock(Friend.class);
        final String friendId2 = "friend2";

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final SearchResult remoteFileItem2 = context.mock(SearchResult.class);
        final SearchResult remoteFileItem3 = context.mock(SearchResult.class);

        final URN urn1 = new TestURN("1");
        final URN urn2 = new TestURN("2");
        final URN urn3 = new TestURN("3");

        context.checking(new Expectations() {
            {
                allowing(friend1).isAnonymous();
                will(returnValue(false));
                allowing(friend2).isAnonymous();
                will(returnValue(false));
                
                allowing(friendPresence1).getFriend();
                will(returnValue(friend1));
                allowing(friendPresence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(friend1).getId();
                will(returnValue(friendId1));

                allowing(friendPresence2).getFriend();
                will(returnValue(friend2));
                allowing(friendPresence2).getPresenceId();
                will(returnValue(presenceId2));
                allowing(friend2).getId();
                will(returnValue(friendId2));

                allowing(remoteFileItem1).getUrn();
                will(returnValue(urn1));
                allowing(remoteFileItem2).getUrn();
                will(returnValue(urn2));
                allowing(remoteFileItem3).getUrn();
                will(returnValue(urn3));
            }
        });

        RemoteLibraryManagerImpl remoteLibraryManagerImpl = new RemoteLibraryManagerImpl();

        RemoteLibrary allFriendFiles = remoteLibraryManagerImpl.getAllFriendsLibrary();
        assertEquals(0, allFriendFiles.size());

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence1);
        PresenceLibrary presenceLibrary1 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence1);

        remoteLibraryManagerImpl.addPresenceLibrary(friendPresence2);
        PresenceLibrary presenceLibrary2 = remoteLibraryManagerImpl.getPresenceLibrary(friendPresence2);

        assertEquals(0, allFriendFiles.size());

        presenceLibrary1.addNewResult(remoteFileItem1);

        assertEquals(1, allFriendFiles.size());
        
        assertContains(CollectionUtils.listOf(allFriendFiles), remoteFileItem1);

        presenceLibrary2.addNewResult(remoteFileItem2);

        assertEquals(2, allFriendFiles.size());
        assertContains(CollectionUtils.listOf(allFriendFiles), remoteFileItem1);
        assertContains(CollectionUtils.listOf(allFriendFiles), remoteFileItem2);

        presenceLibrary2.addNewResult(remoteFileItem3);
        assertEquals(3, allFriendFiles.size());
        assertContains(CollectionUtils.listOf(allFriendFiles), remoteFileItem1);
        assertContains(CollectionUtils.listOf(allFriendFiles), remoteFileItem2);
        assertContains(CollectionUtils.listOf(allFriendFiles), remoteFileItem3);
                
        context.assertIsSatisfied();
    }
    
    /**
     * Ensures that events from a presence library
     * are propagated to the all friends library.
     */
    @SuppressWarnings("unchecked")
    public void testAddEventsArePropagated() {
        
        final RemoteLibraryManagerImpl remoteLibraryManager = new RemoteLibraryManagerImpl();
        
        Mockery context = new Mockery();
        final EventListener<RemoteLibraryEvent> listener = context.mock(EventListener.class);
        final SearchResult searchResult = context.mock(SearchResult.class);
        final FriendPresence friendPresence = context.mock(FriendPresence.class);
        final Friend friend = context.mock(Friend.class);
    
        context.checking(new Expectations() {{
            // actual check
            one(listener).handleEvent(with(new RemoteLibraryEventMatcher(RemoteLibraryEvent.createResultsAddedEvent(remoteLibraryManager.getAllFriendsLibrary(), Collections.singleton(searchResult), -1))));
            one(listener).handleEvent(with(new RemoteLibraryEventMatcher(RemoteLibraryEvent.createResultsClearedEvent(remoteLibraryManager.getAllFriendsLibrary()))));
            ignoring(listener);
            // stubs
            allowing(friendPresence).getPresenceId();
            will(returnValue("friend-presence-id"));
            allowing(friendPresence).getFriend();
            will(returnValue(friend));
            ignoring(friend);
        }});
        
        remoteLibraryManager.getAllFriendsLibrary().addListener(listener);
        boolean added = remoteLibraryManager.addPresenceLibrary(friendPresence);
        assertTrue(added);
        PresenceLibrary presenceLibrary = remoteLibraryManager.getPresenceLibrary(friendPresence);
        assertNotNull(presenceLibrary);
        presenceLibrary.addNewResult(searchResult);
        
        presenceLibrary.clear();
        
        presenceLibrary.setState(RemoteLibraryState.LOADED);
        assertEquals(RemoteLibraryState.LOADED, remoteLibraryManager.getAllFriendsLibrary().getState());
        
        presenceLibrary.setState(RemoteLibraryState.LOADING);
        assertEquals(RemoteLibraryState.LOADING, remoteLibraryManager.getAllFriendsLibrary().getState());
        
        context.assertIsSatisfied();
    }

    private class TestURN implements URN {
        private String urn;

        public TestURN(String urn) {
            this.urn = urn;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestURN) {
                TestURN oUrn = (TestURN) obj;
                return urn.equals(oUrn);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return urn.hashCode();
        }

        @Override
        public int compareTo(URN o) {
            if (o instanceof TestURN) {
                TestURN testURN = (TestURN) o;
                return urn.compareTo(testURN.urn);
            }
            return -1;
        }

        @Override
        public String toString() {
            return urn;
        }
    }
}
