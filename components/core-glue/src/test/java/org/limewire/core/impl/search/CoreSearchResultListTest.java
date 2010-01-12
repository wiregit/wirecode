package org.limewire.core.impl.search;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.util.BaseTestCase;

import com.google.inject.Provider;
import com.limegroup.gnutella.URN;

public class CoreSearchResultListTest extends BaseTestCase {
    /** Instance of class being tested. */
    private CoreSearchResultList model;
    private Provider<PropertiableHeadings> provider;
    private Mockery context;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public CoreSearchResultListTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new Mockery();
        provider = context.mock(Provider.class);
        // Create test instance.
        model = new CoreSearchResultList(new TestSearch(), new TestSearchDetails());
    }
    
    @Override
    protected void tearDown() throws Exception {
        model = null;
        super.tearDown();
    }

    /** Make a search that has no results. */
    public void testEmptySearch() {
        // model is already the CoreSearchResultList, made from a TestSearch
        assertEquals(0, model.getSearchResults().size()); // should be 0 results
    }
    
    /** Make a search that gets a result, and see it there. */
    public void testSearchThenResult() throws Exception {
        SearchResult result = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        model.addResult(result);
        assertEquals(1, model.getSearchResults().size()); // confirm the result is in there
    }
    
    /** A search gets two different results. */
    public void testSearchTwoDifferentResults() throws Exception {
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2")); // different hash
        model.addResult(result1);
        model.addResult(result2);
        assertEquals(2, model.getSearchResults().size()); // different, so both listed separately
    }
    
    /** A search gets two results that share the same URN, and get grouped together. */
    public void testSearchTwoIdenticalResults() throws Exception {
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1")); // same hash
        model.addResult(result1);
        model.addResult(result2);
        //TODO remove Not below when grouping is implemented
        assertNotEquals(1, model.getSearchResults().size()); // same file hash, so both were combined
        // Verify grouped results count.
        assertEquals(1, model.getGroupedResults().size()); // same file hash, so both were combined
        // Verify total results count.
        assertEquals(2, model.getResultCount());
    }

    
    
    
    
    
    
    
    /**
     * Test implementation of Search.
     */
    private static class TestSearch implements Search {

        @Override
        public void addSearchListener(SearchListener searchListener) {
        }

        @Override
        public void removeSearchListener(SearchListener searchListener) {
        }

        @Override
        public SearchCategory getCategory() {
            return null;
        }

        @Override
        public void repeat() {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    }
    
    /**
     * Test implementation of SearchDetails.
     */
    private static class TestSearchDetails implements SearchDetails {

        @Override
        public Map<FilePropertyKey, String> getAdvancedDetails() {
            return null;
        }

        @Override
        public SearchCategory getSearchCategory() {
            return null;
        }

        @Override
        public String getSearchQuery() {
            return null;
        }

        @Override
        public SearchType getSearchType() {
            return null;
        }
    }
    
    /**
     * A dummy SearchResult just for the tests here.
     */
    private static class TestSearchResult implements SearchResult {
        
        public TestSearchResult(URN urn) {
            this.urn = urn;
        }
        
        /** The URN value is real. */
        private final URN urn;

        @Override
        public Category getCategory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getFileExtension() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getFileName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getFileNameWithoutExtension() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getMagnetURL() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getProperty(FilePropertyKey key) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public float getRelevance(String query) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getSize() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public List<RemoteHost> getSources() {
            return Collections.<RemoteHost>singletonList(new TestRemoteHost());
        }

        @Override
        public URN getUrn() {
            return urn;
        }

        @Override
        public boolean isLicensed() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isSpam() {
            // TODO Auto-generated method stub
            return false;
        }
    }
    
    private static class TestRemoteHost implements RemoteHost {
        
        @Override
        public FriendPresence getFriendPresence() {
            return new FriendPresence() {
                @Override
                public void addFeature(Feature feature) {
                }

                @Override
                public <D, F extends Feature<D>> void addTransport(Class<F> clazz,
                        FeatureTransport<D> transport) {
                }

                @Override
                public Feature getFeature(URI id) {
                    return null;
                }

                @Override
                public Collection<Feature> getFeatures() {
                    return null;
                }

                @Override
                public Friend getFriend() {
                    return new Friend() {
                        @Override
                        public void addPresenceListener(
                                EventListener<PresenceEvent> presenceListener) {
                        }

                        @Override
                        public MessageWriter createChat(MessageReader reader) {
                            return null;
                        }

                        @Override
                        public FriendPresence getActivePresence() {
                            return null;
                        }

                        @Override
                        public String getFirstName() {
                            return null;
                        }

                        @Override
                        public String getId() {
                            return null;
                        }

                        @Override
                        public String getName() {
                            return null;
                        }

                        @Override
                        public Network getNetwork() {
                            return null;
                        }

                        @Override
                        public Map<String, FriendPresence> getPresences() {
                            return null;
                        }

                        @Override
                        public String getRenderName() {
                            return "test";
                        }

                        @Override
                        public boolean hasActivePresence() {
                            return false;
                        }

                        @Override
                        public boolean isAnonymous() {
                            return false;
                        }

                        @Override
                        public boolean isSignedIn() {
                            return false;
                        }

                        @Override
                        public boolean isSubscribed() {
                            return false;
                        }

                        @Override
                        public void removeChatListener() {
                        }

                        @Override
                        public void setChatListenerIfNecessary(IncomingChatListener listener) {
                        }

                        @Override
                        public void setName(String name) {
                        }
                    };
                }

                @Override
                public Mode getMode() {
                    return null;
                }

                @Override
                public String getPresenceId() {
                    return null;
                }

                @Override
                public int getPriority() {
                    return 0;
                }

                @Override
                public String getStatus() {
                    return null;
                }

                @Override
                public <F extends Feature<D>, D> FeatureTransport<D> getTransport(Class<F> feature) {
                    return null;
                }

                @Override
                public Type getType() {
                    return null;
                }

                @Override
                public boolean hasFeatures(URI... id) {
                    return false;
                }

                @Override
                public void removeFeature(URI id) {
                }
            };
        }

        @Override
        public boolean isBrowseHostEnabled() {
            return false;
        }

        @Override
        public boolean isChatEnabled() {
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            return false;
        }
    }
}
