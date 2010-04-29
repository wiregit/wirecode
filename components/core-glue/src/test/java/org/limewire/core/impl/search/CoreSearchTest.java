package org.limewire.core.impl.search;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchEvent;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.listener.EventBroadcaster;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;

public class CoreSearchTest extends BaseTestCase {

    public CoreSearchTest(String name) {
        super(name);
    }

    @SuppressWarnings({"unchecked"})
    public void testQueryReplySearchListenerResultsAdded() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};

        final SearchDetails searchDetails = context.mock(SearchDetails.class);
        final String searchQuery = "test";
        
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = context.mock(EventBroadcaster.class);
        final RemoteFileDescAdapter.Factory rfdaFactory = context.mock(RemoteFileDescAdapter.Factory.class);

        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        
        final SearchListener searchListener = context.mock(SearchListener.class);
        final AtomicReference<QueryReplyListener> queryReplyListener = new AtomicReference<QueryReplyListener>();
        final CoreSearch coreSearch = new CoreSearch(searchDetails, searchServices, listenerList,
                searchEventBroadcaster, null, null, rfdaFactory);

        context.checking(new Expectations() {{
                allowing(searchDetails).getSearchQuery();
                will(returnValue(searchQuery));
                allowing(searchDetails).getSearchCategory();
                will(returnValue(SearchCategory.ALL));
                allowing(searchDetails).getSearchType();
                will(returnValue(SearchType.KEYWORD));
                allowing(searchDetails);
        }});
        
        context.checking(new Expectations() {{
                one(searchEventBroadcaster).broadcast(
                        new SearchEvent(coreSearch, SearchEvent.Type.STARTED));
                one(searchServices).newQueryGUID();
                will(returnValue(searchGuid));
                one(listenerList).addQueryReplyListener(with(equal(searchGuid)),
                        with(any(QueryReplyListener.class)));
                will(new AssignParameterAction<QueryReplyListener>(queryReplyListener, 1));
                one(searchServices).mutateQuery(searchQuery);
                will(returnValue(searchQuery));
                one(searchServices).query(searchGuid, searchQuery, "",
                        SearchCategory.ALL);
                one(searchListener).searchStarted(coreSearch);
        }});

        coreSearch.addSearchListener(searchListener);

        coreSearch.start();
        
        try {
            coreSearch.start();
            fail("Illegal search repeat not handled");
        } catch (IllegalStateException e) {
            // Expected
        }

        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final QueryReply queryReply1 = context.mock(QueryReply.class);
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final Address address1 = context.mock(Address.class);
        final RemoteFileDescAdapter rfda = context.mock(RemoteFileDescAdapter.class);
        final byte[] guid1 = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final String fileName1 = "remote file name 1.txt";
        context.checking(new Expectations() {
            {
                allowing(remoteFileDesc1).getAddress();
                will(returnValue(address1));
                allowing(remoteFileDesc1).getClientGUID();
                will(returnValue(guid1));
                allowing(address1).getAddressDescription();
                will(returnValue("address 1 description"));
                allowing(remoteFileDesc1).getFileName();
                will(returnValue(fileName1));
                allowing(remoteFileDesc1).getSize();
                will(returnValue(1234L));
                allowing(remoteFileDesc1).getXMLDocument();
                will(returnValue(null));
                allowing(remoteFileDesc1).getCreationTime();
                will(returnValue(5678L));
                allowing(remoteFileDesc1).getSHA1Urn();
                will(returnValue(null));
                allowing(rfdaFactory).create(remoteFileDesc1, ipPorts);
                will(returnValue(rfda));
                one(searchListener).handleSearchResult(with(same(coreSearch)),
                        with(same(rfda)));
            }
        });
        queryReplyListener.get().handleQueryReply(remoteFileDesc1, queryReply1, ipPorts);

        context.assertIsSatisfied();
    }

    @SuppressWarnings({"unchecked"})
    public void testFriendSearchListenerResultsAdded() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};

        final SearchDetails searchDetails = context.mock(SearchDetails.class);
        final String searchQuery = "test";
        
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = context.mock(EventBroadcaster.class);

        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };
        final SearchListener searchListener = context.mock(SearchListener.class);
        final CoreSearch coreSearch = new CoreSearch(searchDetails, searchServices, listenerList,
                searchEventBroadcaster, null, null, null);

        context.checking(new Expectations() {{
            allowing(searchDetails).getSearchQuery();
            will(returnValue(searchQuery));
            allowing(searchDetails).getSearchCategory();
            will(returnValue(SearchCategory.ALL));
            allowing(searchDetails).getSearchType();
            will(returnValue(SearchType.KEYWORD));
            allowing(searchDetails);
        }});
        
        context.checking(new Expectations() {{
            one(searchEventBroadcaster).broadcast(
                    new SearchEvent(coreSearch, SearchEvent.Type.STARTED));
            one(searchServices).newQueryGUID();
            will(returnValue(searchGuid));
            one(listenerList).addQueryReplyListener(with(equal(searchGuid)),
                    with(any(QueryReplyListener.class)));
            one(searchServices).mutateQuery(searchQuery);
            will(returnValue(searchQuery));
            one(searchServices).query(searchGuid, searchQuery, "",
                    SearchCategory.ALL);
            one(searchListener).searchStarted(coreSearch);
        }});

        coreSearch.addSearchListener(searchListener);

        coreSearch.start();

        final SearchResult searchResult1 = context.mock(SearchResult.class);
        final String fileName1 = "filename1";

        context.checking(new Expectations() {{
                allowing(searchResult1).getFileName();
                will(returnValue(fileName1));
                one(searchListener).handleSearchResults(with(same(coreSearch)),
                        with(new SearchResultsListMatcher(Collections.singletonList(fileName1))));
        }});

        context.assertIsSatisfied();
    }

    private final class SearchResultsListMatcher extends BaseMatcher<Collection<? extends SearchResult>> {
        private final List<String> names;

        private SearchResultsListMatcher(List<String> names) {
            this.names = names;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object item) {
            Collection<? extends SearchResult> searchResults = (Collection<? extends SearchResult>)item;
            assertEquals(item.toString(), names.size(), searchResults.size());
            int i = 0;
            for(SearchResult result : searchResults) {
                assertEquals(names.get(i++), result.getFileName());
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {

        }
    }

    @SuppressWarnings("unchecked")
    public void testStop() {
        Mockery context = new Mockery();
        
        final byte[] guid = new byte[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = context.mock(EventBroadcaster.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final SearchListener listener = context.mock(SearchListener.class);
        
        final CoreSearch search = new CoreSearch(null, searchServices, listenerList, searchEventBroadcaster, null, null, null);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(searchEventBroadcaster).broadcast(with(any(SearchEvent.class)));
                exactly(1).of(listenerList).removeQueryReplyListener(with(same(guid)),
                        with(any(QueryReplyListener.class)));
                exactly(1).of(searchServices).stopQuery(new GUID(guid));
                exactly(1).of(listener).searchStopped(search);
            }});
            
        search.stop();

        search.searchGuid = guid;
        search.processingStarted.set(true);
        search.addSearchListener(listener);
        search.stop();
        
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    public void testRestart() {
        Mockery context = new Mockery();
        
        final byte[] guid1 = new byte[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        final byte[] guid2 = new byte[] {0,1,2,3,4,5,6,'x','n',9,10,11,12,13,14,15};
        
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = context.mock(EventBroadcaster.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final SearchListener listener = context.mock(SearchListener.class);
        final SearchDetails details = context.mock(SearchDetails.class);
        
        final CoreSearch search = new CoreSearch(details, searchServices,
                listenerList, searchEventBroadcaster, null, null, null);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(searchEventBroadcaster).broadcast(new SearchEvent(search, SearchEvent.Type.STOPPED));
                exactly(1).of(listenerList).removeQueryReplyListener(with(same(guid1)),
                        with(any(QueryReplyListener.class)));
                exactly(1).of(searchServices).stopQuery(new GUID(guid1));
                exactly(1).of(listener).searchStopped(search);
                exactly(1).of(listener).searchStarted(with(any(Search.class)));
                exactly(1).of(searchEventBroadcaster).broadcast(new SearchEvent(search, SearchEvent.Type.STARTED));
                exactly(1).of(listenerList).addQueryReplyListener(with(same(guid2)),
                        with(any(QueryReplyListener.class)));
                exactly(1).of(searchServices).queryWhatIsNew(with(same(guid2)),
                        with(same(SearchCategory.OTHER)));
                
                allowing(searchServices).newQueryGUID();
                will(returnValue(guid2));
                allowing(details).getSearchType();
                will(returnValue(SearchType.WHATS_NEW));
                allowing(details).getSearchCategory();
                will(returnValue(SearchCategory.OTHER));
                
            }});
            
        
        try {
            search.repeat();
            fail("Should not be able to repeat a search that has not started");
        } catch (IllegalStateException e) {
            // Expected
        }

        search.searchGuid = guid1;
        search.processingStarted.set(true);
        search.addSearchListener(listener);
        search.repeat();
        
        context.assertIsSatisfied();
    }
    
    public void testGetCategory() {
        Mockery context = new Mockery();
        
        final SearchDetails searchDetails = context.mock(SearchDetails.class);
        
        final CoreSearch coreSearch = new CoreSearch(searchDetails, null, null,
                null, null, null, null);
        
        context.checking(new Expectations() {{
            one(searchDetails).getSearchCategory();
            will(returnValue(SearchCategory.IMAGE));
            one(searchDetails).getSearchCategory();
            will(returnValue(SearchCategory.DOCUMENT));
            allowing(searchDetails);
        }});
        
        
        assertEquals(SearchCategory.IMAGE, coreSearch.getCategory());
        assertEquals(SearchCategory.DOCUMENT, coreSearch.getCategory());
    }
    
    public void testGetQueryGuid() {
        final CoreSearch coreSearch = new CoreSearch(null, null, null, null,
                null, null, null);
        
        coreSearch.searchGuid = new byte[] {4,3,2,1,'q','x','x','x','x','x','x','x','x','x','x','x'};
        
        assertEquals(new GUID(new byte[] {4,3,2,1,'q','x','x','x','x','x','x','x','x','x','x','x'}),
                coreSearch.getQueryGuid());
    }
}
