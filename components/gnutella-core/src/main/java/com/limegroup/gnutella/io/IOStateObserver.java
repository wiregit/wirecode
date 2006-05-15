package com.limegroup.gnutella.io;

/**
 * An observer that is notified when all current states
 * are finished processing, had an IOException, or a state
 * machine was shutdown.
 */
public interface IOStateObserver extends IOErrorObserver {
    
    /**
     * Notification that the states are finished processing.
     */
    public void handleStatesFinished();

}
