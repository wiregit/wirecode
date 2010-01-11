package org.limewire.core.impl.search;

import java.util.List;

import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import com.limegroup.gnutella.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.GUID;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.util.BaseTestCase;

import com.google.inject.Provider;

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
        model = new CoreSearchResultList(new TestSearch());
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
        model.addSearchResult(result);
        assertEquals(1, model.getSearchResults().size()); // confirm the result is in there
    }
    
    /** A search gets two different results. */
    public void testSearchTwoDifferentResults() throws Exception {
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2")); // different hash
        model.addSearchResult(result1);
        model.addSearchResult(result2);
        assertEquals(2, model.getSearchResults().size()); // different, so both listed separately
    }
    
    /** A search gets two results that share the same URN, and get grouped together. */
    public void testSearchTwoIdenticalResults() throws Exception {
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1")); // same hash
        model.addSearchResult(result1);
        model.addSearchResult(result2);
        //TODO remove Not below when grouping is implemented
        assertNotEquals(1, model.getSearchResults().size()); // same file hash, so both were combined
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
        public GUID getQueryGuid() {
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
            // TODO Auto-generated method stub
            return null;
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
}
