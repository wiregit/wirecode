package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ConnectionManager;

/**
 * Helper class that always says it is an Ultrapeer.
 */
public final class UltrapeerConnectionManager extends ConnectionManager {

    public UltrapeerConnectionManager() {
        super();
    }

    public boolean isSupernode() {
        return true;
    }
}

