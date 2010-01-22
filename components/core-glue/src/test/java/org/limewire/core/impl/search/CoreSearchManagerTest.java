package org.limewire.core.impl.search;

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
        Search search = new TestSearch();
        SearchDetails searchDetails = new TestSearchDetails();
        SearchResultList resultList = searchManager.addSearch(search, searchDetails);
        assertNotNull(resultList);
        
        // Verify search listed.
        resultList = searchManager.getSearchResultList(search);
        assertNotNull(resultList);
    }
    
    public void testRemoveSearch() {
        Search search = new TestSearch();
        SearchDetails searchDetails = new TestSearchDetails();
        SearchResultList resultList = searchManager.addSearch(search, searchDetails);
        assertNotNull(resultList);
        
        // Remove search and verify.
        searchManager.removeSearch(search);
        resultList = searchManager.getSearchResultList(search);
        assertNull(resultList);
    }

}
