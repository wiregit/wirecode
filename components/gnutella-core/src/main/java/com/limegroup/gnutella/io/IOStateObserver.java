package com.limegroup.gnutella.io;

public interface IOStateObserver extends IOErrorObserver {
    
    public void handleStatesFinished();

}
