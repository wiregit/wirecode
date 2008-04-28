package com.limegroup.gnutella;

/**
 * Provides simple statistics regarding a specific query result. Typically 
 * obtained via SearchResultStats.getResultHandler(URN).
 *
 */
public interface QueryResultHandler {

    /**
     * Returns the percentage of the data corresponding to the target URN that
     * is available based on results from the query - both partial and whole.
     * 
     */
    public float getPercentAvailable();
    
    /**
     * Returns the number of number of locations from which the entirety of
     * the data for the target URN is available.
     * 
     */
    public int getNumberOfLocations();
    
    /**
     * Returns the number of locations to display in the GUI. This number is
     * distinct from getNumberOfLocations() in that it limits the number of
     * locations-per-reply - a spam-fighting characteristic.
     * 
     */
    public int getNumberOfDisplayLocations();
    
}
