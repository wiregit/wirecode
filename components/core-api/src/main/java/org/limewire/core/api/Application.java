package org.limewire.core.api;

public interface Application {
    
    /**
     * Returns a URL that is uniquified according
     * to the unique properties of the running app.
     */
    String getUniqueUrl(String baseUrl);
    
    /**
     * Starts the core services of the application.
     */
    void startCore();
    
    /**
     * Stops the core services of the application.
     */
    void stopCore();
    

}
