package com.limegroup.gnutella.caas;

public interface Download {

    public void start();
    
    public void stop();
    
    public void addSource(SearchResult sr);
    
    public String getState();
    
    public String getFilename();
    
}
