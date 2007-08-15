package com.limegroup.gnutella.util;


/**
 * Helper class that overrides getNumIntraUltrapeerConnections for
 * testing the horizon calculation and testing the new search
 * architecture.
 */
public final class OldConnection extends TestConnection {
    
    public OldConnection(int connections) {
        super(connections);
    }
    
    public boolean isGoodConnection() {
        return false;
    }

    public String toString() {
        return "OLD TEST CONNECTION";
    }

    public boolean isGoodUltrapeer() {
        return false;
    }
}
