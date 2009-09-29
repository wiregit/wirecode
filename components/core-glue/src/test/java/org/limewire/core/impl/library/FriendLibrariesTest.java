package org.limewire.core.impl.library;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.RemoteLibraryState;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.FriendPresence;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class FriendLibrariesTest extends LimeTestCase {

    public FriendLibrariesTest(String name) {
        super(name);
    }

    /**
     * Tests search for remote file items in the friends library by name. Using
     * a single friend and presence in this instance.
     */
    @SuppressWarnings("unchecked")
    public void testIndexing1FriendLibraryAndFileByFileNameOnly() throws Throwable {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;

        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "1";
        final AtomicReference<EventListener<RemoteLibraryEvent>> indexListener = new AtomicReference<EventListener<RemoteLibraryEvent>>();

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary1).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<RemoteLibraryEvent>>(indexListener));
                allowing(presenceLibrary1).getState();
                will(returnValue(RemoteLibraryState.LOADING));
                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                
                allowing(remoteFileItem1).getProperty(FilePropertyKey.NAME);
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.values()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    will(returnValue(null));
                }
                
                exactly(4).of(presenceLibrary1).get(0);
                will(returnValue(remoteFileItem1));
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem1), 0));
        waitForProcessingQueue(friendLibraries);
        
        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        Collection<SearchResult> matchingItems = friendLibraries
                .getMatchingItems(new TestSearchDetails("name", SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.DOCUMENT));
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("blah",
                SearchCategory.AUDIO));
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.ALL));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        
        // now clear presence library and assert that no suggestions are found
        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsClearedEvent(presenceLibrary1));
        waitForProcessingQueue(friendLibraries);
        
        suggestions = friendLibraries.getSuggestions("name", SearchCategory.AUDIO);
        assertEquals(0, suggestions.size());
                
        context.assertIsSatisfied();
    }

    private void waitForProcessingQueue(FriendLibraries friendLibraries) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        friendLibraries.processingQueue.execute(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            } 
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests search for remote file items in the friends library by name. Using
     * a single friend and presence in this instance.
     */
    @SuppressWarnings("unchecked")
    public void testIndexing1FriendLibraryAndMultipleFilesByFileNameOnly() throws Throwable {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;

        final SearchResult remoteFileItem2 = context.mock(SearchResult.class);
        final String name2 = "name2";
        final Category category2 = Category.DOCUMENT;

        final SearchResult remoteFileItem3 = context.mock(SearchResult.class);
        final String name3 = "blah1";
        final Category category3 = Category.AUDIO;

        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "1";
        final AtomicReference<EventListener<RemoteLibraryEvent>> indexListener = new AtomicReference<EventListener<RemoteLibraryEvent>>();

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));
                
                allowing(presenceLibrary1).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<RemoteLibraryEvent>>(indexListener));
                allowing(presenceLibrary1).getState();
                will(returnValue(RemoteLibraryState.LOADING));
                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                one(presenceLibrary1).removeListener(with(any(EventListener.class)));
                
                allowing(remoteFileItem1).getProperty(FilePropertyKey.NAME);
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                allowing(remoteFileItem2).getProperty(FilePropertyKey.NAME);
                will(returnValue(name2));
                allowing(remoteFileItem2).getCategory();
                will(returnValue(category2));

                allowing(remoteFileItem3).getProperty(FilePropertyKey.NAME);
                will(returnValue(name3));
                allowing(remoteFileItem3).getCategory();
                will(returnValue(category3));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.values()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem2).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem3).getProperty(filePropertyKey);
                    will(returnValue(null));
                }
                
                exactly(4).of(presenceLibrary1).get(0);
                will(returnValue(remoteFileItem1));
                exactly(4).of(presenceLibrary1).get(1);
                will(returnValue(remoteFileItem2));
                exactly(1).of(presenceLibrary1).get(2);
                will(returnValue(remoteFileItem3));
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem1), 0));
        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Arrays.asList(remoteFileItem2, remoteFileItem3), 1));
        waitForProcessingQueue(friendLibraries);

        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        Collection<SearchResult> matchingItems = friendLibraries
                .getMatchingItems(new TestSearchDetails("name", SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.DOCUMENT));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.DOCUMENT));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name3);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("blah",
                SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem3);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.ALL));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);
        
        // now remove presence library to ensure completion items are gone too
        presenceLibraryList1.remove(presenceLibrary1);
        waitForProcessingQueue(friendLibraries);
        
        suggestions = friendLibraries.getSuggestions("name", SearchCategory.AUDIO);
        assertEquals(0, suggestions.size());
        suggestions = friendLibraries.getSuggestions("bl", SearchCategory.AUDIO);
        assertEquals(0, suggestions.size());
        
        context.assertIsSatisfied();
    }

    /**
     * Tests search for remote file items in the friends library by name. Using
     * a multiple friends and presences.
     */
    @SuppressWarnings("unchecked")
    public void testIndexingMultipleFriendLibraryAndMultipleFilesByFileNameOnly() throws Throwable {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();

        final EventList<PresenceLibrary> presenceLibraryList2 = new BasicEventList<PresenceLibrary>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final FriendLibrary friendLibrary2 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary2 = context.mock(PresenceLibrary.class);

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;

        final SearchResult remoteFileItem2 = context.mock(SearchResult.class);
        final String name2 = "name2";
        final Category category2 = Category.DOCUMENT;

        final SearchResult remoteFileItem3 = context.mock(SearchResult.class);
        final String name3 = "blah1";
        final Category category3 = Category.AUDIO;

        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "1";

        final FriendPresence presence2 = context.mock(FriendPresence.class);
        final String presenceId2 = "2";
        final AtomicReference<EventListener<RemoteLibraryEvent>> indexListener1 = new AtomicReference<EventListener<RemoteLibraryEvent>>();
        final AtomicReference<EventListener<RemoteLibraryEvent>> indexListener2 = new AtomicReference<EventListener<RemoteLibraryEvent>>();

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary1).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<RemoteLibraryEvent>>(indexListener1));
                allowing(presenceLibrary1).getState();
                will(returnValue(RemoteLibraryState.LOADING));
                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                
                allowing(friendLibrary2).getPresenceLibraryList();
                will(returnValue(presenceLibraryList2));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary2).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<RemoteLibraryEvent>>(indexListener2));
                allowing(presenceLibrary2).getState();
                will(returnValue(RemoteLibraryState.LOADING));
                allowing(presenceLibrary2).getPresence();
                will(returnValue(presence2));
                allowing(presence2).getPresenceId();
                will(returnValue(presenceId2));
                
                allowing(remoteFileItem1).getProperty(FilePropertyKey.NAME);
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                allowing(remoteFileItem2).getProperty(FilePropertyKey.NAME);
                will(returnValue(name2));
                allowing(remoteFileItem2).getCategory();
                will(returnValue(category2));

                allowing(remoteFileItem3).getProperty(FilePropertyKey.NAME);
                will(returnValue(name3));
                allowing(remoteFileItem3).getCategory();
                will(returnValue(category3));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.values()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem2).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem3).getProperty(filePropertyKey);
                    will(returnValue(null));
                }
                
                exactly(4).of(presenceLibrary1).get(0);
                will(returnValue(remoteFileItem1));
                exactly(4).of(presenceLibrary2).get(1);
                will(returnValue(remoteFileItem2));
                exactly(1).of(presenceLibrary2).get(2);
                will(returnValue(remoteFileItem3));
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);
        presenceLibraryList1.add(presenceLibrary1);

        friendLibraryList.add(friendLibrary2);
        presenceLibraryList2.add(presenceLibrary2);

        indexListener1.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem1), 0));
        indexListener2.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Arrays.asList(remoteFileItem2, remoteFileItem3), 1));
        waitForProcessingQueue(friendLibraries);

        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        Collection<SearchResult> matchingItems = friendLibraries
                .getMatchingItems(new TestSearchDetails("name", SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.DOCUMENT));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.DOCUMENT));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name3);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("blah",
                SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem3);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.ALL));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);
        context.assertIsSatisfied();
    }

    /**
     * Testing search for friends files by metadata.
     */
    @SuppressWarnings("unchecked")
    public void testIndexingFileMetaData() throws Throwable {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;
        final Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        final String album1 = "nameo";
        properties1.put(FilePropertyKey.ALBUM, album1);

        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "1";
        final AtomicReference<EventListener<RemoteLibraryEvent>> indexListener = new AtomicReference<EventListener<RemoteLibraryEvent>>();

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));
                
                allowing(presenceLibrary1).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<RemoteLibraryEvent>>(indexListener));
                allowing(presenceLibrary1).getState();
                will(returnValue(RemoteLibraryState.LOADING));
                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                
                allowing(remoteFileItem1).getProperty(FilePropertyKey.NAME);
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    if (properties1.containsKey(filePropertyKey)) {
                        will(returnValue(properties1.get(filePropertyKey)));
                    } else {
                        will(returnValue(null));
                    }
                }
                
                exactly(5).of(presenceLibrary1).get(0);
                will(returnValue(remoteFileItem1));
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem1), 0));
        waitForProcessingQueue(friendLibraries);

        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        Collection<SearchResult> matchingItems = friendLibraries
                .getMatchingItems(new TestSearchDetails("name", SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.DOCUMENT));
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("blah",
                SearchCategory.AUDIO));
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.AUDIO));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("na",
                SearchCategory.ALL));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("nameo", SearchCategory.ALL);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("nameo",
                SearchCategory.ALL));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        context.assertIsSatisfied();

        suggestions = friendLibraries.getSuggestions("nameo", SearchCategory.ALL,
                FilePropertyKey.ALBUM);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, album1);

        suggestions = friendLibraries
                .getSuggestions("n", SearchCategory.ALL, FilePropertyKey.ALBUM);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, album1);

        suggestions = friendLibraries.getSuggestions("n", SearchCategory.AUDIO,
                FilePropertyKey.ALBUM);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, album1);

        suggestions = friendLibraries.getSuggestions("nameo", SearchCategory.AUDIO,
                FilePropertyKey.TITLE);
        assertEmpty(suggestions);

        context.assertIsSatisfied();
    }

    /**
     * Testing search for friends files with advancedDetails.
     */
    @SuppressWarnings("unchecked")
    public void testAdvancedSearch() throws Throwable {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;
        final Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        final String author1 = "nameo1";
        properties1.put(FilePropertyKey.AUTHOR, author1);

        final SearchResult remoteFileItem2 = context.mock(SearchResult.class);
        final String name2 = "name2";
        final Category category2 = Category.DOCUMENT;
        final Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        final String author2 = "nameo2";
        properties2.put(FilePropertyKey.AUTHOR, author2);

        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "1";
        final AtomicReference<EventListener<RemoteLibraryEvent>> indexListener = new AtomicReference<EventListener<RemoteLibraryEvent>>();

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));
                
                allowing(presenceLibrary1).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<RemoteLibraryEvent>>(indexListener));
                allowing(presenceLibrary1).getState();
                will(returnValue(RemoteLibraryState.LOADING));
                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));

                allowing(remoteFileItem1).getProperty(FilePropertyKey.NAME);
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    if (properties1.containsKey(filePropertyKey)) {
                        will(returnValue(properties1.get(filePropertyKey)));
                    } else {
                        will(returnValue(null));
                    }
                }

                allowing(remoteFileItem2).getProperty(FilePropertyKey.NAME);
                will(returnValue(name2));
                allowing(remoteFileItem2).getCategory();
                will(returnValue(category2));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    allowing(remoteFileItem2).getProperty(filePropertyKey);
                    if (properties2.containsKey(filePropertyKey)) {
                        will(returnValue(properties2.get(filePropertyKey)));
                    } else {
                        will(returnValue(null));
                    }
                }
                
                exactly(7).of(presenceLibrary1).get(0);
                will(returnValue(remoteFileItem1));
                exactly(8).of(presenceLibrary1).get(1);
                will(returnValue(remoteFileItem2));
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem1), 0));
        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem2), 1));
        waitForProcessingQueue(friendLibraries);

        Map<FilePropertyKey, String> advancedDetails1 = new HashMap<FilePropertyKey, String>();
        advancedDetails1.put(FilePropertyKey.AUTHOR, "n");

        Collection<SearchResult> matchingItems = friendLibraries
                .getMatchingItems(new TestSearchDetails("", SearchCategory.AUDIO, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("",
                SearchCategory.DOCUMENT, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("n",
                SearchCategory.DOCUMENT));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("n",
                SearchCategory.ALL));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name1",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name2",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        Map<FilePropertyKey, String> advancedDetails2 = new HashMap<FilePropertyKey, String>();
        advancedDetails2.put(FilePropertyKey.AUTHOR, "nameo");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails2));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        Map<FilePropertyKey, String> advancedDetails3 = new HashMap<FilePropertyKey, String>();
        advancedDetails3.put(FilePropertyKey.AUTHOR, "nameo1");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails3));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        Map<FilePropertyKey, String> advancedDetails4 = new HashMap<FilePropertyKey, String>();
        advancedDetails4.put(FilePropertyKey.AUTHOR, "nameo2");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails4));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);
    }

    /**
     * Testing indexing phrases. Suggestions only index the phrase, while the
     * matches are found based on indexing all of the parts of the phrase.
     */
    @SuppressWarnings("unchecked")
    public void testIndexingPhrases() throws Throwable {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();
     
        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final SearchResult remoteFileItem1 = context.mock(SearchResult.class);
        final String name1 = "name 1";
        final Category category1 = Category.AUDIO;
        final Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        final String author1 = "nameo 1";
        properties1.put(FilePropertyKey.AUTHOR, author1);

        final SearchResult remoteFileItem2 = context.mock(SearchResult.class);
        final String name2 = "name 2";
        final Category category2 = Category.DOCUMENT;
        final Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        final String author2 = "nameo 2";
        properties2.put(FilePropertyKey.AUTHOR, author2);

        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final String presenceId1 = "1";
        final AtomicReference<EventListener<RemoteLibraryEvent>> indexListener = new AtomicReference<EventListener<RemoteLibraryEvent>>();

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary1).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<RemoteLibraryEvent>>(indexListener));
                allowing(presenceLibrary1).getState();
                will(returnValue(RemoteLibraryState.LOADING));
                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));

                allowing(remoteFileItem1).getProperty(FilePropertyKey.NAME);
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    if (properties1.containsKey(filePropertyKey)) {
                        will(returnValue(properties1.get(filePropertyKey)));
                    } else {
                        will(returnValue(null));
                    }
                }

                allowing(remoteFileItem2).getProperty(FilePropertyKey.NAME);
                will(returnValue(name2));
                allowing(remoteFileItem2).getCategory();
                will(returnValue(category2));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    allowing(remoteFileItem2).getProperty(filePropertyKey);
                    if (properties2.containsKey(filePropertyKey)) {
                        will(returnValue(properties2.get(filePropertyKey)));
                    } else {
                        will(returnValue(null));
                    }
                }
                
                exactly(9).of(presenceLibrary1).get(0);
                will(returnValue(remoteFileItem1));
                exactly(10).of(presenceLibrary1).get(1);
                will(returnValue(remoteFileItem2));
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem1), 0));
        indexListener.get().handleEvent(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary1, Collections.singleton(remoteFileItem2), 1));
        waitForProcessingQueue(friendLibraries);

        Collection<String> suggestions = friendLibraries.getSuggestions("", SearchCategory.ALL);
        assertEquals(4, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);
        assertContains(suggestions, author1);
        assertContains(suggestions, author2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(4, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);
        assertContains(suggestions, author1);
        assertContains(suggestions, author2);

        suggestions = friendLibraries.getSuggestions("name ", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        suggestions = friendLibraries.getSuggestions("nameo ", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, author1);
        assertContains(suggestions, author2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL,
                FilePropertyKey.AUTHOR);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, author1);
        assertContains(suggestions, author2);

        suggestions = friendLibraries.getSuggestions("nameo", SearchCategory.ALL,
                FilePropertyKey.AUTHOR);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, author1);
        assertContains(suggestions, author2);

        suggestions = friendLibraries.getSuggestions("nameo 1", SearchCategory.ALL,
                FilePropertyKey.AUTHOR);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, author1);

        suggestions = friendLibraries.getSuggestions("nameo 2", SearchCategory.ALL,
                FilePropertyKey.AUTHOR);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, author2);

        Map<FilePropertyKey, String> advancedDetails1 = new HashMap<FilePropertyKey, String>();
        advancedDetails1.put(FilePropertyKey.AUTHOR, "n");

        Collection<SearchResult> matchingItems = friendLibraries
                .getMatchingItems(new TestSearchDetails("", SearchCategory.AUDIO, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("",
                SearchCategory.DOCUMENT, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("n",
                SearchCategory.DOCUMENT));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("n",
                SearchCategory.ALL));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name 1",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("1",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name 2",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("2",
                SearchCategory.ALL, advancedDetails1));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        Map<FilePropertyKey, String> advancedDetails2 = new HashMap<FilePropertyKey, String>();
        advancedDetails2.put(FilePropertyKey.AUTHOR, "nameo");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails2));
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        Map<FilePropertyKey, String> advancedDetails3 = new HashMap<FilePropertyKey, String>();
        advancedDetails3.put(FilePropertyKey.AUTHOR, "nameo 1");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails3));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        Map<FilePropertyKey, String> advancedDetails4 = new HashMap<FilePropertyKey, String>();
        advancedDetails4.put(FilePropertyKey.AUTHOR, "nameo 2");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("name",
                SearchCategory.ALL, advancedDetails4));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        Map<FilePropertyKey, String> advancedDetails5 = new HashMap<FilePropertyKey, String>();
        advancedDetails5.put(FilePropertyKey.AUTHOR, "1");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("",
                SearchCategory.ALL, advancedDetails5));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        Map<FilePropertyKey, String> advancedDetails6 = new HashMap<FilePropertyKey, String>();
        advancedDetails6.put(FilePropertyKey.AUTHOR, "2");

        matchingItems = friendLibraries.getMatchingItems(new TestSearchDetails("",
                SearchCategory.ALL, advancedDetails6));
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);
    }

    private class TestSearchDetails implements SearchDetails {

        private final SearchCategory category;

        private final String query;

        private final SearchType searchType;

        private final Map<FilePropertyKey, String> advancedDetails;

        public TestSearchDetails(String query, SearchCategory category,
                Map<FilePropertyKey, String> advancedDetails) {
            this.category = category;
            this.query = query;
            this.searchType = SearchType.KEYWORD;
            this.advancedDetails = advancedDetails;
        }

        public TestSearchDetails(String query, SearchCategory category) {
            this(query, category, new HashMap<FilePropertyKey, String>());
        }

        @Override
        public Map<FilePropertyKey, String> getAdvancedDetails() {
            return advancedDetails;
        }

        @Override
        public SearchCategory getSearchCategory() {
            return category;
        }

        @Override
        public String getSearchQuery() {
            return query;
        }

        @Override
        public SearchType getSearchType() {
            return searchType;
        }

    }

}
