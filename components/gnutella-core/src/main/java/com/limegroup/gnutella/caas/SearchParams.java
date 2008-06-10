package com.limegroup.gnutella.caas;

public class SearchParams {

    private String _queryString;
    
    public SearchParams() {
        
    }
    
    public SearchParams(String query) {
        _queryString = query;
    }
    
    public void setQueryString(String s) {
        _queryString = s;
    }
    
    public String getQueryString() {
        return _queryString;
    }
    
}
