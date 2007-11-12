package com.limegroup.gnutella;

/**
 * Defines the interface for services that need to be started and stop.
 * 
 * TODO could be moved to components eventually with a generic implementation
 * of LifeCycleManager that also tracks dependencies. 
 */
public interface Service {

    void start();
    
    void stop();

}
