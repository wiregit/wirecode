package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.BaseTestCase;

public class BasicSearchResultsModelTest extends BaseTestCase {
    public BasicSearchResultsModelTest(String name) {
        super(name);
    }

    public void testGroupingByName2UrnsNameComesEarly() {

        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("1", "other file");
        // other file for urn1 is coming in early
        TestSearchResult testResult3 = new TestSearchResult("2", "other file");
        TestSearchResult testResult4 = new TestSearchResult("1", "file name");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(1, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(3, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
    }

    public void testGroupingByName2UrnsNameComesLate() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("1", "file name");
        // other file for urn1 is coming in late
        TestSearchResult testResult4 = new TestSearchResult("1", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(1, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(3, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
    }

    public void testGroupingByName2UrnsNameComesLateMultipleAdds() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("1", "file name");
        TestSearchResult testResult4 = new TestSearchResult("1", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(2, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(1, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(9, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(3, coreResults1.size());

        Assert.assertNull(group0.getSimilarityParent());
        Assert.assertEquals(group0, group1.getSimilarityParent());
    }

    public void testGroupByName4Urns() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "other file");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("3", "other file");
        TestSearchResult testResult4 = new TestSearchResult("4", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(4, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(1, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(1, coreResults2.size());

        VisualSearchResult group3 = results.get(3);
        List<VisualSearchResult> groupResults3 = group3.getSimilarResults();
        Assert.assertEquals(3, groupResults3.size());
        List<SearchResult> coreResults3 = group1.getCoreSearchResults();
        Assert.assertEquals(1, coreResults3.size());

        Assert.assertEquals(group3, group0.getSimilarityParent());
        Assert.assertNull(group3.getSimilarityParent());
        Assert.assertEquals(group3, group1.getSimilarityParent());
        Assert.assertEquals(group3, group2.getSimilarityParent());

    }

    public void testGroupingByName3Urns() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "other file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "other file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "other file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(2, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(1, coreResults2.size());

        Assert.assertEquals(group1, group0.getSimilarityParent());
        Assert.assertEquals(group1, group2.getSimilarityParent());
        Assert.assertNull(group1.getSimilarityParent());
    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        model.addSearchResult(testResult6);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(2, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(2, coreResults2.size());

        Assert.assertNull(group2.getSimilarityParent());
        Assert.assertEquals(group2, group0.getSimilarityParent());
        Assert.assertEquals(group2, group1.getSimilarityParent());
    }

    public void testGroupingByName3UrnsNameMatchViaTransitiveProperty3GroupHasMoreFiles() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");
        TestSearchResult testResult7 = new TestSearchResult("3", "blah3 file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        model.addSearchResult(testResult6);
        model.addSearchResult(testResult7);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(2, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(3, coreResults2.size());

        Assert.assertNull(group2.getSimilarityParent());
        Assert.assertEquals(group2, group1.getSimilarityParent());
        Assert.assertEquals(group2, group0.getSimilarityParent());
    }

    public void testGroupingUsingNameLikeLimeComparator() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new SearchResultMatcher() {
                    @Override
                    public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
                        for (SearchResult result1 : o1.getCoreSearchResults()) {
                            String name1 = result1.getProperty(SearchResult.PropertyKey.NAME)
                                    .toString();
                            for (SearchResult result2 : o2.getCoreSearchResults()) {
                                String name2 = result2.getProperty(SearchResult.PropertyKey.NAME)
                                        .toString();
                                if (name1.startsWith("lime") && name2.startsWith("lime")) {
                                    return true;
                                }
                                int result = name1.compareTo(name2);
                                if (result == 0) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                }));

        TestSearchResult testResult1 = new TestSearchResult("1", "lime1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "lime2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah3 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah3 file");
        TestSearchResult testResult7 = new TestSearchResult("3", "lime3 file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        model.addSearchResult(testResult6);
        model.addSearchResult(testResult7);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(2, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(3, coreResults2.size());

        Assert.assertNull(group2.getSimilarityParent());
        Assert.assertEquals(group2, group1.getSimilarityParent());
        Assert.assertEquals(group2, group0.getSimilarityParent());

    }
    
    public void testEverythingSimilarComparator() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new SearchResultMatcher() {
                    @Override
                    public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
                        return true;
                    }
                }));

        TestSearchResult testResult1 = new TestSearchResult("1", "lime1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "lime2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah3 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah3 file");
        TestSearchResult testResult7 = new TestSearchResult("3", "lime3 file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        model.addSearchResult(testResult6);
        model.addSearchResult(testResult7);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(2, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(3, coreResults2.size());

        Assert.assertNull(group2.getSimilarityParent());
        Assert.assertEquals(group2, group1.getSimilarityParent());
        Assert.assertEquals(group2, group0.getSimilarityParent());

    }
    
    public void testNothingSimilarComparator() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new SearchResultMatcher() {
                    @Override
                    public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
                        return false;
                    }
                }));

        TestSearchResult testResult1 = new TestSearchResult("1", "lime1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "lime2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah3 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah3 file");
        TestSearchResult testResult7 = new TestSearchResult("3", "lime3 file");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        model.addSearchResult(testResult6);
        model.addSearchResult(testResult7);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(0, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(3, coreResults2.size());

        Assert.assertNull(group2.getSimilarityParent());
        Assert.assertNull(group1.getSimilarityParent());
        Assert.assertNull(group0.getSimilarityParent());

    }

    public void testVisibility() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(
                new SimilarResultsMatchingDetector(new NameMatcher()));

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah1 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");

        model.addSearchResult(testResult1);
        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(1, results.size());
        VisualSearchResult result0 = results.get(0);
        Assert.assertTrue(result0.isVisible());
        Assert.assertFalse(result0.isChildrenVisible());
        result0.setChildrenVisible(true);
        Assert.assertTrue(result0.isVisible());
        Assert.assertTrue(result0.isChildrenVisible());

        model.addSearchResult(testResult2);
        Assert.assertTrue(result0.isVisible());
        Assert.assertTrue(result0.isChildrenVisible());

        model.addSearchResult(testResult3);

        Assert.assertTrue(result0.isVisible());
        Assert.assertTrue(result0.isChildrenVisible());
        List<VisualSearchResult> children = result0.getSimilarResults();
        Assert.assertEquals(1, children.size());
        VisualSearchResult child = children.get(0);
        Assert.assertTrue(child.isVisible());

        result0.setChildrenVisible(false);
        Assert.assertTrue(result0.isVisible());
        Assert.assertFalse(result0.isChildrenVisible());
        Assert.assertFalse(child.isVisible());
        result0.setChildrenVisible(true);

        
        model.addSearchResult(testResult4);
        model.addSearchResult(testResult5);
        VisualSearchResult result1 = results.get(1);

        VisualSearchResult result2 = results.get(2);

        children = result1.getSimilarResults();
        Assert.assertEquals(2, children.size());
        VisualSearchResult child0 = children.get(0);
        VisualSearchResult child1 = children.get(1);
        Assert.assertTrue(child0.isVisible());
        Assert.assertTrue(child1.isVisible());
        result1.setChildrenVisible(false);
        
        
        model.addSearchResult(testResult6);
        
        children = result2.getSimilarResults();
        Assert.assertEquals(2, children.size());
        child0 = children.get(0);
        child1 = children.get(1);
        Assert.assertFalse(child0.isVisible());
        Assert.assertFalse(child1.isVisible());
        result2.setChildrenVisible(true);
        Assert.assertTrue(child0.isVisible());
        Assert.assertTrue(child1.isVisible());
        
        
        
        results = model.getVisualSearchResults();
        Assert.assertEquals(3, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(2, coreResults0.size());

        VisualSearchResult group1 = results.get(1);
        List<VisualSearchResult> groupResults1 = group1.getSimilarResults();
        Assert.assertEquals(0, groupResults1.size());
        List<SearchResult> coreResults1 = group1.getCoreSearchResults();
        Assert.assertEquals(2, coreResults1.size());

        VisualSearchResult group2 = results.get(2);
        List<VisualSearchResult> groupResults2 = group2.getSimilarResults();
        Assert.assertEquals(2, groupResults2.size());
        List<SearchResult> coreResults2 = group2.getCoreSearchResults();
        Assert.assertEquals(2, coreResults2.size());

        Assert.assertNull(group2.getSimilarityParent());
        Assert.assertEquals(group2, group0.getSimilarityParent());
        Assert.assertEquals(group2, group1.getSimilarityParent());
    }

    public class TestSearchResult implements SearchResult {

        private String urn;

        private Map<PropertyKey, Object> properties;

        public TestSearchResult(String urn, String fileName) {
            this.urn = urn;
            this.properties = new HashMap<PropertyKey, Object>();
            this.properties.put(PropertyKey.NAME, fileName);
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public String getFileExtension() {
            return null;
        }

        @Override
        public Map<PropertyKey, Object> getProperties() {
            return properties;
        }

        @Override
        public Object getProperty(PropertyKey key) {
            return properties.get(key);
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public List<RemoteHost> getSources() {
            return new ArrayList<RemoteHost>();
        }

        @Override
        public String getUrn() {
            return urn;
        }

        public String toString() {
            return getUrn() + " - " + getProperty(PropertyKey.NAME);
        }

        @Override
        public Category getCategory() {
            return null;
        }

        @Override
        public boolean isSpam() {
            return false;
        }
    }

}