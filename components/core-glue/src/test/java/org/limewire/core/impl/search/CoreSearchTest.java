package org.limewire.core.impl.search;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.impl.library.FriendSearcher;
import org.limewire.core.settings.PromotionSettings;
import org.limewire.io.Address;
import org.limewire.io.IpPort;
import org.limewire.promotion.PromotionSearcher;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.geocode.CachedGeoLocation;
import com.limegroup.gnutella.messages.QueryReply;

public class CoreSearchTest extends BaseTestCase {

    public CoreSearchTest(String name) {
        super(name);
    }

    public void testSearchListenerResultsAdded() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        PromotionSettings.PROMOTION_SYSTEM_IS_ENABLED.setValue(false);

        final SearchDetails searchDetails = new TestSearchDetails("test", SearchCategory.ALL,
                SearchType.KEYWORD);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final PromotionSearcher promotionSearcher = context.mock(PromotionSearcher.class);
        final FriendSearcher friendSearcher = context.mock(FriendSearcher.class);
        final CachedGeoLocation geoLocation = context.mock(CachedGeoLocation.class);
        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);

        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };
        final SearchListener searchListener = context.mock(SearchListener.class);
        final AtomicReference<QueryReplyListener> queryReplyListener = new AtomicReference<QueryReplyListener>();
        context.checking(new Expectations() {
            {
                one(searchServices).newQueryGUID();
                will(returnValue(searchGuid));
                one(listenerList).addQueryReplyListener(with(equal(searchGuid)),
                        with(any(QueryReplyListener.class)));
                will(new AssignParameterAction<QueryReplyListener>(queryReplyListener, 1));
                one(searchServices).query(searchGuid, searchDetails.getSearchQuery(), "",
                        MediaType.getAnyTypeMediaType());
                one(backgroundExecutor).execute(with(any(Runnable.class)));
                will(new CustomAction("Run runnable") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Runnable runnable = (Runnable) invocation.getParameter(0);
                        runnable.run();
                        return null;
                    }
                });
                one(friendSearcher).doSearch(with(equal(searchDetails)),
                        with(any(FriendSearchListener.class)));
                one(searchListener).searchStarted();
            }
        });

        CoreSearch coreSearch = new CoreSearch(searchDetails, searchServices, listenerList,
                promotionSearcher, friendSearcher, geoLocation, backgroundExecutor);
        coreSearch.addSearchListener(searchListener);

        coreSearch.start();

        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final QueryReply queryReply1 = context.mock(QueryReply.class);
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final Address address1 = context.mock(Address.class);
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
                one(searchListener).handleSearchResult(with(new BaseMatcher<SearchResult>() {
                    @Override
                    public boolean matches(Object item) {
                        if (!SearchResult.class.isInstance(item)) {
                            return false;
                        }

                        SearchResult searchResult = (SearchResult) item;

                        if (!fileName1.equals(searchResult.getFileName())) {
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public void describeTo(Description description) {

                    }
                }));
            }
        });
        queryReplyListener.get().handleQueryReply(remoteFileDesc1, queryReply1, ipPorts);

        context.assertIsSatisfied();
    }

    private final class TestSearchDetails implements SearchDetails {

        private final SearchCategory searchCategory;

        private final String query;

        private final SearchType searchType;

        public TestSearchDetails(String query, SearchCategory searchCategory, SearchType searchType) {
            this.query = query;
            this.searchCategory = searchCategory;
            this.searchType = searchType;
        }

        @Override
        public SearchCategory getSearchCategory() {
            return searchCategory;
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
