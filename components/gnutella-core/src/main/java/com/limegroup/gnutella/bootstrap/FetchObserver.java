package com.limegroup.gnutella.bootstrap;

/**
 * An observer interface for those that want to be notified
 * when endpoint fetching finishes.
 */
public interface FetchObserver {
    /**
     * Called when BootstrapServerManager finishes
     * contacting GWebCaches to retrieve endpoints.
     */
    public void endpointFetchFinished(int fetched);
    
    /**
     * Called when the BootstrapServerManager is about
     * to start contacting GWebCaches to retrieve endpoints.
     */
    public void endpointFetchStarted();
}