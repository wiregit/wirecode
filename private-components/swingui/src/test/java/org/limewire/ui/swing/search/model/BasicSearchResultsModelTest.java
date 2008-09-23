package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.BaseTestCase;

public class BasicSearchResultsModelTest extends BaseTestCase {
    public BasicSearchResultsModelTest(String name) {
        super(name);
    }

    public void testUrnGrouping1() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));
        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("1", "alternate file name");
        TestSearchResult testResult3 = new TestSearchResult("1", "other file");
        TestSearchResult testResult4 = new TestSearchResult("1", "file name");

        model.addSearchResult(testResult1);
        model.addSearchResult(testResult2);
        model.addSearchResult(testResult3);
        model.addSearchResult(testResult4);

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(1, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(4, coreResults0.size());
        Assert.assertNull(group0.getSimilarityParent());
    }

    public void testUrnOrnNameGrouping1() {

        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("1", "other file");
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

    public void testUrnOrNameGrouping2() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));;

        TestSearchResult testResult1 = new TestSearchResult("1", "file name");
        TestSearchResult testResult2 = new TestSearchResult("2", "other file");
        TestSearchResult testResult3 = new TestSearchResult("1", "file name");
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
    
    public void testUrnOrNameGrouping3() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));;

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
    
    public void testUrnOrNameGrouping4() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));;

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
    
    public void testUrnOrNameGrouping5() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));;

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
    
    public void testUrnOrNameGrouping6() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));;

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
    
    public void testUrnOrNameGrouping7() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));;

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
    
    public void testUrnOrNameGrouping8() {
        BasicSearchResultsModel model = new BasicSearchResultsModel(new ComparatorBasedSimilarResultsDetector(new NamesMatchComparator()));;

        TestSearchResult testResult1 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult2 = new TestSearchResult("1", "blah1 file");
        TestSearchResult testResult3 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult4 = new TestSearchResult("2", "blah2 file");
        TestSearchResult testResult5 = new TestSearchResult("3", "blah1 file");
        TestSearchResult testResult6 = new TestSearchResult("3", "blah2 file");
        TestSearchResult testResult7 = new TestSearchResult("3", "blah2 file");

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

    public class TestSearchResult implements SearchResult {

        private String urn;

        private Map<PropertyKey, Object> properties;

        private final String description;

        public TestSearchResult(String urn, String fileName) {
            this.urn = urn;
            this.properties = new HashMap<PropertyKey, Object>();
            this.properties.put(PropertyKey.NAME, fileName);
            this.description = UUID.randomUUID().toString();

        }

        @Override
        public String getDescription() {
            return description;
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
            // TODO Auto-generated method stub
            return null;
        }
    }

}