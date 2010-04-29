package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.io.GUID;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.TextMatcherEditor;

import com.google.inject.Provider;

/**
 * Test case for BasicSearchResultsModel. 
 */
public class BasicSearchResultsModelTest extends BaseTestCase {
    /** Instance of class being tested. */
    private BasicSearchResultsModel model;
    private Provider<PropertiableHeadings> provider;
    private Mockery context;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public BasicSearchResultsModelTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new Mockery();
        provider = context.mock(Provider.class);
        // Create test instance.
        model = new BasicSearchResultsModel(new TestSearchInfo(), new TestSearch(),
                new VisualSearchResultFactoryImpl(provider), null, null, new TestSearchManager());
    }
    
    @Override
    protected void tearDown() throws Exception {
        model = null;
        super.tearDown();
    }
    
    private void waitForUiThread() {
        SwingUtils.invokeNowOrWait(new Runnable() {
            @Override
            public void run() {
            }
        });
    }
    
    private void addResult(BasicSearchResultsModel model, GroupedSearchResult result) {
        model.addResultsInternal(Collections.<GroupedSearchResult>singletonList(result));
    }
    
    public void testAddResultsInternal() {
        // Create large result set to add.
        List<GroupedSearchResult> list = new ArrayList<GroupedSearchResult>();
        for (int i = 1; i <= 1005; i++) {
            GroupedSearchResult result = new MockGroupedSearchResult(new MockURN(String.valueOf(i)), "file" + String.valueOf(i));
            list.add(result);
        }
        
        // Define listener to count list change events.
        class ListEventCounter implements ListEventListener<VisualSearchResult> {
            public int count;
            
            @Override
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                count++;
            }
        }
        ListEventCounter counter = new ListEventCounter();
        model.getUnfilteredList().addListEventListener(counter);
        
        // Add results.
        model.addResultsInternal(list);
        waitForUiThread();
        
        // Verify results added and list change events.
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(1005, results.size());
        assertEquals(1, counter.count);
    }

    /** Tests method to retrieve filtered search results. */
    public void testGetFilteredSearchResults() {

        // Create test search results.
        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "xray");
        MockGroupedSearchResult testResult2 = new MockGroupedSearchResult(new MockURN("2"), "zulu");
        MockGroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("3"), "whiskey");
        MockGroupedSearchResult testResult4 = new MockGroupedSearchResult(new MockURN("4"), "yankee");
        testResult3.setCategory(Category.VIDEO);
        testResult4.setCategory(Category.IMAGE);

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        
        // Apply category filter.
        model.setFilterEditor(new AbstractMatcherEditor<VisualSearchResult>() {
            Matcher<VisualSearchResult> matcher = new Matcher<VisualSearchResult>() {
                @Override
                public boolean matches(VisualSearchResult item) {
                    return item.getCategory() == Category.VIDEO;
                }
            };
            
            @Override
            public Matcher<VisualSearchResult> getMatcher() {
                return matcher;
            }
        });
        waitForUiThread();
        
        // Get filtered search results.
        List<VisualSearchResult> filteredList = model.getFilteredSearchResults();
        
        // Verify filtered list.
        int expectedSize = 1;
        int actualSize = filteredList.size();
        assertEquals("filtered list size", expectedSize, actualSize);

    }
    
    /** Tests method to retrieve sorted and filtered search results. */
    public void testGetSortedSearchResults() {

        final PropertiableHeadings propertiableHeadings = context.mock(PropertiableHeadings.class);

        context.checking(new Expectations(){
            {
                allowing(provider).get();
                will(returnValue(propertiableHeadings));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("xray"));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("zulu"));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("whiskey"));
                one(propertiableHeadings).getHeading(with(any(PropertiableFile.class)));
                will(returnValue("yankee"));
            }
        });
        
        // Create test search results.
        GroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "xray");
        GroupedSearchResult testResult2 = new MockGroupedSearchResult(new MockURN("2"), "zulu");
        GroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("3"), "whiskey");
        GroupedSearchResult testResult4 = new MockGroupedSearchResult(new MockURN("4"), "yankee");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        
        // Get sorted search results.
        model.setSelectedCategory(SearchCategory.ALL);
        waitForUiThread();
        List<VisualSearchResult> sortedList = model.getSortedSearchResults();

        // Verify unsorted order.
        String expectedReturn = "xray";
        String actualReturn = sortedList.get(0).getHeading();
        assertEquals("unsorted list", expectedReturn, actualReturn);
    }

    /** Tests method to set filter editor with filter text. */
    public void testSetFilterEditor() {

        // Create test search results.
        GroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "xray");
        GroupedSearchResult testResult2 = new MockGroupedSearchResult(new MockURN("2"), "zulu");
        GroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("3"), "whiskey");
        GroupedSearchResult testResult4 = new MockGroupedSearchResult(new MockURN("4"), "yankee");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);
        
        // Get all search results.
        List<VisualSearchResult> filteredList = model.getFilteredSearchResults();
        
        // Verify unfiltered list.
        int expectedSize = 4;
        int actualSize = filteredList.size();
        assertEquals("unfiltered list size", expectedSize, actualSize);
        
        // Apply filter editor.
        TextMatcherEditor<VisualSearchResult> editor = new TextMatcherEditor<VisualSearchResult>(
                new VisualSearchResultTextFilterator());
        editor.setFilterText(new String[] {"z"});
        model.setFilterEditor(editor);
        waitForUiThread();
        
        // Verify filtered list.
        expectedSize = 1;
        actualSize = filteredList.size();
        assertEquals("filtered list size", expectedSize, actualSize);

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
     * Test implementation of SearchInfo.
     */
    private static class TestSearchInfo implements SearchInfo {

        @Override
        public String getTitle() {
            return null;
        }

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
    
    private static class TestSearchManager implements SearchManager {

        @Override
        public SearchResultList addMonitoredSearch(Search search, SearchDetails searchDetails) {
            return addSearch(search, searchDetails);
        }

        @Override
        public SearchResultList addSearch(Search search, SearchDetails searchDetails) {
            return new TestSearchResultList();
        }

        @Override
        public SearchResultList getSearchResultList(GUID guid) {
            return null;
        }

        @Override
        public SearchResultList getSearchResultList(Search search) {
            return null;
        }

        @Override
        public List<SearchResultList> getActiveSearchLists() {
            return null;
        }

        @Override
        public void removeSearch(Search search) {
        }

        @Override
        public void stopSearch(SearchResultList resultList) {
        }
    }
    
    private static class TestSearchResultList implements SearchResultList {
        private final EventList<GroupedSearchResult> groupedUrnResultList = 
            new BasicEventList<GroupedSearchResult>();

        @Override
        public void addListener(EventListener<Collection<GroupedSearchResult>> listener) {
        }

        @Override
        public boolean removeListener(EventListener<Collection<GroupedSearchResult>> listener) {
            return false;
        }

        @Override
        public void clear() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public GroupedSearchResult getGroupedResult(URN urn) {
            return null;
        }

        @Override
        public EventList<GroupedSearchResult> getGroupedResults() {
            return groupedUrnResultList;
        }

        @Override
        public GUID getGuid() {
            return null;
        }

        @Override
        public int getResultCount() {
            return 0;
        }

        @Override
        public Search getSearch() {
            return null;
        }

        @Override
        public String getSearchQuery() {
            return null;
        }
    }
}
