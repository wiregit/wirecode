package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.related.RelatedFiles;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;

/**
 * JUnit test case for CoreSearchResultList.
 */
public class CoreSearchResultListTest extends BaseTestCase {
    /** Instance of class being tested. */
    private CoreSearchResultList resultList;
    
    private Mockery context = new Mockery();
    
    /**
     * Constructs a test case for the specified method name.
     */
    public CoreSearchResultListTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock search objects.
        final Search mockSearch = context.mock(Search.class);
        final SearchDetails mockDetails = context.mock(SearchDetails.class);
        final RelatedFiles mockRelatedFiles = context.mock(RelatedFiles.class);
        context.checking(new Expectations() {{
            allowing(mockSearch);
            allowing(mockDetails);
            allowing(mockRelatedFiles);
        }});
        
        // Create test instance.
        resultList = new CoreSearchResultList(mockSearch, mockDetails, mockRelatedFiles);
    }
    
    @Override
    protected void tearDown() throws Exception {
        resultList = null;
        super.tearDown();
    }

    /** Make a search that has no results. */
    public void testEmptySearch() {
        // model is already the CoreSearchResultList, made from a TestSearch
        assertEquals(0, resultList.getGroupedResults().size()); // should be 0 results
    }
    
    /** Make a search that gets a result, and see it there. */
    public void testAddResult() throws Exception {
        SearchResult result = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        resultList.addResult(result);
        
        // confirm the result is in there
        assertEquals(1, resultList.getGroupedResults().size());
        assertEquals(1, resultList.getResultCount());
    }
    
    /** A search gets two different results. */
    public void testAddResultsTwoDifferent() throws Exception {
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2")); // different hash
        
        List<SearchResult> list = new ArrayList<SearchResult>();
        list.add(result1);
        list.add(result2);
        resultList.addResults(list);
        
        // different, so both listed separately
        assertEquals(2, resultList.getGroupedResults().size());
        assertEquals(2, resultList.getResultCount());
    }
    
    /** A search gets two results that share the same URN, and get grouped together. */
    public void testAddResultsWithSameUrn() throws Exception {
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2")); // different hash
        SearchResult result3 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1")); // same hash
        
        List<SearchResult> list = new ArrayList<SearchResult>();
        list.add(result1);
        list.add(result2);
        list.add(result3);
        resultList.addResults(list);
        
        // Verify grouped results count.
        assertEquals(2, resultList.getGroupedResults().size());
        
        // Verify total results count.
        assertEquals(3, resultList.getResultCount());
    }
    
    /** Test method to clear results. */
    public void testClear() throws Exception {
        // Add two results.
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2")); // different hash
        
        List<SearchResult> list = new ArrayList<SearchResult>();
        list.add(result1);
        list.add(result2);
        resultList.addResults(list);
        
        // Verify results added.
        assertEquals(2, resultList.getGroupedResults().size());
        assertEquals(2, resultList.getResultCount());
        
        // Clear result list and verify.
        resultList.clear();
        assertEquals(0, resultList.getGroupedResults().size());
        assertEquals(0, resultList.getResultCount());
    }
    
    /** Tests event notification when results added. */
    public void testListListener() throws Exception {
        // Add listener to list.
        TestSearchListListener listener = new TestSearchListListener();
        resultList.addListener(listener);
        
        // Add result and check listener.
        SearchResult result1 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        resultList.addResult(result1);
        assertEquals(1, listener.getCount());
        
        // Add result with same URN, and check listener.
        listener.reset();
        SearchResult result2 = new TestSearchResult(URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1"));
        resultList.addResult(result2);
        assertEquals(0, listener.getCount());
    }
    
    /**
     * Test implementation of SearchResultListListener.
     */
    private static class TestSearchListListener implements EventListener<Collection<GroupedSearchResult>> {
        private int count = 0;

        @Override
        public void handleEvent(Collection<GroupedSearchResult> results) {
            count = results.size();
        }
        
        public int getCount() {
            return count;
        }
        
        public void reset() {
            count = 0;
        }
    }
}
