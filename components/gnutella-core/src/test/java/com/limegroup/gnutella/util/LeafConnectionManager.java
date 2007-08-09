package com.limegroup.gnutella.util;

import com.limegroup.gnutella.HackConnectionManager;

/**
 * Helper class that always says it is a leaf.
 */
public final class LeafConnectionManager extends HackConnectionManager {

    public boolean isSupernode() {
        return false;
    }
}

