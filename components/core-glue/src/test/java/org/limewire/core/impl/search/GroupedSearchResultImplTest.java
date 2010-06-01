package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchResult;
import org.limewire.io.URN;
import org.limewire.util.BaseTestCase;


/**
 * JUnit test case for GroupedSearchResultImpl.
 */
public class GroupedSearchResultImplTest extends BaseTestCase {
    /** Instance of class being tested. */
    private GroupedSearchResultImpl groupedResult;
    
    private URN urn;

    /**
     * Constructs a test case for the specified method name.
     */
    public GroupedSearchResultImplTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Create a grouped result with a single source.
        urn = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");
        groupedResult = new GroupedSearchResultImpl(new TestSearchResult(urn, "test0"), "test");
    }
    
    @Override
    protected void tearDown() throws Exception {
        groupedResult = null;
        super.tearDown();
    }

    /** Tests construction with single source. */
    public void testSingleSource() {
        assertEquals(1, groupedResult.getFriends().size());
        assertEquals(1, groupedResult.getSources().size());
        assertEquals(urn, groupedResult.getUrn());
    }
    
    /** Tests method to add another source. */
    public void testAddNewSource() {
        // Add second source with different name.
        SearchResult newSource = new TestSearchResult(urn, "test1");
        groupedResult.addNewSource(newSource, "test");
        
        assertEquals(2, groupedResult.getFriends().size());
        assertEquals(2, groupedResult.getSources().size());
    }
}
