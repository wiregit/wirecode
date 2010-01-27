package org.limewire.core.impl.search;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for CoreSearchManager.
 */
public class CoreSearchManagerTest extends BaseTestCase {
    /** Instance of class being tested. */
    CoreSearchManager searchManager;

    private Mockery context = new Mockery();
    
    /**
     * Constructs a test case for the specified method name.
     */
    public CoreSearchManagerTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        searchManager = new CoreSearchManager();
    }    

    @Override
    protected void tearDown() throws Exception {
        searchManager = null;
        super.tearDown();
    }
    
    public void testAddSearch() {
        // Create mock objects.
        final Search mockSearch = context.mock(Search.class);
        final SearchDetails mockDetails = context.mock(SearchDetails.class);
        context.checking(new Expectations() {{
            allowing(mockSearch);
            allowing(mockDetails);
        }});
        
        // Add search.
        SearchResultList resultList = searchManager.addSearch(mockSearch, mockDetails);
        assertNotNull(resultList);
        
        // Verify search listed.
        assertSame(resultList, searchManager.getSearchResultList(mockSearch));
    }
    
    public void testRemoveSearch() {
        // Create mock objects.
        final Search mockSearch = context.mock(Search.class);
        final SearchDetails mockDetails = context.mock(SearchDetails.class);
        context.checking(new Expectations() {{
            allowing(mockSearch);
            allowing(mockDetails);
        }});
        
        // Add search.
        SearchResultList resultList = searchManager.addSearch(mockSearch, mockDetails);
        assertNotNull(resultList);
        
        // Remove search and verify.
        searchManager.removeSearch(mockSearch);
        resultList = searchManager.getSearchResultList(mockSearch);
        assertNull(resultList);
    }

}
