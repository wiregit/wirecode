package org.limewire.core.api;

/** Contains methods relating to the overall application. */
public interface Application {
    
    /**
     * Returns a URL that is uniquified according
     * to the unique properties of the running app.
     */
    String getUniqueUrl(String baseUrl);
    
    /** Returns true if the application is in a 'testing' version. */
    boolean isTestingVersion();
    
    /**
     * Starts the core services of the application.
     */
    void startCore();
    
    /**
     * Stops the core services of the application.
     */
    void stopCore();
    
    /** Returns the version of the program. */
    String getVersion();
    

}
