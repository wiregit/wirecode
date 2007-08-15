package com.limegroup.gnutella.util;

/**
 * Specialized class that uses special keywords for Ultrapeer routing
 * tables.
 */
public final class UltrapeerConnection extends NewConnection {   

    public UltrapeerConnection() {
        super(15, false);
    }

    public UltrapeerConnection(String[] keywords) {
        super(15, false);
        for(int i=0; i<keywords.length; i++) {
            QRT.add(keywords[i]);
        }
    }


    public boolean supportsProbeQueries() {
        return true;
    }
}

