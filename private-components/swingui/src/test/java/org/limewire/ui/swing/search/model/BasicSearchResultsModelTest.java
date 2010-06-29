package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.limewire.core.api.search.SearchResult;
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
    
    private void addGroupedResult(MockGroupedSearchResult mockResult, String fileName) {
        mockResult.addResult(mockResult.getUrn(), fileName);
        
        GroupedSearchResult newResult = new MockGroupedSearchResult(mockResult.getUrn(), fileName);
        model.addResultsInternal(Collections.<GroupedSearchResult>singletonList(newResult));
        waitForUiThread();
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
    
    public void testGroupingByName2UrnsNameComesEarly() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));
        
        // Add two results grouped by URN. 
        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "file name");
        addResult(model, testResult1);
        addGroupedResult(testResult1, "other file");
        // Add result for 2nd URN with matching name.
        MockGroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("2"), "other file");
        addResult(model, testResult3);
        // Add result for 1st URN.
        addGroupedResult(testResult1, "file name");

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult vsr0 = results.get(0);
        List<VisualSearchResult> similarResults0 = vsr0.getSimilarResults();
        assertEquals(1, similarResults0.size()); // Parent with one child
        List<SearchResult> coreResults0 = vsr0.getCoreSearchResults();
        assertEquals(3, coreResults0.size());

        VisualSearchResult vsr1 = results.get(1);
        List<VisualSearchResult> similarResults1 = vsr1.getSimilarResults();
        assertEquals(0, similarResults1.size()); // Child
        List<SearchResult> coreResults1 = vsr1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(vsr0.getSimilarityParent()); // Parent has no parent
        assertEquals(vsr0, vsr1.getSimilarityParent());
    }

    public void testGroupingByName2UrnsNameComesLate() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));
        
        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "file name");
        MockGroupedSearchResult testResult2 = new MockGroupedSearchResult(new MockURN("2"), "other file");

        // Add results for two separate URNs.
        addResult(model, testResult1);
        addResult(model, testResult2);
        // Add results for 1st URN including matching name.
        addGroupedResult(testResult1, "file name");
        addGroupedResult(testResult1, "other file");

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult vsr0 = results.get(0);
        List<VisualSearchResult> similarResults0 = vsr0.getSimilarResults();
        assertEquals(1, similarResults0.size()); // Parent with one child
        List<SearchResult> coreResults0 = vsr0.getCoreSearchResults();
        assertEquals(3, coreResults0.size());

        VisualSearchResult vsr1 = results.get(1);
        List<VisualSearchResult> similarResults1 = vsr1.getSimilarResults();
        assertEquals(0, similarResults1.size()); // Child
        List<SearchResult> coreResults1 = vsr1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(vsr0.getSimilarityParent()); // Parent has no parent
        assertEquals(vsr0, vsr1.getSimilarityParent());
    }
    
    public void testGroupingByName2UrnsNameComesLateMultipleAdds() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "file name");
        MockGroupedSearchResult testResult2 = new MockGroupedSearchResult(new MockURN("2"), "other file");

        // Add results - multiple adds for same URN are ignored.
        addResult(model, testResult1);
        addResult(model, testResult2);
        addGroupedResult(testResult1, "file name");
        addGroupedResult(testResult1, "other file");
        addResult(model, testResult1);
        addResult(model, testResult2);
        addGroupedResult(testResult1, "file name");
        addGroupedResult(testResult1, "other file");
        addResult(model, testResult1);
        addResult(model, testResult2);
        addGroupedResult(testResult1, "file name");
        addGroupedResult(testResult1, "other file");

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult vsr0 = results.get(0);
        List<VisualSearchResult> similarResults1 = vsr0.getSimilarResults();
        assertEquals(1, similarResults1.size()); // Parent with one child
        List<SearchResult> coreResults1 = vsr0.getCoreSearchResults();
        assertEquals(7, coreResults1.size());

        VisualSearchResult vsr1 = results.get(1);
        List<VisualSearchResult> similarResults0 = vsr1.getSimilarResults();
        assertEquals(0, similarResults0.size()); // Child
        List<SearchResult> coreResults0 = vsr1.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        assertNull(vsr0.getSimilarityParent()); // Parent has no parent
        assertEquals(vsr0, vsr1.getSimilarityParent());

    }

    public void testGroupingByName4Urns() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        GroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "other file");
        GroupedSearchResult testResult2 = new MockGroupedSearchResult(new MockURN("2"), "other file");
        GroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("3"), "other file");
        GroupedSearchResult testResult4 = new MockGroupedSearchResult(new MockURN("4"), "other file");

        addResult(model, testResult1);
        addResult(model, testResult2);
        addResult(model, testResult3);
        addResult(model, testResult4);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(4, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(3, similarResults0.size()); // Parent with three children

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size()); // Child

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size()); // Child

        VisualSearchResult group3 = results.get(3);
        List<VisualSearchResult> similarResults3 = group3.getSimilarResults();
        assertEquals(0, similarResults3.size()); // Child

        assertNull(group0.getSimilarityParent()); // Parent has no parent
        assertEquals(group0, group1.getSimilarityParent());
        assertEquals(group0, group2.getSimilarityParent());
        assertEquals(group0, group3.getSimilarityParent());

    }

    public void testGroupingByName3Urns() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "other file");
        MockGroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("2"), "other file");
        MockGroupedSearchResult testResult5 = new MockGroupedSearchResult(new MockURN("3"), "other file");

        addResult(model, testResult1);
        addGroupedResult(testResult1, "blah1 file");
        addResult(model, testResult3);
        addGroupedResult(testResult3, "blah2 file");
        addResult(model, testResult5);

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(3, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(2, similarResults0.size()); // Parent with two children
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size()); // Child
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size()); // Child
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(1, coreResults2.size());

        assertNull(group0.getSimilarityParent()); // Parent has no parent
        assertEquals(group0, group1.getSimilarityParent());
        assertEquals(group0, group2.getSimilarityParent());

    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "blah1 file");
        MockGroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("2"), "blah2 file");
        MockGroupedSearchResult testResult5 = new MockGroupedSearchResult(new MockURN("3"), "blah1 file");

        addResult(model, testResult1);
        addGroupedResult(testResult1, "blah1 file");
        addResult(model, testResult3);
        addGroupedResult(testResult3, "blah2 file");
        addResult(model, testResult5);
        addGroupedResult(testResult5, "blah2 file");

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(3, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(2, similarResults0.size()); // Parent with two children
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size()); // Child
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size()); // Child
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(2, coreResults2.size());

        assertNull(group0.getSimilarityParent()); // Parent has no parent
        assertEquals(group0, group1.getSimilarityParent());
        assertEquals(group0, group2.getSimilarityParent());

    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty3GroupHasMoreFiles() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));

        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "blah1 file");
        MockGroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("2"), "blah2 file");
        MockGroupedSearchResult testResult5 = new MockGroupedSearchResult(new MockURN("3"), "blah1 file");

        addResult(model, testResult1);
        addGroupedResult(testResult1, "blah1 file");
        addResult(model, testResult3);
        addGroupedResult(testResult3, "blah2 file");
        addResult(model, testResult5);
        addGroupedResult(testResult5, "blah2 file");
        addGroupedResult(testResult5, "blah3 file");

        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(3, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(0, similarResults0.size()); // Child
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size()); // Child
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(2, similarResults2.size()); // Parent with two children
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(3, coreResults2.size());

        assertEquals(group2, group0.getSimilarityParent());
        assertEquals(group2, group1.getSimilarityParent());
        assertNull(group2.getSimilarityParent()); // Parent has no parent

    }

    public void testVisibility() {

        model.addResultListener(new GroupingListEventListener(new SimilarResultsFileNameDetector()));
        
        MockGroupedSearchResult testResult1 = new MockGroupedSearchResult(new MockURN("1"), "blah1 file");
        MockGroupedSearchResult testResult3 = new MockGroupedSearchResult(new MockURN("2"), "blah1 file");
        MockGroupedSearchResult testResult5 = new MockGroupedSearchResult(new MockURN("3"), "blah1 file");
        
        addResult(model, testResult1);
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(1, results.size());
        VisualSearchResult result0 = results.get(0);
        assertTrue(result0.isVisible());
        assertFalse(result0.isChildrenVisible());
        result0.setChildrenVisible(true);
        assertTrue(result0.isVisible());
        assertTrue(result0.isChildrenVisible());

        addGroupedResult(testResult1, "blah1 file");
        assertTrue(result0.isVisible());
        assertTrue(result0.isChildrenVisible());

        addResult(model, testResult3);
        assertTrue(result0.isVisible());
        assertTrue(result0.isChildrenVisible());
        List<VisualSearchResult> children = result0.getSimilarResults();
        assertEquals(1, children.size());
        VisualSearchResult child = children.get(0);
        assertTrue(child.isVisible());

        result0.setChildrenVisible(false);
        assertTrue(result0.isVisible());
        assertFalse(result0.isChildrenVisible());
        assertFalse(child.isVisible());
        result0.setChildrenVisible(true);

        addGroupedResult(testResult3, "blah2 file");
        addResult(model, testResult5);
        children = result0.getSimilarResults();
        assertEquals(2, children.size());
        VisualSearchResult child0 = children.get(0);
        VisualSearchResult child1 = children.get(1);
        assertTrue(child0.isVisible());
        assertTrue(child1.isVisible());
        result0.setChildrenVisible(false);

        addGroupedResult(testResult5, "blah2 file");
        children = result0.getSimilarResults();
        assertEquals(2, children.size());
        child0 = children.get(0);
        child1 = children.get(1);
        assertFalse(child0.isVisible());
        assertFalse(child1.isVisible());
        result0.setChildrenVisible(true);
        assertTrue(child0.isVisible());
        assertTrue(child1.isVisible());

        results = model.getUnfilteredList();
        assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(2, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> similarResults2 = group2.getSimilarResults();
        assertEquals(0, similarResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        assertEquals(2, coreResults2.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group2.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }
    
    public void testSameNameHyphenNameHyphenName() {

        
        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test-foo-bar");
        GroupedSearchResult searchResult1 = new MockGroupedSearchResult(new MockURN("1"), "test-foo-bar.mp3", properties1); 
        
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "test-foo-bar");
        GroupedSearchResult searchResult2 = new MockGroupedSearchResult(new MockURN("2"), "test-foo-bar.mp3", properties2);
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }
    
    public void testNotSameNameOrButSameTrackMetaData() {

        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test");
        GroupedSearchResult searchResult1 = new MockGroupedSearchResult(new MockURN("1"), "test.mp3", properties1);
        
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "blah123");
        properties2.put(FilePropertyKey.TITLE, "test");
        GroupedSearchResult searchResult2 = new MockGroupedSearchResult(new MockURN("2"), "blah123.mp3", properties2);
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(0, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        //should be no similar results
        assertNull(group1.getSimilarityParent());
        assertNull(group0.getSimilarityParent());


    }
    
    public void testSameNameOrAlbumAndTrackMetaData() {

        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test-blah");
        GroupedSearchResult searchResult1 = new MockGroupedSearchResult(new MockURN("1"), "test-blah.mp3", properties1);
        
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "blah123");
        properties2.put(FilePropertyKey.ALBUM, "test");
        properties2.put(FilePropertyKey.TITLE, "blah");
        GroupedSearchResult searchResult2 = new MockGroupedSearchResult(new MockURN("2"), "blah123.mp3", properties2);
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

    }
    
    public void testSameNameOrArtistAndTrackMetaData() {

        Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        properties1.put(FilePropertyKey.NAME, "test-blah");
        GroupedSearchResult searchResult1 = new MockGroupedSearchResult(new MockURN("1"), "test-blah.mp3", properties1);
        
        Map<FilePropertyKey, Object> properties2 = new HashMap<FilePropertyKey, Object>();
        properties2.put(FilePropertyKey.NAME, "blah123");
        properties2.put(FilePropertyKey.AUTHOR, "test");
        properties2.put(FilePropertyKey.TITLE, "blah");
        GroupedSearchResult searchResult2 = new MockGroupedSearchResult(new MockURN("2"), "blah123.mp3", properties2);
        
        model.addResultListener(new GroupingListEventListener(new AudioMetaDataSimilarResultsDetector()));
        
        addResult(model, searchResult1);
        addResult(model, searchResult2);
        
        List<VisualSearchResult> results = model.getUnfilteredList();
        assertEquals(2, results.size());
        
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> similarResults0 = group0.getSimilarResults();
        assertEquals(1, similarResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> similarResults1 = group1.getSimilarResults();
        assertEquals(0, similarResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        assertEquals(1, coreResults1.size());

        assertNull(group0.getSimilarityParent());
        assertEquals(group0, group1.getSimilarityParent());

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
        
        // Apply sort option.
        model.setSortOption(SortOption.NAME);
        waitForUiThread();
        
        // Verify sorted order.
        expectedReturn = "whiskey";
        actualReturn = sortedList.get(0).getHeading();
        assertEquals("sorted list", expectedReturn, actualReturn);

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
