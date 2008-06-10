package com.limegroup.gnutella.caas.restlet;

import com.limegroup.gnutella.caas.SearchParams;
import com.limegroup.gnutella.caas.SearchResultHandler;

public class RestletSearch implements com.limegroup.gnutella.caas.Search {

    private SearchParams _params;
    private SearchResultHandler _handler;
    
    public RestletSearch(SearchParams sp, SearchResultHandler srh) {
        _params = sp;
        _handler = srh;
    }
    
    public void start() {
        
    }
    
    public void stop() {
        
    }
    
    public void getMoreResults() {
        
    }
    
    public SearchResultHandler getSearchResultHandler() {
        return _handler;
    }
    
    public SearchParams getSearchParams() {
        return _params;
    }
    
}
