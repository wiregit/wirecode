package com.limegroup.gnutella.util;

/**
 * Specialized class that uses special keywords for leaf routing
 * tables.
 */
public final class LeafConnection extends NewConnection {
        
    public LeafConnection(String[] keywords) {
        super(15);
        for(int i=0; i<keywords.length; i++) {
            QRT.add(keywords[i]);
        }
    }
}
