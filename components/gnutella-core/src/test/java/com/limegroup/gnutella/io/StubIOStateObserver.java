package com.limegroup.gnutella.io;

import java.io.IOException;

public class StubIOStateObserver implements IOStateObserver {
    
    private boolean statesFinished;
    private boolean shutdown;
    private IOException iox;
    
    public void clear() {
        statesFinished = false;
        shutdown = false;
        iox = null;
    }

    public void handleStatesFinished() {
        statesFinished = true;
    }

    public void shutdown() {
        shutdown = true;        
    }

    public void handleIOException(IOException iox) {
        this.iox = iox;
    }

    public IOException getIox() {
        return iox;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public boolean isStatesFinished() {
        return statesFinished;
    }

}
