package org.limewire.core.impl.library;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.library.RemoteLibraryManagerImpl.PresenceLibraryImpl;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;

public class PresenceLibraryImplTest extends BaseTestCase {

    
    private Mockery context;
    private EventListener<RemoteLibraryEvent> listener;
    private SearchResult searchResult1;
    private PresenceLibraryImpl presenceLibrary;
    private SearchResult searchResult2;

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        listener = context.mock(EventListener.class);
        searchResult1 = context.mock(SearchResult.class);
        searchResult2 = context.mock(SearchResult.class);
        presenceLibrary = new PresenceLibraryImpl(null);
        presenceLibrary.addListener(listener);
    }
    
    public void testAddNewResult() {
        context.checking(new Expectations() {{
            one(listener).handleEvent(with(new RemoteLibraryEventMatcher(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary, Collections.singleton(searchResult1), 0))));
        }});
        presenceLibrary.addNewResult(searchResult1);
        assertEquals(1, presenceLibrary.size());
        context.assertIsSatisfied();
    }
    
    public void testSetNewResults() {
        context.checking(new Expectations() {{
            one(listener).handleEvent(with(new RemoteLibraryEventMatcher(RemoteLibraryEvent.createResultsClearedEvent(presenceLibrary))));
            one(listener).handleEvent(with(new RemoteLibraryEventMatcher(RemoteLibraryEvent.createResultsAddedEvent(presenceLibrary, Arrays.asList(searchResult1, searchResult2), 0))));
        }});
        presenceLibrary.setNewResults(Arrays.asList(searchResult1, searchResult2));
        assertEquals(2, presenceLibrary.size());
        context.assertIsSatisfied();
    }
    
    public void testClear() {
        context.checking(new Expectations() {{
            one(listener).handleEvent(with(new RemoteLibraryEventMatcher(RemoteLibraryEvent.createResultsClearedEvent(presenceLibrary))));
        }});
        presenceLibrary.clear();
        assertEquals(0, presenceLibrary.size());
        context.assertIsSatisfied();
    }
    
    public void testIterator() {
        context.checking(new Expectations() {{
            ignoring(listener);
        }});
        presenceLibrary.setNewResults(Arrays.asList(searchResult1, searchResult2));
        assertEquals(2, presenceLibrary.size());
        Iterator<SearchResult> iterator = presenceLibrary.iterator();
        assertTrue(iterator.hasNext());
        assertSame(searchResult1, iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(searchResult2, iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    /**
     * Expectation is that next call still returns next element, but
     * then hasNext() returns false.
     */
    public void testIteratorIsClearedBeforeNextCall() {
        context.checking(new Expectations() {{
            ignoring(listener);
        }});
        presenceLibrary.setNewResults(Arrays.asList(searchResult1, searchResult2));
        assertEquals(2, presenceLibrary.size());
        Iterator<SearchResult> iterator = presenceLibrary.iterator();
        assertTrue(iterator.hasNext());
        presenceLibrary.clear();
        assertSame(searchResult1, iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    public void testIteratorIsClearedBeforeHasNextCall() {
        context.checking(new Expectations() {{
            ignoring(listener);
        }});
        presenceLibrary.setNewResults(Arrays.asList(searchResult1, searchResult2));
        assertEquals(2, presenceLibrary.size());
        Iterator<SearchResult> iterator = presenceLibrary.iterator();
        assertTrue(iterator.hasNext());
        assertSame(searchResult1, iterator.next());
        presenceLibrary.clear();
        assertFalse(iterator.hasNext());
    }
    
    static class RemoteLibraryEventMatcher extends TypeSafeMatcher<RemoteLibraryEvent> {

        private final RemoteLibraryEvent expectedEvent;

        public RemoteLibraryEventMatcher(RemoteLibraryEvent expectedEvent) {
            this.expectedEvent = expectedEvent;
        }
        
        @Override
        public boolean matchesSafely(RemoteLibraryEvent event) {
            return expectedEvent.getSource() == event.getSource() &&
            expectedEvent.getType() == event.getType() && 
            expectedEvent.getState() == event.getState() &&
            expectedEvent.getAddedResults().equals(event.getAddedResults()) &&
            expectedEvent.getStartIndex() == event.getStartIndex();
        }   

        @Override
        public void describeTo(Description description) {
        }
        
    }
}
