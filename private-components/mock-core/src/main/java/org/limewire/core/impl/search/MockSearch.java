package org.limewire.core.impl.search;

import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.search.ResultType;
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
                    
                    @Override
                    public ResultType getResultType() {
                        return ResultType.AUDIO;
                    }
                    
                    @Override
                    public String getDescription() {
                        return "5.0 Blues";
                    }
                    
                    @Override
                    public String getFileExtension() {
                        return "ogg";
                    }
                    
                    @Override
                    public long getSize() {
                        return 123456789L;
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
                    
                    @Override
                    public String getDescription() {
                        return "5.0 Blues Source 2";
                    }
                    
                    @Override
                    public ResultType getResultType() {
                        return ResultType.AUDIO;
                    }

                    @Override
                    public String getFileExtension() {
                        return "ogg";
                    }
                    
                    @Override
                    public long getSize() {
                        return 123456789L;
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
                    
                    @Override
                    public String getDescription() {
                        return "Standdown Situps";
                    }
                    
                    @Override
                    public ResultType getResultType() {
                        return ResultType.VIDEO;
                    }
                    
                    @Override
                    public String getFileExtension() {
                        return "ogv";
                    }
                    
                    @Override
                    public long getSize() {
                        return 123456L;
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
                    
                    @Override
                    public String getDescription() {
                        return "Craziness";
                    }
                    
                    @Override
                    public ResultType getResultType() {
                        return ResultType.UNKNOWN;
                    }
                    
                    @Override
                    public String getFileExtension() {
                        return "tmp";
                    }
                    
                    @Override
                    public long getSize() {
                        return 1L;
                    }
                });
            }
        }).start();

    }

}
