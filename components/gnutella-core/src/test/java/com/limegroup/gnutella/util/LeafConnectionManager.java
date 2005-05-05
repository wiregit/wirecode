package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ConnectionManager;

/**
 * Helper class that always says it is a leaf.
 */
public final class LeafConnectionManager extends ConnectionManager {

    public LeafConnectionManager() {
        super();
    }

    public boolean isSupernode() {
        return false;
    }
}

