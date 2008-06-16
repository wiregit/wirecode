package com.limegroup.gnutella.caas;

public interface Download {

    /**
     * 
     */
    public boolean start();
    
    /**
     * 
     */
    public boolean stop();
    
    /**
     * 
     */
    public boolean pause();
    
    /**
     * 
     */
    public boolean resume();
    
    /**
     * 
     */
    public void update();
    
    /**
     * 
     */
    public void addSource(SearchResult sr);
    
    /**
     * 
     */
    public long getAmountRead();
    
    /**
     * 
     */
    public int getAmountPending();
    
    /**
     * 
     */
    public long getAmountVerified();
    
    /**
     * 
     */
    public boolean isComplete();
    
    /**
     * 
     */
    public String getFilename();
    
    /**
     * 
     */
    public String getState();
    
}
