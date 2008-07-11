package org.limewire.core.impl.search;

import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;

public class MockSearch implements Search {

    public MockSearch(SearchDetails searchDetails) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void start(final SearchListener searchListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                searchListener.handleSearchResult(new SearchResult() {
                    @Override
                    public Map<Object, Object> getProperties() {
                        return Collections.emptyMap();
                    }
                    @Override
                    public String getUrn() {
                        return "test";
                    }
                });
                searchListener.handleSearchResult(new SearchResult() {
                    @Override
                    public Map<Object, Object> getProperties() {
                        return Collections.emptyMap();
                    }
                    @Override
                    public String getUrn() {
                        return "test";
                    }
                });
                searchListener.handleSearchResult(new SearchResult() {
                    @Override
                    public Map<Object, Object> getProperties() {
                        return Collections.emptyMap();
                    }
                    @Override
                    public String getUrn() {
                        return "test1";
                    }
                });
                searchListener.handleSearchResult(new SearchResult() {
                    @Override
                    public Map<Object, Object> getProperties() {
                        return Collections.emptyMap();
                    }
                    @Override
                    public String getUrn() {
                        return "test2";
                    }
                });
            }
        }).start();

    }

}
