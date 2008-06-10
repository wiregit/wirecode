package com.limegroup.gnutella.caas;

public interface Search {

    public void start();
    
    public void stop();
    
    public void getMoreResults();
    
    public SearchResultHandler getSearchResultHandler();
    
    public SearchParams getSearchParams();
    
}
