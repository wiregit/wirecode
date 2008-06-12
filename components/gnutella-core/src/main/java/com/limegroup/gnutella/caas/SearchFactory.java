package com.limegroup.gnutella.caas;

public interface SearchFactory {

    public Search createSearch(SearchParams params, SearchResultHandler handler);
    
}
