package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.BaseTestCase;

public class BasicSearchResultsModelTest extends BaseTestCase {
    public BasicSearchResultsModelTest(String name) {
        super(name);
    }

    public void testUrnGrouping1() {
        BasicSearchResultsModel model = new BasicSearchResultsModel();
        model.addSearchResult(new TestSearchResult("1", "file name"));
        model.addSearchResult(new TestSearchResult("1", "alternate file name"));
        model.addSearchResult(new TestSearchResult("1", "other file"));
        model.addSearchResult(new TestSearchResult("1", "file name"));

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(1, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(4, coreResults0.size());
    }

    public void testUrnOrnNameGrouping1() {
        BasicSearchResultsModel model = new BasicSearchResultsModel();
        model.addSearchResult(new TestSearchResult("1", "file name"));
        model.addSearchResult(new TestSearchResult("1", "other file"));
        model.addSearchResult(new TestSearchResult("2", "other file"));
        model.addSearchResult(new TestSearchResult("1", "file name"));

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(1, results.size());
        VisualSearchResult group0 = results.get(0);
        List<VisualSearchResult> groupResults0 = group0.getSimilarResults();
        Assert.assertEquals(0, groupResults0.size());
        List<SearchResult> coreResults0 = group0.getCoreSearchResults();
        Assert.assertEquals(4, coreResults0.size());
    }

    public void testUrnOrNameGrouping2() {
        BasicSearchResultsModel model = new BasicSearchResultsModel();
        model.addSearchResult(new TestSearchResult("1", "file name"));
        model.addSearchResult(new TestSearchResult("2", "other file"));
        model.addSearchResult(new TestSearchResult("1", "file name"));
        model.addSearchResult(new TestSearchResult("1", "other file"));

        List<VisualSearchResult> results = model.getVisualSearchResults();
        Assert.assertEquals(2, results.size());
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
            return null;
        }

        @Override
        public Object getProperty(PropertyKey key) {
            return properties.get(key);
        }

        @Override
        public ResultType getResultType() {
            return null;
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
    }

}
