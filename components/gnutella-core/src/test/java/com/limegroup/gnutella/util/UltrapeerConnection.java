package com.limegroup.gnutella.util;
import com.limegroup.gnutella.routing.*;

/**
 * Specialized class that uses special keywords for Ultrapeer routing
 * tables.
 */
public final class UltrapeerConnection extends NewConnection {   

    public UltrapeerConnection() {
        super(15);
    }

    public UltrapeerConnection(String[] keywords) {
        super(15);
        for(int i=0; i<keywords.length; i++) {
            QRT.add(keywords[i]);
        }
    }


    public boolean supportsProbeQueries() {
        return true;
    }
}

