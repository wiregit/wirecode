package com.limegroup.gnutella.caas;

public interface Search {

    public void start();
    
    public boolean stop();
    
    public void getMoreResults();
    
    public SearchResultHandler getSearchResultHandler();
    
    public SearchParams getSearchParams();
    
}
